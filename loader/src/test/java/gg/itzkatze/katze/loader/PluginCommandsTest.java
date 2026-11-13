package gg.itzkatze.katze.loader;

import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PluginCommandsTest {
    @Test void packagedPluginIncludesTheCustomArenaAndItsNbtReader() throws Exception {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("katzegames.test.pluginJar")).toFile())) {
            assertNotNull(jar.getJarEntry("arena/village.schem"));
            assertNotNull(jar.getJarEntry("net/kyori/adventure/nbt/BinaryTagIO.class"));
        }
    }

    @Test void packagedPluginDeclaresTheWavesCommandForPaperRegistration() throws Exception {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("katzegames.test.pluginJar")).toFile());
             var input = jar.getInputStream(jar.getJarEntry("plugin.yml"))) {
            PluginDescriptionFile description = new PluginDescriptionFile(input);
            assertTrue(description.getCommands().containsKey("waves"));
            assertTrue(description.getCommands().containsKey("tracker"));
            assertTrue(description.getCommands().containsKey("watch"));
        }
    }
}
