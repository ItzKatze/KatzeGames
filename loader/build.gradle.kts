plugins {
    java
    application
    id("com.gradleup.shadow") version "9.3.1"
}

val targetJavaVersion = 25
val paperVersion = providers.gradleProperty("paperVersion").get()
val minecraftVersion = paperVersion.substringBefore(".build")

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":forcebattle"))
    implementation(project(":allitems"))
    implementation(project(":deathswap"))
    implementation(project(":randomizer"))
    implementation(project(":itemrain"))
    implementation(project(":bingo"))
    implementation(project(":mobarmy"))
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    testImplementation("io.papermc.paper:paper-api:$paperVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveBaseName.set("KatzeGames")
    archiveClassifier.set("")
    archiveVersion.set("${project.version}+$minecraftVersion")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

val pluginJar = tasks.named<Jar>("shadowJar")
tasks.test {
    useJUnitPlatform()
    dependsOn(pluginJar)
    inputs.file(pluginJar.flatMap { it.archiveFile })
    systemProperty("katzegames.test.pluginJar", pluginJar.get().archiveFile.get().asFile.absolutePath)
}
