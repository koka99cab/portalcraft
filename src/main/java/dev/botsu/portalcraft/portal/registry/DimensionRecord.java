package dev.botsu.portalcraft.portal.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import dev.botsu.portalcraft.portal.resolver.PortalFamily;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * An immutable record of a procedurally created Portalcraft dimension.
 *
 * <p>Stored in {@link DimensionRegistry} and persisted across restarts.
 * This is the link between a {@link PortalIdentity} and the actual
 * {@link ResourceKey} of the created {@link net.minecraft.server.level.ServerLevel}.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code stableId} — the 64-bit identity from Stage 3, used as the map key.</li>
 *   <li>{@code canonicalSource} — human-readable canonical string for display/debugging.</li>
 *   <li>{@code dimensionKeyPath} — the path component of the dimension resource key,
 *       e.g. {@code "dim_1a2b3c4d5e6f7a8b"}. The namespace is always {@code "portalcraft"}.</li>
 *   <li>{@code family} — portal family; used to look up the correct
 *       {@link dev.botsu.portalcraft.portal.dimension.PortalGenerationPreset} on restart.</li>
 *   <li>{@code createdAtEpoch} — Unix timestamp (seconds) of first creation.</li>
 * </ul>
 */
public record DimensionRecord(
        long stableId,
        String canonicalSource,
        String dimensionKeyPath,
        PortalFamily family,
        long createdAtEpoch
) {

    /** Codec for NBT persistence in {@link DimensionRegistry}. */
    public static final Codec<DimensionRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.LONG.fieldOf("stableId").forGetter(DimensionRecord::stableId),
        Codec.STRING.fieldOf("canonicalSource").forGetter(DimensionRecord::canonicalSource),
        Codec.STRING.fieldOf("dimensionKeyPath").forGetter(DimensionRecord::dimensionKeyPath),
        PortalFamily.CODEC.fieldOf("family").forGetter(DimensionRecord::family),
        Codec.LONG.fieldOf("createdAtEpoch").forGetter(DimensionRecord::createdAtEpoch)
    ).apply(i, DimensionRecord::new));

    /**
     * Reconstructs the full {@link ResourceKey} for the dimension from the stored path.
     */
    public ResourceKey<Level> dimensionKey() {
        return ResourceKey.create(
                Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("portalcraft", dimensionKeyPath)
        );
    }
}
