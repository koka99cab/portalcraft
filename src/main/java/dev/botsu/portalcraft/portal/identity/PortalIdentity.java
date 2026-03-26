package dev.botsu.portalcraft.portal.identity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Stage 3 of the portal identity pipeline — stable identity derivation.
 *
 * <p>An immutable, deterministic identifier for a portal recipe. Two portals built
 * from the same blocks and interior dimensions (regardless of axis orientation) will
 * always produce an identical {@code PortalIdentity}.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code canonicalSource} — the human-readable canonical string from Stage 2.
 *       Preserved for display, logging, and future migration.</li>
 *   <li>{@code stableId} — a deterministic 64-bit hash derived from
 *       {@code canonicalSource}. This is the lookup key in
 *       {@link dev.botsu.portalcraft.portal.registry.DimensionRegistry}.</li>
 *   <li>{@code version} — schema version for future migration support.
 *       Currently always {@link #CURRENT_VERSION}.</li>
 * </ul>
 *
 * <h2>Must NOT contain</h2>
 * <ul>
 *   <li>Any world-scanning, dimension-creation, or teleportation logic.</li>
 *   <li>Direct references to Minecraft levels or server state.</li>
 * </ul>
 */
public record PortalIdentity(
        String canonicalSource,
        long stableId,
        int version
) {

    /** Current schema version. Increment when the hashing algorithm changes. */
    public static final int CURRENT_VERSION = 1;

    /** Codec for NBT persistence. */
    public static final Codec<PortalIdentity> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("canonicalSource").forGetter(PortalIdentity::canonicalSource),
        Codec.LONG.fieldOf("stableId").forGetter(PortalIdentity::stableId),
        Codec.INT.fieldOf("version").forGetter(PortalIdentity::version)
    ).apply(i, PortalIdentity::new));

    /**
     * Returns the dimension {@link ResourceKey} that this identity maps to.
     * Format: {@code portalcraft:dim_<stableId as hex>}.
     */
    public ResourceKey<Level> toDimensionKey() {
        return ResourceKey.create(
                Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("portalcraft", "dim_" + Long.toHexString(stableId))
        );
    }
}
