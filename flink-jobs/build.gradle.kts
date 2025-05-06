import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "dev.gamov"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

val flinkVersion = "1.20.1"
val kafkaVersion = "3.6.1"
val icebergVersion = "1.9.0"
val flinkKafkaVersion = "3.4.0-1.20"

dependencies {
    // Common module
    implementation(project(":common"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")
    implementation("io.confluent:kafka-schema-registry-client:7.5.1")
    implementation("org.apache.avro:avro:1.11.3")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.5")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.7.5")

    // Flink Core
    implementation("org.apache.flink:flink-streaming-java:$flinkVersion")
    implementation("org.apache.flink:flink-clients:$flinkVersion")
    implementation("org.apache.flink:flink-runtime-web:$flinkVersion")

    // Flink Table API & SQL
    implementation("org.apache.flink:flink-table-api-java-bridge:$flinkVersion")
    implementation("org.apache.flink:flink-table-planner-loader:$flinkVersion")
    implementation("org.apache.flink:flink-table-runtime:$flinkVersion")

    // Flink Connectors
    implementation("org.apache.flink:flink-connector-kafka:$flinkKafkaVersion")
    implementation("org.apache.flink:flink-connector-files:$flinkVersion")
    implementation("org.apache.flink:flink-avro:$flinkVersion")
    implementation("org.apache.flink:flink-avro-confluent-registry:$flinkVersion")
    implementation("org.apache.flink:flink-json:$flinkVersion")

    // Iceberg
    implementation("org.apache.iceberg:iceberg-flink-runtime-1.20:$icebergVersion")
    implementation("org.apache.iceberg:iceberg-core:$icebergVersion")
    implementation("org.apache.iceberg:iceberg-api:$icebergVersion")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.apache.flink:flink-test-utils:$flinkVersion")
    testImplementation("org.apache.flink:flink-table-test-utils:$flinkVersion")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:kafka:1.19.3")

    // Generator module for tests
    testImplementation(project(":generator"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Default application is the stateless job
application {
    mainClass.set("dev.gamov.flightdemo.flink.StatelessJobKt")
}

// Create a task for the stateful job
tasks.register<JavaExec>("runStatefulJob") {
    group = "application"
    description = "Runs the stateful Flink job"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.gamov.flightdemo.flink.StatefulJobKt")
}

// Create a task for the stateless job
tasks.register<JavaExec>("runStatelessJob") {
    group = "application"
    description = "Runs the stateless Flink job"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.gamov.flightdemo.flink.StatelessJobKt")
}

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to "dev.gamov.flightdemo.flink.StatelessJobKt"
        )
    }
    archiveClassifier.set("")
    mergeServiceFiles()
}

// Fix dependency issues between shadowJar and distribution tasks
tasks.distZip {
    dependsOn(tasks.shadowJar)
}

tasks.distTar {
    dependsOn(tasks.shadowJar)
}

tasks.startScripts {
    dependsOn(tasks.shadowJar)
}

// Fix dependency issue with startShadowScripts
tasks.named("startShadowScripts") {
    dependsOn(tasks.jar)
}
