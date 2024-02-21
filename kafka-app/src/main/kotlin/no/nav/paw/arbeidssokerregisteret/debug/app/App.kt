package no.nav.paw.arbeidssokerregisteret.debug.app

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.paw.arbeidssokerregisteret.debug.app.auth.Autentiseringskonfigurasjon
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.LongDeserializer
import java.time.Duration
import java.time.Instant
import java.util.UUID

const val OPPLYSNINGER_TOPIC = "paw.opplysninger-om-arbeidssoeker-beta-v7"
const val PERIODE_TOPIC = "paw.arbeidssokerperioder-beta-v7"
const val PROFILERING = "paw.arbeidssoker-profilering-beta-v2"

val topics = listOf(OPPLYSNINGER_TOPIC, PERIODE_TOPIC, PROFILERING)

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val kafkaFactory = KafkaFactory(kafkaConfig)
    val authConfig = loadNaisOrLocalConfiguration<Autentiseringskonfigurasjon>("ktor_server_autentisering.toml")

    fun consumer(): Consumer<Long, Any> = kafkaFactory.createConsumer(
        groupId = "paw-arbeidssokerregisteret-debug-app-${UUID.randomUUID()}",
        clientId = "paw-arbeidssokerregisteret-debug-app-${UUID.randomUUID()}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = KafkaAvroDeserializer::class
    )

    embeddedServer(Netty, port = 8080) {
        authentication {
            authConfig.providers.forEach { provider ->
                tokenValidationSupport(
                    name = provider.name,
                    requiredClaims = RequiredClaims(
                        issuer = provider.name,
                        claimMap = provider.requiredClaims.toTypedArray()
                    ),
                    config = TokenSupportConfig(
                        IssuerConfig(
                            name = provider.name,
                            discoveryUrl = provider.discoveryUrl,
                            acceptedAudience = provider.acceptedAudience
                        ),
                    ),
                )
            }
        }
        routing {
            get("/isAlive") {
                call.respondText("I'm alive!")
            }
            get("/isReady") {
                call.respondText("I'm ready!")
            }
            authenticate(authConfig.kafkaKeyApiAuthProvider) {
                get("/api-topics/by-trace-id/{traceId}") {
                    val traceId: String? = call.parameters["traceId"]
                    val partition: Int? = call.request.queryParameters["partition"]?.toIntOrNull()
                    if (traceId == null || partition == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing params")
                        return@get
                    } else {
                        val topicPartitions = topics.map { topic -> TopicPartition(topic, partition) }
                        val records = consumer().use { consumer ->
                            consumer.assign(topicPartitions)
                            consumer.seekToBeginning(topicPartitions)
                            consumer.asSequence()
                                .filter { record ->
                                    val traceparent = record.headers()
                                        .lastHeader("traceparent")?.value()
                                        ?.toString(Charsets.UTF_8)
                                    traceparent?.contains(traceId) == true
                                }.map { record -> record.timestamp() to (record.value() as GenericRecord) }
                                .map { (timestamp, generic) ->
                                    listOfNotNull(
                                        "schema" to generic.schema.name,
                                        "timestamp" to Instant.ofEpochMilli(timestamp)
                                            .toString(),
                                        "id" to generic.get("id")?.toString(),
                                        "ref:periodeId" to generic.get("periodeId")
                                            ?.toString(),
                                        "ref:opplysningerOmArbeidssokerId" to generic.get("opplysningerOmArbeidssokerId")
                                            ?.toString(),
                                    ).toMap()
                                }.toList()
                        }
                        call.respond(records)
                    }
                }
            }
        }
    }.start(wait = true)
}

fun <K, V> Consumer<K, V>.asSequence(): Sequence<ConsumerRecord<K, V>> =
    generateSequence {
        this.poll(Duration.ofSeconds(2))
    }.flatten()