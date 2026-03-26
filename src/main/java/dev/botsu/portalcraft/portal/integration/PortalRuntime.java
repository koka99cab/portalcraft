package dev.botsu.portalcraft.portal.integration;

import dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Integration seam between Portalcraft's portal logic and the underlying transport layer.
 *
 * <p>All portal behaviour that touches dimension registration, teleportation, or
 * world-side effects goes through this interface. This makes it possible to:
 * <ul>
 *   <li>Use a vanilla-style implementation by default ({@link VanillaStylePortalRuntime}).</li>
 *   <li>Plug in an external portal library later ({@link ExternalPortalRuntime})
 *       without rewriting any other Portalcraft code.</li>
 * </ul>
 */
public interface PortalRuntime {

    /**
     * Called when a portal frame is first lit.
     *
     * @param level     the level the portal was lit in
     * @param framePos  the position of any frame block (anchor point)
     * @param identity  the stable Stage-3 identity of the portal
     * @param canonical the canonical Stage-2 signature (for display / preset selection)
     */
    void onPortalActivated(ServerLevel level, BlockPos framePos,
                           PortalIdentity identity, CanonicalPortalSignature canonical);

    /**
     * Called when a portal frame collapses or is intentionally deactivated.
     *
     * @param level    the level the portal was in
     * @param framePos the position of any former frame block
     */
    void onPortalDeactivated(ServerLevel level, BlockPos framePos);

    /**
     * Teleports {@code entity} through the portal anchored at {@code portalPos}.
     *
     * @param level     the source level
     * @param entity    the entity entering the portal
     * @param portalPos the position of the portal block the entity touched
     * @param identity  the stable identity of the portal being traversed
     */
    void teleport(ServerLevel level, Entity entity, BlockPos portalPos, PortalIdentity identity);
}
