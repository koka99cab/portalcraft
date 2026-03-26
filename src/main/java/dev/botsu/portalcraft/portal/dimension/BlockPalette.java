package dev.botsu.portalcraft.portal.dimension;

import net.minecraft.world.level.block.state.BlockState;

/**
 * An immutable set of block states that defines the surface, sub-surface, and underground
 * layers of a procedural Portalcraft dimension.
 *
 * <h2>Layer semantics</h2>
 * <ul>
 *   <li>{@code surface}  — placed on {@code SurfaceRules.ON_FLOOR} (topmost solid block).</li>
 *   <li>{@code under}    — placed on {@code SurfaceRules.UNDER_FLOOR} (≈ 1–4 blocks below).</li>
 *   <li>{@code deep}     — placed on {@code SurfaceRules.DEEP_UNDER_FLOOR} (≈ 4–8 blocks below)
 *       and doubles as the {@code defaultBlock} in {@link net.minecraft.world.level.levelgen.NoiseGeneratorSettings}
 *       (i.e., the bulk underground fill material everywhere below those layers).</li>
 *   <li>{@code ceiling}  — placed on {@code SurfaceRules.ON_CEILING}; {@code null} for open-sky
 *       dimensions. Non-null only for ceiling dimensions (MARINE, INFERNAL).</li>
 * </ul>
 *
 * <p>Instances are created by {@link BlockPaletteRegistry} and looked up by the
 * string key stored in {@link PortalGenerationPreset#blockPalette()}.
 *
 * <h2>Design note</h2>
 * <p>This record is intentionally block-state-level (not block-level) so that future
 * palettes can specify non-default states (e.g. {@code minecraft:basalt[axis=y]})
 * without requiring changes to the API surface.
 */
public record BlockPalette(
        String id,
        BlockState surface,
        BlockState under,
        BlockState deep,
        BlockState ceiling  // null → no ceiling surface rule
) {

    /**
     * Returns {@code true} if this palette defines a ceiling surface block.
     * Ceiling dimensions (MARINE, INFERNAL) should have a non-null ceiling.
     */
    public boolean hasCeiling() {
        return ceiling != null;
    }
}
