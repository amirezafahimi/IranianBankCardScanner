// A general-purpose Maven mirror, listed last in both repository blocks below.
//
// It is there for contributors on networks where dl.google.com is unusable: it
// answers, but serves a 404 for every artifact, so neither the Android Gradle
// Plugin nor any androidx dependency can be resolved through google(). Gradle
// treats a 404 as a miss and moves on to the next repository, so for everyone
// else — and in CI — resolution still finishes at google() or mavenCentral() and
// this entry is never reached.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.myket.ir") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.myket.ir") }
    }
}

rootProject.name = "cardscanner"

include(":cardscanner")
include(":sample")
