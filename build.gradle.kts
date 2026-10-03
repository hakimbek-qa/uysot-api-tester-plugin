plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "uz.uysot"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

intellij {
    version.set("2024.1")
    type.set("IC")
    downloadSources.set(false)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("251.*")
    }

    buildSearchableOptions {
        enabled = false
    }

    signPlugin {
        enabled.set(false)
    }

    publishPlugin {
        enabled.set(false)
    }
}
