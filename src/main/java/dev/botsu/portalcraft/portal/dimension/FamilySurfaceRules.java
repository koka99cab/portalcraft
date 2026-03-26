package dev.botsu.portalcraft.portal.dimension;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link SurfaceRules.RuleSource}s for procedural Portalcraft dimensions.
 *
 * <h2>P3 — Three-layer differentiation</h2>
 * <ul>
 *   <li><b>Surface</b>  — {@link SurfaceRules#ON_FLOOR}: topmost solid block.</li>
 *   <li><b>Subsurface</b> — {@link SurfaceRules#UNDER_FLOOR}: ≈ 1–4 blocks below; transition layer.</li>
 *   <li><b>Deep</b>     — {@link SurfaceRules#DEEP_UNDER_FLOOR}: ≈ 4–8 blocks below.</li>
 *   <li><b>Ceiling</b>  — {@link SurfaceRules#ON_CEILING}: only when {@link BlockPalette#hasCeiling()}.</li>
 * </ul>
 *
 * <h2>C — Data-driven palettes</h2>
 * <p>Block selection is fully delegated to {@link BlockPaletteRegistry}: this class contains
 * <em>no</em> family-specific {@code switch} statements. The palette is resolved from
 * {@link PortalGenerationPreset#blockPalette()}, so changing a family's blocks requires
 * only updating the registry (or swapping the palette key in {@link FamilyPresetRegistry}).
 *
 * <h2>Sequence order</h2>
 * <p>{@link SurfaceRules#sequence} stops at the first matching rule. Because
 * {@code UNDER_FLOOR ⊇ ON_FLOOR} and {@code DEEP_UNDER_FLOOR ⊇ UNDER_FLOOR}, the most
 * specific condition must always come first.
 */
public final class FamilySurfaceRules {

    private FamilySurfaceRules() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Builds the {@link RuleSource} for the given preset by looking up its
     * {@link BlockPalette} in {@link BlockPaletteRegistry}.
     */
    public static RuleSource buildForPreset(PortalGenerationPreset preset) {
        return buildForPalette(BlockPaletteRegistry.get(preset.blockPalette()));
    }

    /**
     * Returns the default underground filler block (deep layer) for the given preset.
     * This becomes the {@code defaultBlock} in
     * {@link net.minecraft.world.level.levelgen.NoiseGeneratorSettings}, replacing stone
     * everywhere below the surface-rule zone.
     */
    public static BlockState defaultBlockForPreset(PortalGenerationPreset preset) {
        return BlockPaletteRegistry.get(preset.blockPalette()).deep();
    }

    // ── Core builder ─────────────────────────────────────────────────────────

    /**
     * Converts a {@link BlockPalette} into a {@link RuleSource} sequence.
     *
     * <p>Rules are added in specificity order (most specific first), so that
     * {@code SurfaceRules.sequence} can stop at the first match:
     * <ol>
     *   <li>ON_FLOOR → surface block</li>
     *   <li>UNDER_FLOOR → under block</li>
     *   <li>DEEP_UNDER_FLOOR → deep block</li>
     *   <li>ON_CEILING → ceiling block (if {@link BlockPalette#hasCeiling()})</li>
     * </ol>
     */
    public static RuleSource buildForPalette(BlockPalette palette) {
        List<RuleSource> rules = new ArrayList<>(4);
        rules.add(SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
            SurfaceRules.state(palette.surface())));
        rules.add(SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
            SurfaceRules.state(palette.under())));
        rules.add(SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR,
            SurfaceRules.state(palette.deep())));
        if (palette.hasCeiling()) {
            rules.add(SurfaceRules.ifTrue(SurfaceRules.ON_CEILING,
                SurfaceRules.state(palette.ceiling())));
        }
        return SurfaceRules.sequence(rules.toArray(RuleSource[]::new));
    }
}
