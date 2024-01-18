package no.nav.paw.arbeidssokerregisteret.debug.app

import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.config.kafka.KAFKA_CONFIG

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
}
