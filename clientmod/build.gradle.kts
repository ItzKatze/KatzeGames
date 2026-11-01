import java.util.Properties

plugins {
    id("net.fabricmc.fabric-loom")
    id("com.diffplug.spotless")
}

val paperVersion = providers.gradleProperty("paperVersion").get()
val minecraftVersion = paperVersion.substringBefore(".build")

version = "${project.version}+$minecraftVersion"

base {
    archivesName.set("KatzeGames-Client")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("fabricLoaderVersion").get()}")
    implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabricApiVersion").get()}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    val props = mapOf("version" to version, "minecraft_version" to minecraftVersion)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

val generateLanguageFiles = tasks.register("generateLanguageFiles") {
    val texts = rootProject.file("core/src/main/resources/lang/client")
    val output = layout.buildDirectory.dir("generated/lang")
    inputs.dir(texts)
    outputs.dir(output)
    doLast {
        fun read(language: String): Map<String, String> {
            val properties = Properties()
            texts.resolve("$language.properties").reader(Charsets.UTF_8).use { properties.load(it) }
            return properties.stringPropertyNames().associateWith { properties.getProperty(it) }.toSortedMap()
        }

        fun quote(text: String): String = buildString {
            append('"')
            for (c in text) {
                when {
                    c == '\\' -> append("\\\\")
                    c == '"' -> append("\\\"")
                    c < ' ' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
                    else -> append(c)
                }
            }
            append('"')
        }

        fun json(language: String): String =
            read(language).entries.joinToString(",\n", "{\n", "\n}\n") { "  ${quote(it.key)}: ${quote(it.value)}" }

        val outputDir = output.get().asFile
        outputDir.deleteRecursively()
        val langDir = outputDir.resolve("assets/katzegames/lang")
        langDir.mkdirs()
        langDir.resolve("en_us.json").writeText(json("en"), Charsets.UTF_8)
        val german = json("de")
        for (file in listOf("de_de", "de_at", "de_ch", "bar", "ksh", "nds_de", "sxu")) {
            langDir.resolve("$file.json").writeText(german, Charsets.UTF_8)
        }
    }
}

sourceSets.main {
    resources.srcDir(generateLanguageFiles)
}

spotless {
    java {
        target("src/**/*.java")
        removeUnusedImports()
        toggleOffOn()
        trimTrailingWhitespace()
        formatAnnotations()
    }
}
