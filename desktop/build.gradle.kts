plugins {
    kotlin("jvm")
    id("application")
    id("java-library")
}

application {
    mainClass.set("com.leviathan.game.DesktopLauncher")
}

group = "com.leviathan.game"
version = "1.0.0"

val gdxVersion = "1.12.1"

dependencies {
    implementation(project(":core"))
    implementation(kotlin("stdlib"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions { jvmTarget = "17" }
}
