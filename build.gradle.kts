import org.gradle.internal.os.OperatingSystem

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    apply(plugin = "idea")

    idea {
        module {
            isDownloadSources = true
            isDownloadJavadoc = true
        }
    }
}
