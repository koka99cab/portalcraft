package dev.botsu.portalcraft.portal.integration;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Default {@link PortalRuntime} that implements portal behaviour using only
 * vanilla Minecraft / Fabric API mechanisms.
 *
 * <h2>Teleportation</h2>
 * <p>Actual teleportation is handled by the {@link dev.botsu.portalcraft.portal.block.PortalBlock}
 * via the {@link net.minecraft.world.level.block.Portal} interface — the vanilla entity-inside-portal
 * mechanism triggers the transition automatically. The {@link #teleport} method in this class
 * is therefore a no-op that exists for external callers (e.g. command teleportation in the future).
 *
 * <h2>Activation / deactivation</h2>
 * <p>Logs lifecycle events at {@code INFO} level.
 */
public class VanillaStylePortalRuntime implements PortalRuntime {

    @Override
    public void onPortalActivated(ServerLevel level, BlockPos framePos,
                                  PortalIdentity identity, CanonicalPortalSignature canonical) {
        PortalcraftConstants.LOGGER.info(
            "[Portalcraft] Portal activated — dim={} pos={} family={} april_fool={} id={}",
            level.dimension().identifier(),
            framePos,
            canonical.dominantFamily(),
            canonical.aprilFool(),
            Long.toHexString(identity.stableId())
        );
    }

    @Override
    public void onPortalDeactivated(ServerLevel level, BlockPos framePos) {
        PortalcraftConstants.LOGGER.info(
            "[Portalcraft] Portal deactivated — dim={} pos={}",
            level.dimension().identifier(), framePos);
    }

    @Override
    public void teleport(ServerLevel level, Entity entity, BlockPos portalPos,
                         PortalIdentity identity) {
        // Teleportation is driven by PortalBlock.getPortalDestination() via the Portal interface.
        PortalcraftConstants.LOGGER.debug(
            "[Portalcraft] teleport() called for {} at {} — handled by PortalBlock.",
            entity.getName().getString(), portalPos);
    }
}
