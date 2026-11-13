package gg.itzkatze.katze.mobarmy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobCaptureTest {
    @Test void identityAndDamageDoNotSplitOtherwiseIdenticalVariants() {
        assertEquals(MobCapture.canonical("{id:'minecraft:drowned',UUID:[I;1,2,3,4],Health:2f,Pos:[1d,2d,3d],Age:-10,Equipment:{hand:'trident'}}"),
                MobCapture.canonical("{Equipment:{hand:'trident'},Age:-10,Health:20f,UUID:[I;5,6,7,8],id:'minecraft:drowned',Pos:[8d,7d,6d]}"));
    }
    @Test void equipmentAndNestedDataRemainPartOfTheVariant() {
        assertNotEquals(MobCapture.canonical("{id:'minecraft:drowned',Equipment:{hand:'trident'}}"), MobCapture.canonical("{id:'minecraft:drowned',Equipment:{hand:'air'}}"));
        String nbt = "{Health:4f,custom_name:'A, {quoted} \\\"name\\\"',attributes:[{id:'minecraft:max_health',base:40d}]}";
        assertTrue(MobCapture.canonical(nbt).contains("base:40d"));
        assertTrue(MobCapture.canonical(nbt).contains("A, {quoted}"));
    }
}
