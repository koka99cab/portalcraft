package dev.botsu.portalcraft.portal.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.CheckerboardColumnBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * F — Novelty (April Fool) generator helpers.
 *
 * <p>Activated for the {@link dev.botsu.portalcraft.portal.resolver.PortalFamily#COLOR COLOR}
 * family. Produces a dramatically chaotic dimension by combining three elements:
 *
 * <ol>
 *   <li><b>Altitude-banded surface blocks</b>: the wool / stained-glass blocks found in
 *       the portal frame are assigned to elevation bands (peaks → valleys), so AMPLIFIED
 *       terrain shows vivid horizontal colour stripes.</li>
 *   <li><b>Checkerboard biome distribution</b>: five visually distinct biomes alternate in
 *       a {@link CheckerboardColumnBiomeSource} at 32-block resolution, producing rapid
 *       climate and mob-spawn transitions.</li>
 *   <li><b>Forced AMPLIFIED noise</b>: regardless of frame diversity, terrain always uses
 *       the amplified noise template for maximum vertical variation.</li>
 * </ol>
 *
 * <h2>Altitude-band thresholds (AMPLIFIED, Y range −64 to 320)</h2>
 * <pre>
 *   ≥ 200  → block[0]   (extreme peaks)
 *   ≥ 140  → block[1]   (upper slopes)
 *   ≥  90  → block[2]   (midlands)
 *   ≥  50  → block[3]   (lowlands)
 *   else   → block[N-1] (valleys / sea-level areas)
 *   UNDER_FLOOR → stone  (neutral subsurface; avoids wool lag underground)
 * </pre>
 *
 * <h2>Checkerboard biomes</h2>
 * <p>{@link CheckerboardColumnBiomeSource} uses scale=3 → 8 biome-columns per tile
 * = 32 blocks, giving frequent but not completely disorienting biome changes.
 * The fixed palette is chosen for visual diversity and colour variety:
 * FLOWER_FOREST, CHERRY_GROVE, SUNFLOWER_PLAINS, BAMBOO_JUNGLE, BADLANDS.
 */
public final class NoveltyGenerator {

    private NoveltyGenerator() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Extracts the wool / stained-glass {@link BlockState}s present in the portal frame,
     * ordered from most common to least common.
     *
     * <p>Blocks with no known dye-block mapping are silently discarded.
     * Returns a list of 1–5 entries (duplicates collapsed to the dominant per-colour).
     * Falls back to a single white-wool entry for completely unrecognised frames.
     *
     * @param canonicalSource canonical string, e.g. {@code "minecraft:red_wool:8,...,w:3,h:5"}
     * @return ordered list of BlockStates, most-frequent first; never empty
     */
    public static List<BlockState> extractFrameBlocks(String canonicalSource) {
        Map<String, Integer> materials = parseCanonical(canonicalSource);

        // Filter to dye blocks only, accumulate counts
        Map<String, Integer> dyeCounts = new HashMap<>();
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            Block b = DYE_BLOCKS.get(entry.getKey());
            if (b != null) {
                dyeCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        if (dyeCounts.isEmpty()) {
            return List.of(Blocks.WHITE_WOOL.defaultBlockState()); // safe fallback
        }

        // Sort by count descending, cap at 5
        List<BlockState> result = dyeCounts.entrySet().stream()
            .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed())
            .limit(5)
            .map(e -> DYE_BLOCKS.get(e.getKey()).defaultBlockState())
            .toList();

        return result;
    }

    /**
     * Builds the novelty surface rule for the given block list.
     *
     * <p>Each block is assigned an altitude band (from highest to lowest), so that
     * AMPLIFIED terrain shows vivid horizontal colour stripes. The sub-surface layer
     * ({@link SurfaceRules#UNDER_FLOOR}) is always stone to avoid performance issues
     * from massive wool fill underground.
     *
     * @param blocks ordered block list (most-prominent first); must be non-empty
     * @return a {@link SurfaceRules#sequence} covering ON_FLOOR + UNDER_FLOOR
     */
    public static RuleSource buildSurfaceRules(List<BlockState> blocks) {
        List<RuleSource> rules = new ArrayList<>();

        // ── Altitude bands (ON_FLOOR only) ────────────────────────────────────
        int n = blocks.size();
        int[] thresholds = altitudeThresholds(n); // e.g. [200, 140, 90, 50] for n=5

        for (int i = 0; i < thresholds.length; i++) {
            rules.add(SurfaceRules.ifTrue(
                SurfaceRules.yBlockCheck(VerticalAnchor.absolute(thresholds[i]), 0),
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                    SurfaceRules.state(blocks.get(i)))
            ));
        }
        // Last block: floor at any remaining elevation (valleys)
        rules.add(SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
            SurfaceRules.state(blocks.get(n - 1))));

        // ── Subsurface (neutral stone under surface) ───────────────────────────
        rules.add(SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
            SurfaceRules.state(Blocks.STONE.defaultBlockState())));

        return SurfaceRules.sequence(rules.toArray(RuleSource[]::new));
    }

    /**
     * Builds a {@link CheckerboardColumnBiomeSource} with five visually diverse biomes
     * at scale 3 (32-block tile size).
     *
     * <p>The five biomes are chosen to maximise colour variety and visual impact with
     * AMPLIFIED terrain:
     * FLOWER_FOREST, CHERRY_GROVE, SUNFLOWER_PLAINS, BAMBOO_JUNGLE, BADLANDS.
     *
     * @param biomeGetter holder getter for the current registry access
     */
    public static CheckerboardColumnBiomeSource buildCheckerboardBiomeSource(
            HolderGetter<Biome> biomeGetter) {

        List<Holder<Biome>> holders = new ArrayList<>(CHECKERBOARD_BIOMES.size());
        for (ResourceKey<Biome> key : CHECKERBOARD_BIOMES) {
            holders.add(biomeGetter.getOrThrow(key));
        }

        // scale=3 → biome tile = 2^3 = 8 biome-columns = 32 blocks
        return new CheckerboardColumnBiomeSource(HolderSet.direct(holders), 3);
    }

    // ── Altitude threshold helpers ────────────────────────────────────────────

    /**
     * Returns the Y thresholds for {@code n} blocks:
     * {@code thresholds.length == n - 1}, selecting bands from highest to lowest.
     *
     * <pre>
     *   n=1  → []
     *   n=2  → [100]
     *   n=3  → [150, 80]
     *   n=4  → [180, 120, 70]
     *   n=5  → [200, 140, 90, 50]
     * </pre>
     */
    static int[] altitudeThresholds(int n) {
        return switch (n) {
            case 1  -> new int[]{};
            case 2  -> new int[]{ 100 };
            case 3  -> new int[]{ 150, 80 };
            case 4  -> new int[]{ 180, 120, 70 };
            default -> new int[]{ 200, 140, 90, 50 };  // 5+ blocks
        };
    }

    // ── Checkerboard biome set ────────────────────────────────────────────────

    /** Five visually vibrant biomes used for the novelty checkerboard pattern. */
    private static final List<ResourceKey<Biome>> CHECKERBOARD_BIOMES = List.of(
        Biomes.FLOWER_FOREST,
        Biomes.CHERRY_GROVE,
        Biomes.SUNFLOWER_PLAINS,
        Biomes.BAMBOO_JUNGLE,
        Biomes.BADLANDS
    );

    // ── Dye block lookup ──────────────────────────────────────────────────────

    /**
     * Static map from block ID → {@link Block} for all wool, stained-glass, and plain
     * glass variants. Used by {@link #extractFrameBlocks}.
     */
    private static final Map<String, Block> DYE_BLOCKS = buildDyeBlockMap();

    private static Map<String, Block> buildDyeBlockMap() {
        Map<String, Block> m = new HashMap<>();

        // Plain glass → white
        m.put("minecraft:glass",                  Blocks.GLASS);

        // Wool
        m.put("minecraft:white_wool",             Blocks.WHITE_WOOL);
        m.put("minecraft:light_gray_wool",        Blocks.LIGHT_GRAY_WOOL);
        m.put("minecraft:gray_wool",              Blocks.GRAY_WOOL);
        m.put("minecraft:black_wool",             Blocks.BLACK_WOOL);
        m.put("minecraft:brown_wool",             Blocks.BROWN_WOOL);
        m.put("minecraft:red_wool",               Blocks.RED_WOOL);
        m.put("minecraft:orange_wool",            Blocks.ORANGE_WOOL);
        m.put("minecraft:yellow_wool",            Blocks.YELLOW_WOOL);
        m.put("minecraft:lime_wool",              Blocks.LIME_WOOL);
        m.put("minecraft:green_wool",             Blocks.GREEN_WOOL);
        m.put("minecraft:cyan_wool",              Blocks.CYAN_WOOL);
        m.put("minecraft:light_blue_wool",        Blocks.LIGHT_BLUE_WOOL);
        m.put("minecraft:blue_wool",              Blocks.BLUE_WOOL);
        m.put("minecraft:purple_wool",            Blocks.PURPLE_WOOL);
        m.put("minecraft:magenta_wool",           Blocks.MAGENTA_WOOL);
        m.put("minecraft:pink_wool",              Blocks.PINK_WOOL);

        // Stained glass
        m.put("minecraft:white_stained_glass",       Blocks.WHITE_STAINED_GLASS);
        m.put("minecraft:light_gray_stained_glass",  Blocks.LIGHT_GRAY_STAINED_GLASS);
        m.put("minecraft:gray_stained_glass",        Blocks.GRAY_STAINED_GLASS);
        m.put("minecraft:black_stained_glass",       Blocks.BLACK_STAINED_GLASS);
        m.put("minecraft:brown_stained_glass",       Blocks.BROWN_STAINED_GLASS);
        m.put("minecraft:red_stained_glass",         Blocks.RED_STAINED_GLASS);
        m.put("minecraft:orange_stained_glass",      Blocks.ORANGE_STAINED_GLASS);
        m.put("minecraft:yellow_stained_glass",      Blocks.YELLOW_STAINED_GLASS);
        m.put("minecraft:lime_stained_glass",        Blocks.LIME_STAINED_GLASS);
        m.put("minecraft:green_stained_glass",       Blocks.GREEN_STAINED_GLASS);
        m.put("minecraft:cyan_stained_glass",        Blocks.CYAN_STAINED_GLASS);
        m.put("minecraft:light_blue_stained_glass",  Blocks.LIGHT_BLUE_STAINED_GLASS);
        m.put("minecraft:blue_stained_glass",        Blocks.BLUE_STAINED_GLASS);
        m.put("minecraft:purple_stained_glass",      Blocks.PURPLE_STAINED_GLASS);
        m.put("minecraft:magenta_stained_glass",     Blocks.MAGENTA_STAINED_GLASS);
        m.put("minecraft:pink_stained_glass",        Blocks.PINK_STAINED_GLASS);

        return Map.copyOf(m);
    }

    // ── Canonical string parser ───────────────────────────────────────────────

    /** Parses {@code "blockA:count,...,w:W,h:H"} → {@code Map<blockId, count>}. */
    private static Map<String, Integer> parseCanonical(String canonicalSource) {
        if (canonicalSource == null || canonicalSource.isEmpty()) return Map.of();
        Map<String, Integer> result = new HashMap<>();
        for (String part : canonicalSource.split(",")) {
            if (part.startsWith("w:") || part.startsWith("h:")) continue;
            int last = part.lastIndexOf(':');
            if (last < 1) continue;
            String id = part.substring(0, last);
            try {
                int count = Integer.parseInt(part.substring(last + 1));
                if (count > 0) result.put(id, count);
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }
}
