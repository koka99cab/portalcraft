package dev.botsu.portalcraft.portal.resolver;

import com.mojang.serialization.Codec;

/**
 * High-level portal families used to route portals to destination dimensions and
 * to drive procedural generation presets (Step 7).
 *
 * <p>Derived from the dominant {@link dev.botsu.portalcraft.portal.taxonomy.MaterialFamily}
 * of the portal frame via {@link PortalResolver#resolveFamily}. Each family maps to a
 * different {@link dev.botsu.portalcraft.portal.dimension.PortalGenerationPreset} and a
 * distinct destination dimension.
 *
 * <h2>MVP routing</h2>
 * <p>Until Step 7 implements custom dimensions, most families route to the Nether;
 * {@link #END} routes to The End, and portals opened from non-Overworld dimensions
 * always return to the Overworld. See {@link dev.botsu.portalcraft.portal.dimension.VanillaDimensionService}.
 */
public enum PortalFamily {
    /** Stone, deepslate, blackstone — classic subterranean portal. */
    STONE,
    /** Sandstone, red sandstone — warm, arid portal. */
    SAND,
    /** Terracotta, hardened clay — earthy / clay portal. */
    CLAY,
    /** Prismarine, dark prismarine — oceanic portal. */
    MARINE,
    /** End stone, purpur — void-like portal. Routes to The End. */
    END,
    /** Netherrack, basalt, soul sand/soil, crimson/warped stems — infernal portal. */
    INFERNAL,
    /** Mud bricks, packed mud, moss — organic / jungle portal. */
    VEGETAL,
    /** Quartz, gold, precious blocks — crystalline portal. */
    PRECIOUS,
    /** Dark blocks — shadowy, void portal. */
    DARK,
    /** Glowstone, shroomlight, beacon — luminous portal. */
    LIGHT,
    /** Bone block, ancient debris, netherite — ancient portal. */
    ANCIENT,
    /** Packed ice, blue ice — frozen portal. */
    ICE,
    /** All-wool / all-glass frame — April Fool color portal. */
    COLOR,
    /** Catch-all for unclassified or mixed frames. */
    UNKNOWN;

    /** Codec that serialises as the enum constant name (e.g. {@code "STONE"}). */
    public static final Codec<PortalFamily> CODEC = Codec.STRING.comapFlatMap(
        s -> {
            try {
                return com.mojang.serialization.DataResult.success(PortalFamily.valueOf(s));
            } catch (IllegalArgumentException e) {
                return com.mojang.serialization.DataResult.error(() -> "Unknown PortalFamily: " + s);
            }
        },
        PortalFamily::name
    );
}
