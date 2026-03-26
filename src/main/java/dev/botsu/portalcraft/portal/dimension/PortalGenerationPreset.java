package dev.botsu.portalcraft.portal.dimension;

import dev.botsu.portalcraft.portal.block.PortalColor;
import dev.botsu.portalcraft.portal.resolver.PortalFamily;

/**
 * Immutable descriptor of the procedural generation parameters for a portal family.
 *
 * <p>Each {@link PortalFamily} is associated with one preset via {@link FamilyPresetRegistry}.
 * In Step 7 this record feeds a custom {@link net.minecraft.world.level.chunk.ChunkGenerator}
 * and {@link net.minecraft.world.level.dimension.DimensionType}. For now it is used for
 * logging and planning only.
 *
 * <h2>Parameters (§14.2 of spec)</h2>
 * <ul>
 *   <li>{@code skyColor}         — packed 0xRRGGBB sky tint.</li>
 *   <li>{@code fogColor}         — packed 0xRRGGBB fog/atmosphere tint.</li>
 *   <li>{@code temperature}      — biome temperature (0.0 cold → 2.0 hot).</li>
 *   <li>{@code humidity}         — biome humidity (0.0 dry → 1.0 wet).</li>
 *   <li>{@code terrainScale}     — vertical amplitude multiplier (0.5 flat → 2.0 mountainous).</li>
 *   <li>{@code hasCeiling}       — bedrock ceiling like the Nether.</li>
 *   <li>{@code hasSkyLight}      — sky access like the Overworld.</li>
 *   <li>{@code blockPalette}     — string key for the block palette registry (Step 7).</li>
 *   <li>{@code portalColor}      — matching visual tint used by the portal block.</li>
 * </ul>
 */
public record PortalGenerationPreset(
        PortalFamily family,
        int     skyColor,
        int     fogColor,
        float   temperature,
        float   humidity,
        float   terrainScale,
        boolean hasCeiling,
        boolean hasSkyLight,
        String  blockPalette,
        PortalColor portalColor
) {
    @Override
    public String toString() {
        return "PortalGenerationPreset[family=%s, palette=%s, temp=%.1f, humid=%.1f, scale=%.1f, ceiling=%s, sky=%s]"
            .formatted(family, blockPalette, temperature, humidity, terrainScale, hasCeiling, hasSkyLight);
    }
}
