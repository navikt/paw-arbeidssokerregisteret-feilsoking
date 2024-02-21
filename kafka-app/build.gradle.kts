plugins {
    kotlin("jvm") version "1.9.20"
    id("com.google.cloud.tools.jib") version "3.4.0"
    application
}
val jvmVersion = JavaVersion.VERSION_21
val image: String? by project

val pawUtilsVersion = "24.02.06.10-1"
val hopliteVersion = "2.8.0.RC3"

val navCommonModulesVersion = "3.2023.10.23_12.41-bafec3836d28"
val tokenSupportVersion = "3.1.5"

dependencies {
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")
    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")
    implementation("org.apache.kafka:kafka-clients:3.6.1")
    implementation("io.confluent:kafka-avro-serializer:7.6.0")
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)

}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.debug.app.AppKt")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    mavenNav("paw-arbeidssokerregisteret")
}

fun RepositoryHandler.mavenNav(repo: String): MavenArtifactRepository {
    val githubPassword: String by project

    return maven {
        setUrl("https://maven.pkg.github.com/navikt/$repo")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}

jib {
    from.image = "ghcr.io/navikt/baseimages/temurin:${jvmVersion.majorVersion}"
    to.image = "${image ?: rootProject.name }:${project.version}"
}