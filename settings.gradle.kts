pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Alloy"

include(":app")
include(":core:common")
include(":core:design")
include(":core:datastore")
include(":core:proc")
include(":core:netlocal")
include(":tooling:probe")

include(":modules:statspill")
include(":modules:scenes")
include(":modules:clip")
include(":modules:scratch")
