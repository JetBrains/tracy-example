plugins {
    application
    id("org.jetbrains.ai.tracy") version "0.0.29"
    kotlin("jvm") version "2.3.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.ai.tracy:tracy-core:0.0.29")
    implementation("org.jetbrains.ai.tracy:tracy-openai:0.0.29")
    implementation("com.openai:openai-java:4.16.0")
}

application {
    mainClass.set("tracy.example.app.AppKt")
}