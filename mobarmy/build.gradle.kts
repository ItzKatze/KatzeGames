plugins { java }

dependencies {
    implementation(project(":core"))
    implementation("net.kyori:adventure-nbt:5.2.0")
    testImplementation("io.papermc.paper:paper-api:${providers.gradleProperty("paperVersion").get()}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.test { useJUnitPlatform() }
