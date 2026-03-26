package dev.botsu.portalcraft.portal;

import dev.botsu.portalcraft.portal.analysis.PortalStructure;
import dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature;
import dev.botsu.portalcraft.portal.canonical.PortalCanonicalizer;
import dev.botsu.portalcraft.portal.frame.PortalFrame;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import dev.botsu.portalcraft.portal.identity.PortalIdentityDeriver;
import dev.botsu.portalcraft.portal.taxonomy.MaterialFamily;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static dev.botsu.portalcraft.portal.PortalFrameShapeTest.buildFrame;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PortalCanonicalizer} (Stage 2) and {@link PortalIdentityDeriver} (Stage 3).
 *
 * <p>All tests use pure-Java frame construction via {@link PortalFrameShapeTest#buildFrame}
 * and plain {@link Map} material counts — no Minecraft registry access required.
 */
class PortalCanonicalizerTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static CanonicalPortalSignature canonical(PortalFrame frame, Map<String, Integer> materials) {
        return PortalCanonicalizer.canonicalize(new PortalStructure(frame, materials));
    }

    private static PortalIdentity identity(PortalFrame frame, Map<String, Integer> materials) {
        return PortalIdentityDeriver.derive(canonical(frame, materials));
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    void determinismTest() {
        PortalFrame frame = buildFrame(4, 5, Direction.Axis.X);
        Map<String, Integer> materials = Map.of(
            "minecraft:stone", 18,
            "minecraft:cobblestone", 4
        );
        PortalIdentity id1 = identity(frame, materials);
        PortalIdentity id2 = identity(frame, materials);
        assertEquals(id1.stableId(), id2.stableId(),
            "Same frame + materials must always produce the same stableId");
    }

    @Test
    void differentSizesDifferentIds() {
        Map<String, Integer> materials = Map.of("minecraft:stone", 22);
        PortalIdentity id1 = identity(buildFrame(4, 5, Direction.Axis.X), materials);
        PortalIdentity id2 = identity(buildFrame(3, 4, Direction.Axis.X), materials);
        assertNotEquals(id1.stableId(), id2.stableId(),
            "Different inner dimensions must produce different stableIds");
    }

    // ── Axis invariance ───────────────────────────────────────────────────────

    @Test
    void axisInvariance_sameId() {
        Map<String, Integer> materials = Map.of(
            "minecraft:stone", 12,
            "minecraft:cobblestone", 6
        );
        PortalIdentity idX = identity(buildFrame(4, 5, Direction.Axis.X), materials);
        PortalIdentity idZ = identity(buildFrame(4, 5, Direction.Axis.Z), materials);
        assertEquals(idX.stableId(), idZ.stableId(),
            "Same recipe built on X or Z axis must produce the same stableId");
    }

    @Test
    void axisInvariance_sameCanonicalString() {
        Map<String, Integer> materials = Map.of("minecraft:stone", 22);
        CanonicalPortalSignature sigX = canonical(buildFrame(3, 4, Direction.Axis.X), materials);
        CanonicalPortalSignature sigZ = canonical(buildFrame(3, 4, Direction.Axis.Z), materials);
        assertEquals(sigX.canonicalString(), sigZ.canonicalString(),
            "Axis must not appear in the canonical string");
    }

    // ── April Fool flag ───────────────────────────────────────────────────────

    @Test
    void aprilFoolFlagPropagates() {
        PortalFrame frame = buildFrame(2, 3, Direction.Axis.X, /* aprilFool= */ true);
        Map<String, Integer> materials = Map.of("minecraft:white_wool", 14);
        CanonicalPortalSignature sig = canonical(frame, materials);
        assertTrue(sig.aprilFool(), "aprilFool=true on frame must be reflected in canonical signature");
    }

    @Test
    void aprilFoolFalseByDefault() {
        PortalFrame frame = buildFrame(2, 3, Direction.Axis.X);
        Map<String, Integer> materials = Map.of("minecraft:stone", 14);
        CanonicalPortalSignature sig = canonical(frame, materials);
        assertFalse(sig.aprilFool());
    }

    // ── Dominant block ────────────────────────────────────────────────────────

    @Test
    void dominantBlockDetection() {
        PortalFrame frame = buildFrame(3, 4, Direction.Axis.X);
        Map<String, Integer> materials = Map.of(
            "minecraft:stone", 10,
            "minecraft:cobblestone", 5,
            "minecraft:stone_bricks", 3
        );
        CanonicalPortalSignature sig = canonical(frame, materials);
        assertEquals("minecraft:stone", sig.dominantBlock(),
            "The block with the highest count should be dominant");
    }

    // ── Diversity score ───────────────────────────────────────────────────────

    @Test
    void diversityScore() {
        PortalFrame frame = buildFrame(3, 4, Direction.Axis.X);
        Map<String, Integer> materials = Map.of(
            "minecraft:stone", 8,
            "minecraft:cobblestone", 5,
            "minecraft:stone_bricks", 5
        );
        CanonicalPortalSignature sig = canonical(frame, materials);
        assertEquals(3, sig.diversityScore(), "3 distinct block types → diversityScore=3");
    }

    // ── Family classification ─────────────────────────────────────────────────

    @Test
    void familyClassification_stone() {
        PortalFrame frame = buildFrame(3, 4, Direction.Axis.X);
        Map<String, Integer> materials = Map.of(
            "minecraft:stone", 12,
            "minecraft:cobblestone", 6
        );
        CanonicalPortalSignature sig = canonical(frame, materials);
        assertEquals(MaterialFamily.STONE, sig.dominantFamily(),
            "Stone-heavy frame should classify as STONE family");
    }

    @Test
    void familyClassification_color() {
        PortalFrame frame = buildFrame(2, 3, Direction.Axis.X, true);
        Map<String, Integer> materials = Map.of(
            "minecraft:white_wool", 8,
            "minecraft:red_wool", 6
        );
        CanonicalPortalSignature sig = canonical(frame, materials);
        assertEquals(MaterialFamily.COLOR, sig.dominantFamily(),
            "Wool-heavy April Fool frame should classify as COLOR family");
    }

    @Test
    void familyCounts_aggregated() {
        PortalFrame frame = buildFrame(4, 5, Direction.Axis.X);
        Map<String, Integer> materials = Map.of(
            "minecraft:stone", 10,
            "minecraft:cobblestone", 5,    // both STONE
            "minecraft:purpur_block", 7    // END
        );
        CanonicalPortalSignature sig = canonical(frame, materials);
        assertEquals(15, sig.familyCounts().getOrDefault(MaterialFamily.STONE, 0),
            "stone(10) + cobblestone(5) should aggregate to 15 in STONE family");
        assertEquals(7, sig.familyCounts().getOrDefault(MaterialFamily.END, 0),
            "purpur_block(7) should aggregate to 7 in END family");
    }
}
