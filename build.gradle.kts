allprojects {
    apply(plugin = "idea")

    idea {
        module {
            isDownloadSources = true
            isDownloadJavadoc = true
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
