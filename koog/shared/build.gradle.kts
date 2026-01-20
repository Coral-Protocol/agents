plugins {
    kotlin("jvm") version "2.3.0"
}

group = "org.coralprotocol"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    val ktorVersion = "3.3.3"
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-cio:${ktorVersion}")

    val koogVersion = "0.6.0"
    api("ai.koog:koog-agents:$koogVersion")
    api("ai.koog:agents-mcp:$koogVersion")

    val hopliteVersion = "2.9.0"
    implementation("com.sksamuel.hoplite:hoplite-core:${hopliteVersion}")
}

tasks.test {
    useJUnitPlatform()
}