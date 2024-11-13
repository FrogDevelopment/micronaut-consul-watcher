plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.ajoberstar.grgit:grgit-gradle:5.2.2")
    implementation("org.jreleaser:jreleaser-gradle-plugin:1.13.1")
}

