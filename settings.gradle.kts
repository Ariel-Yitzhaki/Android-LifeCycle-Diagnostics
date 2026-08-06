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

rootProject.name = "Android-LifeCycle-Diagnostics"
include(":Diagnostics")
include(":sample-views")

// sample-compose is kept out of the repository. Included only when the directory is present
// locally, so a clone does not get a module with no directory behind it.
if (file("sample-compose").exists()) {
    include(":sample-compose")
}
