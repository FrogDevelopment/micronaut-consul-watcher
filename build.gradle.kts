plugins {
    id("io.micronaut.minimal.library") version "4.4.3"
}

group = "com.frogdevelopment.micronaut.consul"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val vertxConsul = "4.5.10"
    val commonsCollections4 = "4.4"
    val commonsLang3 = "3.17.0"

    compileOnly(mn.lombok)
    compileOnly(mn.micronaut.serde.jackson)

    annotationProcessor(mn.lombok)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(mn.micronaut.serde.processor)

    implementation(mn.caffeine)
    implementation(mn.micronaut.reactor)
    implementation(mn.micronaut.discovery.client)
    implementation("io.vertx:vertx-consul-client:${vertxConsul}")
    implementation("org.apache.commons:commons-collections4:${commonsCollections4}")
    implementation("org.apache.commons:commons-lang3:${commonsLang3}")
    implementation(mn.guava)
    implementation(mn.snakeyaml)

    val awaitility = "4.2.2"
    val testcontainers = "1.20.1"

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.assertj.core)
    testImplementation(mn.mockito.junit.jupiter)
    testImplementation(mn.logback.classic)
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainers")
    testImplementation("org.testcontainers:consul:$testcontainers")

    testAnnotationProcessor(mn.lombok)
    testCompileOnly(mn.lombok)
    testRuntimeOnly(mn.micronaut.serde.jackson)
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.frogdevelopment.micronaut.*")
    }
}
