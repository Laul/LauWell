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

rootProject.name = "LauWell"

// Pure Kotlin: no Android types, unit-tested on the JVM.
include(":core:model", ":core:domain", ":core:charts")

// Android: Room, Health Connect, Compose UI.
include(":core:data", ":app")
