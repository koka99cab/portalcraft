package dev.botsu.portalcraft.portal.activation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Orchestrates the full portal activation pipeline.
 *
 * <p>When a player uses flint-and-steel near a valid frame, the activation service:
 * <ol>
 *   <li>Invokes {@link dev.botsu.portalcraft.portal.frame.PortalFrameDetector} to locate the frame.</li>
 *   <li>Invokes {@link dev.botsu.portalcraft.portal.frame.PortalFrameValidator} to confirm it is valid.</li>
 *   <li>Extracts raw materials via {@link dev.botsu.portalcraft.portal.analysis.PortalStructureExtractor} (Stage 1).</li>
 *   <li>Normalises to a {@link dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature} via
 *       {@link dev.botsu.portalcraft.portal.canonical.PortalCanonicalizer} (Stage 2).</li>
 *   <li>Derives a deterministic {@link dev.botsu.portalcraft.portal.identity.PortalIdentity} via
 *       {@link dev.botsu.portalcraft.portal.identity.PortalIdentityDeriver} (Stage 3).</li>
 *   <li>Fills the interior with the Portalcraft portal block.</li>
 *   <li>Registers the portal with the {@link dev.botsu.portalcraft.portal.integration.PortalRuntime}.</li>
 * </ol>
 */
public interface PortalActivationService {

    /**
     * Attempts to activate a portal starting from {@code clickedPos}.
     *
     * @param level      the level
     * @param clickedPos the position the player interacted with
     * @param player     the player triggering activation
     * @return {@code true} if activation succeeded and the portal is now lit
     */
    boolean tryActivate(Level level, BlockPos clickedPos, Player player);
}
