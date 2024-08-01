plugins {
    kotlin("jvm") version "2.0.0"
}

group = "pro.mezentsev.tracker"
version = "0.1.0-SNAPSHOT"

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlin.compiler.embeddable)
    testImplementation(libs.kotlin.compiler.testing)
    testImplementation(libs.compose.runtime)
    testImplementation(libs.compose.runtime)
}
