package dev.botsu.portalcraft.portal.taxonomy;

import com.mojang.serialization.Codec;

/**
 * Material families used to group portal frame blocks into thematic categories.
 *
 * <p>Each family drives portal signature routing:
 * <ul>
 *   <li>Mineral/natural families → coherent procedural destinations.</li>
 *   <li>{@link #COLOR} — April Fool wool/glass portals → surreal fantasy dimensions.</li>
 *   <li>{@link #UNKNOWN} — unclassified block (logged but not fatal).</li>
 * </ul>
 *
 * <p>This enum is free of Minecraft API imports and can be used in pure unit tests.
 */
public enum MaterialFamily {

    /** Stone, cobblestone, deepslate, bricks, blackstone. */
    STONE,

    /** Sandstone, red sandstone, terracotta, clay. */
    SAND,

    /** Clay-derived blocks (currently grouped with SAND at MVP scope). */
    CLAY,

    /** Ice, packed ice, blue ice, snow blocks. */
    ICE,

    /** Prismarine variants, sea lantern, coral blocks. */
    MARINE,

    /** Netherrack, basalt, crimson/warped stems, soul sand. */
    INFERNAL,

    /** End stone, purpur, chorus. */
    END,

    /** Moss, mud bricks, packed mud, leaves, wood (if added later). */
    VEGETAL,

    /** Quartz, amethyst, crystal-like blocks. */
    PRECIOUS,

    /** Obsidian, blackstone polished, dark-themed blocks. */
    DARK,

    /** Glowstone, shroomlight, luminous blocks. */
    LIGHT,

    /** Bone block, ancient debris-adjacent materials. */
    ANCIENT,

    /** Wool and full-block glass — the April Fool portal family. */
    COLOR,

    /** Block not mapped to any family. */
    UNKNOWN;

    /** Codec that serialises as the enum constant name (e.g. {@code "STONE"}). */
    public static final Codec<MaterialFamily> CODEC = Codec.STRING.comapFlatMap(
        s -> {
            try {
                return com.mojang.serialization.DataResult.success(MaterialFamily.valueOf(s));
            } catch (IllegalArgumentException e) {
                return com.mojang.serialization.DataResult.error(() -> "Unknown MaterialFamily: " + s);
            }
        },
        MaterialFamily::name
    );
}
