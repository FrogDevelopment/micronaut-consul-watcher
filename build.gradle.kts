plugins {
    id("io.micronaut.minimal.library") version "4.4.2"
}

group = "com.frogdevelopment.micronaut.consul"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val vertxConsul = "4.5.9"
    val commonsCollections4 = "4.4"
    val commonsLang3 = "3.17.0"
    val awaitility = "4.2.2"

    compileOnly(mn.lombok)
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

    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.assertj.core)
    testImplementation(mn.mockito.junit.jupiter)
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation(mn.logback.classic)

    testAnnotationProcessor(mn.lombok)
    testCompileOnly(mn.lombok)
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.frogdevelopment.micronaut.*")
    }
}
