package dev.botsu.portalcraft.portal.integration;

import dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Placeholder {@link PortalRuntime} for an optional external portal library.
 *
 * <p><b>Status:</b> no external portal library with confirmed 1.21.11 Fabric
 * compatibility is known at time of writing. This class exists to keep the
 * integration surface isolated so no other Portalcraft code needs to change
 * when a library is eventually chosen.
 */
public class ExternalPortalRuntime implements PortalRuntime {

    private static final String NOT_IMPLEMENTED =
        "ExternalPortalRuntime is a stub. Wire a real external portal library and implement this method.";

    @Override
    public void onPortalActivated(ServerLevel level, BlockPos framePos,
                                  PortalIdentity identity, CanonicalPortalSignature canonical) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public void onPortalDeactivated(ServerLevel level, BlockPos framePos) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public void teleport(ServerLevel level, Entity entity, BlockPos portalPos,
                         PortalIdentity identity) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
