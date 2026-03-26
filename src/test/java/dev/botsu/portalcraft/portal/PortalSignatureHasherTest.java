package dev.botsu.portalcraft.portal;

import dev.botsu.portalcraft.portal.signature.PortalSignatureHasher;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PortalSignatureHasher}.
 *
 * <p>These tests run with plain JUnit 5 — no Minecraft bootstrap required.
 */
class PortalSignatureHasherTest {

    @Test
    void sameInputProducesSameHash() {
        Map<String, Integer> materials = Map.of("minecraft:stone", 12, "minecraft:obsidian", 4);
        assertEquals(
            PortalSignatureHasher.hash(materials),
            PortalSignatureHasher.hash(materials),
            "Repeated calls with the same input must produce the same hash"
        );
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        Map<String, Integer> stone = Map.of("minecraft:stone", 12);
        Map<String, Integer> granite = Map.of("minecraft:granite", 12);
        assertNotEquals(
            PortalSignatureHasher.hash(stone),
            PortalSignatureHasher.hash(granite),
            "Frames made of different materials must hash differently"
        );
    }

    @Test
    void orderIndependent() {
        // LinkedHashMap preserves insertion order — both maps have same content
        // but inserted in opposite order.
        Map<String, Integer> a = new HashMap<>();
        a.put("minecraft:stone", 10);
        a.put("minecraft:obsidian", 4);

        Map<String, Integer> b = new HashMap<>();
        b.put("minecraft:obsidian", 4);
        b.put("minecraft:stone", 10);

        assertEquals(
            PortalSignatureHasher.hash(a),
            PortalSignatureHasher.hash(b),
            "Insertion order must not affect the hash"
        );
    }

    @Test
    void emptyCompositionIsStable() {
        assertEquals(
            PortalSignatureHasher.hash(Map.of()),
            PortalSignatureHasher.hash(Map.of()),
            "Empty composition must produce a stable hash"
        );
    }
}
