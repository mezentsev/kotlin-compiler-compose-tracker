@file:Suppress("UnstableApiUsage")

includeBuild("./tracker-compose-compiler-plugin")

pluginManagement {
    includeBuild("./tracker-plugin")
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "tracker-compose"

include(
    ":showcase",
    ":tracker-core",
    ":tracker-profiler",
)
