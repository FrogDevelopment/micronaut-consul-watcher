plugins {
    alias(libs.plugins.micronaut.minimal.library)
    alias(libs.plugins.frogdevelopment.version)
    alias(libs.plugins.frogdevelopment.publish)
    alias(libs.plugins.frogdevelopment.deploy)
    alias(libs.plugins.sonar)
    jacoco
}

repositories {
    mavenCentral()
}

dependencies {

    annotationProcessor(mn.lombok)
    annotationProcessor(mn.micronaut.inject.java)

    compileOnly(mn.lombok)
    compileOnly(mn.snakeyaml)

    implementation(mn.micronaut.jackson.databind)
    implementation(mn.micronaut.serde.jackson)
    implementation(mn.micronaut.discovery.client)
    implementation(mn.micronaut.reactor)
    implementation(mn.micronaut.retry)
    implementation(mn.guava)

    // ----------- TESTS -----------
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
    testImplementation(libs.commons.lang3)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers.junit5)
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.frogdevelopment.micronaut.consul.*")
    }
}

jacoco {
    toolVersion = libs.versions.tools.jacoco.get()
}

tasks {
    named<Test>("test") {
        reports {
            html.required.set(false)
        }
    }

    named<JacocoReport>("jacocoTestReport") {
        reports {
            xml.required.set(true)
            csv.required.set(false)
            html.required.set(false)
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "FrogDevelopment_micronaut-consul-watcher")
        property("sonar.organization", "frogdevelopment")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.junit.reportPaths", "build/test-results/test")
        property("sonar.sources", "src/main/")
        property("sonar.tests", "src/test/")
        property("sonar.inclusions", "src/main/**/*")
        property("sonar.test.exclusions", "src/test/**/*")
    }
}
