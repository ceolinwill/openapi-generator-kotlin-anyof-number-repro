pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // The CLI jar bundles its dependencies, so only the jar is needed, not its POM's snapshot parent.
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            metadataSources { artifact() }
            content { includeModule("org.openapitools", "openapi-generator-cli") }
        }
    }
}

rootProject.name = "openapi-generator-kotlin-anyof-number-repro"
