plugins {
    kotlin("jvm") version "1.9.20"
    application
}

val pawUtilsVersion = "24.01.11.9-1"
val hopliteVersion = "2.8.0.RC3"

dependencies {
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")
    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.debug.app.AppKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}