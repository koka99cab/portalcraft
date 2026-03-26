package dev.botsu.portalcraft.portal.frame;

import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Validates a {@link PortalFrameScanResult} against the full rule-set and produces
 * a {@link PortalFrame} when all checks pass.
 *
 * <p>Validation covers:
 * <ol>
 *   <li>Geometric correctness (delegated from the scan result — already verified by the detector).</li>
 *   <li>Tag membership: all frame blocks must belong to {@code portalcraft:frame_blocks}
 *       <em>or</em> all must belong to {@code portalcraft:april_fool_frame_blocks}.</li>
 *   <li>April Fool classification: if all blocks are in the April Fool tag,
 *       {@link PortalFrame#aprilFool()} is set to {@code true}.</li>
 * </ol>
 *
 * <p>Non-full-cube shapes (panes, fences, doors …) are excluded implicitly because they are
 * not present in either whitelisted tag.
 */
public interface PortalFrameValidator {

    /**
     * Validates {@code scanResult} and returns a populated {@link PortalFrame} if valid.
     *
     * @param level      the level used to read block states and tag membership
     * @param scanResult the raw geometric scan result from {@link PortalFrameDetector}
     * @return an {@link Optional} containing the validated frame, or empty if validation fails
     */
    Optional<PortalFrame> validate(Level level, PortalFrameScanResult scanResult);
}
