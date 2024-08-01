import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `kotlin-dsl`
}

group = "pro.mezentsev.tracker"
version = "0.1"

gradlePlugin {
    plugins {
        create("tracker-compose-compiler-plugin") {
            id = "pro.mezentsev.tracker.compose"
            implementationClass = "pro.mezentsev.tracker.plugin.TrackerComposeCompilerPlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("gradle-plugin-api"))

    implementation(libs.tools.gradle)
    implementation(libs.tools.gradleApi)
    implementation(libs.tools.common)
    implementation(libs.tools.sdkCommon)
    implementation(libs.commons.io)
    implementation(libs.asm.utils)
    implementation(libs.asm.commons)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation(libs.test.rules)
    testImplementation(libs.test.runner)
    testImplementation(libs.junit.jupiter)
}
