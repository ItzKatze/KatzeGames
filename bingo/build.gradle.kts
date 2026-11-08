plugins { java }

val packTools = sourceSets.create("packTools") {
    java.srcDir("tools")
}

repositories {
    exclusiveContent {
        forRepository { maven("https://repo.nexomc.com/releases/") }
        filter { includeModule("team.unnamed", "creative-api"); includeModule("team.unnamed", "creative-serializer-minecraft") }
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperVersion").get()}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    add(packTools.implementationConfigurationName, "com.google.code.gson:gson:2.13.2")
    add(packTools.implementationConfigurationName, "team.unnamed:creative-serializer-minecraft:1.15.4")
    testImplementation(packTools.output)
    testImplementation("team.unnamed:creative-serializer-minecraft:1.15.4")
    testRuntimeOnly(project(":forcebattle"))
}

tasks.test {
    useJUnitPlatform()
    inputs.files(fileTree("src/main/item-icons"))
    inputs.files(fileTree("src/main/mob-icons"))
}

tasks.register<JavaExec>("generateBingoPack") {
    classpath = packTools.runtimeClasspath
    mainClass.set("BuildPack")
    doFirst {
        if (args.isNullOrEmpty()) {
            val minecraftClientJar = providers.gradleProperty("minecraftClientJar").orNull
                ?: throw GradleException("Set -PminecraftClientJar=<path-to-minecraft-client.jar>")
            args(projectDir.absolutePath, minecraftClientJar)
        }
    }
}

val bingoPack = tasks.register<Zip>("bingoPack") {
    from("src/main/pack")
    archiveFileName.set("KatzeGames-Bingo.zip")
    destinationDirectory.set(layout.buildDirectory.dir("pack"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.processResources {
    from(bingoPack) { into("bingo") }
}
