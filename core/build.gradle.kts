plugins {
    kotlin("jvm") version "1.9.22"
    id("java-library")
}

group = "com.leviathan.game"
version = "1.0.0"

val gdxVersion = "1.12.1"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.google.code.gson:gson:2.10.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions { jvmTarget = "17" }
}
