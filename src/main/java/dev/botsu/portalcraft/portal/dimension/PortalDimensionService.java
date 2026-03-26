package dev.botsu.portalcraft.portal.dimension;

import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import dev.botsu.portalcraft.portal.resolver.PortalFamily;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Resolves the destination {@link ResourceKey<Level>} for a portal teleportation.
 *
 * <p>The interface deliberately takes a pre-computed {@link PortalIdentity} and
 * {@link PortalFamily} rather than the raw canonical signature. This enforces the
 * separation of concerns: dimension resolution only needs the stable identity and
 * the family for preset selection; it must not re-derive either.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link VanillaDimensionService} — routes to vanilla Nether / End / Overworld.
 *       Used as the fallback when procedural creation fails.</li>
 *   <li>{@link ProceduralDimensionService} — the primary implementation; consults
 *       {@link dev.botsu.portalcraft.portal.registry.DimensionRegistry} and creates
 *       a new dimension only when the identity is not yet registered.</li>
 * </ul>
 *
 * <p>The active implementation is held by
 * {@link dev.botsu.portalcraft.portal.resolver.PortalResolver} and can be replaced
 * at initialisation time.
 */
public interface PortalDimensionService {

    /**
     * Returns the dimension the entity should be sent to.
     *
     * @param identity the stable Stage-3 portal identity (lookup key)
     * @param family   the portal family derived from the canonical signature
     *                 (used to select the generation preset when creating a new dimension)
     * @param source   the level the entity is currently in
     * @return destination dimension key; never {@code null}
     */
    ResourceKey<Level> resolveDimension(PortalIdentity identity, PortalFamily family, ServerLevel source);
}
