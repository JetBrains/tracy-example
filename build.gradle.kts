plugins {
    application
    kotlin("jvm") version "2.3.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.openai:openai-java:4.16.0")
}

application {
    mainClass.set("tracy.example.app.AppKt")
}