plugins {
    kotlin("android") version "1.9.22"
    id("com.android.application") version "8.2.2"
}

group = "com.leviathan.game"
version = "1.0.0"

val gdxVersion = "1.12.1"

val natives by configurations.creating

android {
    namespace = "com.leviathan.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.leviathan.boardgames"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(kotlin("stdlib"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

tasks.register("copyNatives") {
    doLast {
        val jniLibs = file("src/main/jniLibs")
        jniLibs.mkdirs()
        configurations.named("natives").get().files.forEach { jar ->
            val abi = jar.name.substringAfterLast("natives-").substringBefore(".jar")
            val targetDir = file("$jniLibs/$abi")
            targetDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(targetDir)
                include("**/*.so")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("copyNatives")
}
