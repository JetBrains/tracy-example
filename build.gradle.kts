plugins {
    application
    kotlin("jvm") version "2.3.0"
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

val otelVersion = "1.44.1"

dependencies {
    implementation("com.openai:openai-java:4.16.0")
    implementation("io.opentelemetry:opentelemetry-sdk:$otelVersion")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:$otelVersion")
    implementation("io.opentelemetry:opentelemetry-exporter-logging:$otelVersion")
}

application {
    mainClass.set("tracy.example.app.AppKt")
}