plugins {
    id("io.micronaut.minimal.library") version "4.4.4"
    id("com.frogdevelopment.version-convention")
    id("com.frogdevelopment.jreleaser.deploy-convention")
    id("com.frogdevelopment.jreleaser.publish-convention")
}

repositories {
    mavenCentral()
}

dependencies {

    annotationProcessor(mn.lombok)
    annotationProcessor(mn.micronaut.inject.java)

    compileOnly(mn.lombok)
    compileOnly(mn.snakeyaml)
    compileOnly(mn.micronaut.jackson.core)

    implementation(mn.micronaut.discovery.client)
    implementation(mn.micronaut.reactor)
    implementation(mn.guava)

    // ----------- TESTS -----------
    val awaitility = "4.2.2"
    val testcontainers = mn.versions.testcontainers.asProvider().get()
    val commonsLang3 = "3.17.0"

    testAnnotationProcessor(mn.lombok)
    testAnnotationProcessor(mn.micronaut.inject.java)

    testCompileOnly(mn.lombok)

    testRuntimeOnly(mn.micronaut.serde.jackson)
    testRuntimeOnly(mn.snakeyaml)

    testImplementation(mn.logback.classic)
    testImplementation(mn.assertj.core)
    testImplementation(mn.junit.jupiter.params)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.mockito.junit.jupiter)
    testImplementation(mn.testcontainers.consul)
    testImplementation("org.apache.commons:commons-lang3:${commonsLang3}")
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainers")
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.frogdevelopment.micronaut.consul.*")
    }
}
