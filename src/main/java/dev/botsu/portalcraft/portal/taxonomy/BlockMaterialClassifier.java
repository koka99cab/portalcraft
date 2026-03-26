package dev.botsu.portalcraft.portal.taxonomy;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps block identifiers to {@link MaterialFamily} values.
 *
 * <p>The mapping is hardcoded at class-load time and covers the MVP whitelist.
 * Blocks not present in the map return {@link MaterialFamily#UNKNOWN}.
 *
 * <p>This class is intentionally free of Minecraft API imports so it can be
 * exercised in plain JUnit 5 unit tests without a game bootstrap.
 */
public final class BlockMaterialClassifier {

    private static final Map<String, MaterialFamily> REGISTRY = buildRegistry();

    private BlockMaterialClassifier() {}

    /**
     * Returns the {@link MaterialFamily} for a block identified by its namespaced ID
     * (e.g. {@code "minecraft:stone"}).
     *
     * @param blockId the full namespaced block identifier
     * @return the corresponding family, or {@link MaterialFamily#UNKNOWN}
     */
    public static MaterialFamily classifyById(String blockId) {
        return REGISTRY.getOrDefault(blockId, MaterialFamily.UNKNOWN);
    }

    private static Map<String, MaterialFamily> buildRegistry() {
        Map<String, MaterialFamily> m = new HashMap<>();

        // ── STONE family ────────────────────────────────────────────────────
        put(m, MaterialFamily.STONE,
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:stone_bricks",
            "minecraft:cracked_stone_bricks",
            "minecraft:mossy_stone_bricks",
            "minecraft:chiseled_stone_bricks",
            "minecraft:deepslate",
            "minecraft:cobbled_deepslate",
            "minecraft:polished_deepslate",
            "minecraft:deepslate_bricks",
            "minecraft:cracked_deepslate_bricks",
            "minecraft:deepslate_tiles",
            "minecraft:cracked_deepslate_tiles",
            "minecraft:chiseled_deepslate",
            "minecraft:blackstone",
            "minecraft:polished_blackstone",
            "minecraft:polished_blackstone_bricks",
            "minecraft:cracked_polished_blackstone_bricks",
            "minecraft:chiseled_polished_blackstone",
            "minecraft:bricks",
            "minecraft:granite",
            "minecraft:polished_granite",
            "minecraft:diorite",
            "minecraft:polished_diorite",
            "minecraft:andesite",
            "minecraft:polished_andesite",
            "minecraft:calcite"
        );

        // ── SAND / TERRACOTTA family ─────────────────────────────────────────
        put(m, MaterialFamily.SAND,
            "minecraft:sandstone",
            "minecraft:chiseled_sandstone",
            "minecraft:cut_sandstone",
            "minecraft:smooth_sandstone",
            "minecraft:red_sandstone",
            "minecraft:chiseled_red_sandstone",
            "minecraft:cut_red_sandstone",
            "minecraft:smooth_red_sandstone",
            "minecraft:terracotta",
            "minecraft:white_terracotta",
            "minecraft:orange_terracotta",
            "minecraft:magenta_terracotta",
            "minecraft:light_blue_terracotta",
            "minecraft:yellow_terracotta",
            "minecraft:lime_terracotta",
            "minecraft:pink_terracotta",
            "minecraft:gray_terracotta",
            "minecraft:light_gray_terracotta",
            "minecraft:cyan_terracotta",
            "minecraft:purple_terracotta",
            "minecraft:blue_terracotta",
            "minecraft:brown_terracotta",
            "minecraft:green_terracotta",
            "minecraft:red_terracotta",
            "minecraft:black_terracotta"
        );

        // ── CLAY family ──────────────────────────────────────────────────────
        put(m, MaterialFamily.CLAY,
            "minecraft:mud_bricks",
            "minecraft:packed_mud"
        );

        // ── ICE family ───────────────────────────────────────────────────────
        put(m, MaterialFamily.ICE,
            "minecraft:ice",
            "minecraft:packed_ice",
            "minecraft:blue_ice",
            "minecraft:snow_block"
        );

        // ── MARINE family ────────────────────────────────────────────────────
        put(m, MaterialFamily.MARINE,
            "minecraft:prismarine",
            "minecraft:prismarine_bricks",
            "minecraft:dark_prismarine",
            "minecraft:sea_lantern"
        );

        // ── INFERNAL family ──────────────────────────────────────────────────
        put(m, MaterialFamily.INFERNAL,
            "minecraft:netherrack",
            "minecraft:basalt",
            "minecraft:smooth_basalt",
            "minecraft:polished_basalt",
            "minecraft:nether_bricks",
            "minecraft:cracked_nether_bricks",
            "minecraft:chiseled_nether_bricks",
            "minecraft:soul_sand",
            "minecraft:soul_soil",
            "minecraft:crimson_stem",
            "minecraft:stripped_crimson_stem",
            "minecraft:warped_stem",
            "minecraft:stripped_warped_stem",
            "minecraft:nether_wart_block",
            "minecraft:warped_wart_block",
            "minecraft:shroomlight"
        );

        // ── END family ───────────────────────────────────────────────────────
        put(m, MaterialFamily.END,
            "minecraft:end_stone",
            "minecraft:end_stone_bricks",
            "minecraft:purpur_block",
            "minecraft:purpur_pillar"
        );

        // ── VEGETAL family ───────────────────────────────────────────────────
        put(m, MaterialFamily.VEGETAL,
            "minecraft:moss_block",
            "minecraft:muddy_mangrove_roots"
        );

        // ── PRECIOUS family ──────────────────────────────────────────────────
        put(m, MaterialFamily.PRECIOUS,
            "minecraft:quartz_block",
            "minecraft:smooth_quartz",
            "minecraft:chiseled_quartz_block",
            "minecraft:quartz_pillar",
            "minecraft:amethyst_block",
            "minecraft:budding_amethyst",
            "minecraft:gold_block",
            "minecraft:iron_block",
            "minecraft:diamond_block",
            "minecraft:emerald_block",
            "minecraft:lapis_block",
            "minecraft:netherite_block"
        );

        // ── DARK family ──────────────────────────────────────────────────────
        put(m, MaterialFamily.DARK,
            "minecraft:obsidian",
            "minecraft:crying_obsidian",
            "minecraft:coal_block"
        );

        // ── LIGHT family ─────────────────────────────────────────────────────
        put(m, MaterialFamily.LIGHT,
            "minecraft:glowstone"
        );

        // ── ANCIENT family ───────────────────────────────────────────────────
        put(m, MaterialFamily.ANCIENT,
            "minecraft:bone_block",
            "minecraft:ancient_debris"
        );

        // ── COLOR family (April Fool — wool + full-block glass) ──────────────
        put(m, MaterialFamily.COLOR,
            "minecraft:white_wool",    "minecraft:orange_wool",     "minecraft:magenta_wool",
            "minecraft:light_blue_wool","minecraft:yellow_wool",    "minecraft:lime_wool",
            "minecraft:pink_wool",     "minecraft:gray_wool",       "minecraft:light_gray_wool",
            "minecraft:cyan_wool",     "minecraft:purple_wool",     "minecraft:blue_wool",
            "minecraft:brown_wool",    "minecraft:green_wool",      "minecraft:red_wool",
            "minecraft:black_wool",
            "minecraft:glass",
            "minecraft:white_stained_glass",   "minecraft:orange_stained_glass",
            "minecraft:magenta_stained_glass",  "minecraft:light_blue_stained_glass",
            "minecraft:yellow_stained_glass",   "minecraft:lime_stained_glass",
            "minecraft:pink_stained_glass",     "minecraft:gray_stained_glass",
            "minecraft:light_gray_stained_glass","minecraft:cyan_stained_glass",
            "minecraft:purple_stained_glass",   "minecraft:blue_stained_glass",
            "minecraft:brown_stained_glass",    "minecraft:green_stained_glass",
            "minecraft:red_stained_glass",      "minecraft:black_stained_glass"
        );

        return Map.copyOf(m);
    }

    private static void put(Map<String, MaterialFamily> map, MaterialFamily family, String... ids) {
        for (String id : ids) {
            map.put(id, family);
        }
    }
}
