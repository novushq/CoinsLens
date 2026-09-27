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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Coinlens"
include(":app")
include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":core:navigation")
include(":core:identify")
include(":core:ai")
include(":feature:capture")
include(":feature:result")
include(":feature:home")
include(":feature:collection")
include(":feature:share")
include(":feature:onboarding")
include(":feature:paywall")
include(":feature:settings")
