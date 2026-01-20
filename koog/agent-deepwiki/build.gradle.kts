plugins {
    kotlin("jvm") version "2.3.0"
    application
    kotlin("plugin.serialization") version "2.3.0"
    id("org.graalvm.buildtools.native") version "0.10.2"
}

group = "org.coralprotocol"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.coralprotocol.AgentKt")
}

dependencies {
    testImplementation(kotlin("test"))

    val koogVersion = "0.6.0"
    api("ai.koog:koog-agents:$koogVersion")
    api("ai.koog:agents-mcp:$koogVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    implementation(project(":shared"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    outputs.upToDateWhen { false }
}