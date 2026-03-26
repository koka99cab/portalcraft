package dev.botsu.portalcraft.portal.dimension;

import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers and provides named {@link BlockPalette}s for use in procedural dimension generation.
 *
 * <h2>Design</h2>
 * <p>Each palette is keyed by the same string that appears in {@link PortalGenerationPreset#blockPalette()}.
 * This indirection means a family's block selection can be changed or overridden without touching
 * {@link FamilyPresetRegistry} — only this registry needs updating.
 *
 * <p>Future extensions:
 * <ul>
 *   <li>Load additional palettes from JSON datapacks.</li>
 *   <li>Allow players/operators to register custom palettes at runtime.</li>
 *   <li>Version palettes with a schema field for migration support.</li>
 * </ul>
 *
 * <h2>Registered palettes</h2>
 * <table>
 *   <tr><th>Key</th><th>Surface</th><th>Under</th><th>Deep</th><th>Ceiling</th></tr>
 *   <tr><td>stone</td><td>cobblestone</td><td>stone</td><td>stone</td><td>—</td></tr>
 *   <tr><td>desert</td><td>sand</td><td>sandstone</td><td>sandstone</td><td>—</td></tr>
 *   <tr><td>clay</td><td>clay</td><td>terracotta</td><td>terracotta</td><td>—</td></tr>
 *   <tr><td>marine</td><td>prismarine_bricks</td><td>prismarine</td><td>prismarine</td><td>dark_prismarine</td></tr>
 *   <tr><td>end</td><td>purpur_block</td><td>end_stone_bricks</td><td>end_stone</td><td>—</td></tr>
 *   <tr><td>infernal</td><td>netherrack</td><td>basalt</td><td>netherrack</td><td>netherrack</td></tr>
 *   <tr><td>jungle</td><td>grass_block</td><td>dirt</td><td>dirt</td><td>—</td></tr>
 *   <tr><td>crystal</td><td>gold_block</td><td>calcite</td><td>calcite</td><td>—</td></tr>
 *   <tr><td>dark</td><td>basalt</td><td>blackstone</td><td>blackstone</td><td>—</td></tr>
 *   <tr><td>light</td><td>white_concrete</td><td>smooth_quartz</td><td>smooth_quartz</td><td>—</td></tr>
 *   <tr><td>ancient</td><td>cobbled_deepslate</td><td>deepslate</td><td>deepslate</td><td>—</td></tr>
 *   <tr><td>ice</td><td>snow_block</td><td>ice</td><td>packed_ice</td><td>—</td></tr>
 *   <tr><td>color</td><td>pink_concrete</td><td>lime_concrete</td><td>white_concrete</td><td>—</td></tr>
 * </table>
 */
public final class BlockPaletteRegistry {

    private BlockPaletteRegistry() {}

    private static final Map<String, BlockPalette> PALETTES = buildPalettes();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the {@link BlockPalette} for the given key, falling back to the
     * {@code "stone"} palette if the key is unknown.
     *
     * @param id palette key as stored in {@link PortalGenerationPreset#blockPalette()}
     */
    public static BlockPalette get(String id) {
        BlockPalette palette = PALETTES.get(id);
        if (palette == null) {
            palette = PALETTES.get("stone");
        }
        return palette;
    }

    /**
     * Returns {@code true} if a palette with the given key has been registered.
     */
    public static boolean contains(String id) {
        return PALETTES.containsKey(id);
    }

    // ── Registry construction ─────────────────────────────────────────────────

    private static Map<String, BlockPalette> buildPalettes() {
        Map<String, BlockPalette> map = new HashMap<>();

        // ── stone: underground / subterranean ──────────────────────────────────
        register(map, "stone",
            s(Blocks.COBBLESTONE),
            s(Blocks.STONE),
            s(Blocks.STONE),
            null);

        // ── desert: arid / sandy ──────────────────────────────────────────────
        register(map, "desert",
            s(Blocks.SAND),
            s(Blocks.SANDSTONE),
            s(Blocks.SANDSTONE),
            null);

        // ── clay: earthy / savanna ────────────────────────────────────────────
        register(map, "clay",
            s(Blocks.CLAY),
            s(Blocks.TERRACOTTA),
            s(Blocks.TERRACOTTA),
            null);

        // ── marine: ocean caves (ceiling dimension) ───────────────────────────
        register(map, "marine",
            s(Blocks.PRISMARINE_BRICKS),
            s(Blocks.PRISMARINE),
            s(Blocks.PRISMARINE),
            s(Blocks.DARK_PRISMARINE));        // ceiling

        // ── end: void / purpur ────────────────────────────────────────────────
        register(map, "end",
            s(Blocks.PURPUR_BLOCK),
            s(Blocks.END_STONE_BRICKS),
            s(Blocks.END_STONE),
            null);

        // ── infernal: nether / fire (ceiling dimension) ───────────────────────
        register(map, "infernal",
            s(Blocks.NETHERRACK),
            s(Blocks.BASALT),
            s(Blocks.NETHERRACK),
            s(Blocks.NETHERRACK));             // ceiling (same as floor — classic nether feel)

        // ── jungle: lush / vegetal ────────────────────────────────────────────
        register(map, "jungle",
            s(Blocks.GRASS_BLOCK),
            s(Blocks.DIRT),
            s(Blocks.DIRT),
            null);

        // ── crystal: precious / quartz ────────────────────────────────────────
        register(map, "crystal",
            s(Blocks.GOLD_BLOCK),
            s(Blocks.CALCITE),
            s(Blocks.CALCITE),
            null);

        // ── dark: shadow / obsidian ───────────────────────────────────────────
        register(map, "dark",
            s(Blocks.BASALT),
            s(Blocks.BLACKSTONE),
            s(Blocks.BLACKSTONE),
            null);

        // ── light: luminous / quartz ──────────────────────────────────────────
        register(map, "light",
            s(Blocks.WHITE_CONCRETE),
            s(Blocks.SMOOTH_QUARTZ),
            s(Blocks.SMOOTH_QUARTZ),
            null);

        // ── ancient: deepslate / bone ─────────────────────────────────────────
        register(map, "ancient",
            s(Blocks.COBBLED_DEEPSLATE),
            s(Blocks.DEEPSLATE),
            s(Blocks.DEEPSLATE),
            null);

        // ── ice: frozen / glacial ─────────────────────────────────────────────
        register(map, "ice",
            s(Blocks.SNOW_BLOCK),
            s(Blocks.ICE),
            s(Blocks.PACKED_ICE),
            null);

        // ── color: April Fool / rainbow ───────────────────────────────────────
        register(map, "color",
            s(Blocks.PINK_CONCRETE),
            s(Blocks.LIME_CONCRETE),
            s(Blocks.WHITE_CONCRETE),
            null);

        return Map.copyOf(map);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void register(Map<String, BlockPalette> map,
                                  String id,
                                  net.minecraft.world.level.block.state.BlockState surface,
                                  net.minecraft.world.level.block.state.BlockState under,
                                  net.minecraft.world.level.block.state.BlockState deep,
                                  net.minecraft.world.level.block.state.BlockState ceiling) {
        map.put(id, new BlockPalette(id, surface, under, deep, ceiling));
    }

    /** Shorthand: default block state. */
    private static net.minecraft.world.level.block.state.BlockState s(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState();
    }
}
