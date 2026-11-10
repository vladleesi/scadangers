pluginManagement {
    repositories {
        google()
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
rootProject.name = "Scadangers"
include(":app")
include(":core:common", ":core:designsystem", ":core:database", ":core:security", ":core:testing")
include(":feature:dashboard", ":feature:apps", ":feature:device", ":feature:network")
include(":feature:nearby", ":feature:alerts", ":feature:settings")
