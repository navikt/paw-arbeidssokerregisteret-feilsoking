plugins {
    kotlin("jvm") version "1.9.20"
    application
}

val pawUtilsVersion = "24.01.11.9-1"
val hopliteVersion = "2.8.0.RC3"

dependencies {
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.debug.app.AppKt")
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