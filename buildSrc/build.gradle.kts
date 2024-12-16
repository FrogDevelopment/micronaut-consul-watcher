plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.ajoberstar.grgit:grgit-gradle:5.3.0")
    implementation("org.jreleaser:jreleaser-gradle-plugin:1.15.0")
}

