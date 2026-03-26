package dev.botsu.portalcraft.portal.frame;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.PortalcraftTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Default implementation of {@link PortalFrameValidator}.
 *
 * <h2>Validation steps</h2>
 * <ol>
 *   <li>Reject immediately if the scan result is not valid.</li>
 *   <li>For each frame block, check tag membership:
 *     <ul>
 *       <li>Track whether <em>all</em> blocks are in {@code portalcraft:frame_blocks}.</li>
 *       <li>Track whether <em>all</em> blocks are in {@code portalcraft:april_fool_frame_blocks}.</li>
 *     </ul>
 *   </li>
 *   <li>A frame is valid only if at least one of these "all-in-tag" conditions holds.
 *       Mixed frames (some from one tag, some from the other) are rejected.</li>
 *   <li>The {@code aprilFool} flag is set when all blocks satisfy the April Fool tag.</li>
 * </ol>
 *
 * <p><strong>Note:</strong> tag resolution requires a live server environment.
 * This validator cannot be exercised in plain JUnit unit tests.
 */
public class DefaultPortalFrameValidator implements PortalFrameValidator {

    @Override
    public Optional<PortalFrame> validate(Level level, PortalFrameScanResult scanResult) {
        if (!scanResult.valid()) {
            return Optional.empty();
        }

        boolean allInFrameTag      = true;
        boolean allInAprilFoolTag  = true;

        for (BlockPos pos : scanResult.frameBlocks()) {
            BlockState state = level.getBlockState(pos);

            if (allInFrameTag && !state.is(PortalcraftTags.FRAME_BLOCKS)) {
                allInFrameTag = false;
            }
            if (allInAprilFoolTag && !state.is(PortalcraftTags.APRIL_FOOL_FRAME_BLOCKS)) {
                allInAprilFoolTag = false;
            }

            // Early exit: neither condition can be satisfied anymore
            if (!allInFrameTag && !allInAprilFoolTag) {
                PortalcraftConstants.LOGGER.debug(
                    "Frame validation failed at {}: block {} is in neither tag.",
                    pos, state.getBlock()
                );
                return Optional.empty();
            }
        }

        // April Fool takes priority (all-in-april-fool implies all-in-special-rule)
        boolean aprilFool = allInAprilFoolTag;

        return Optional.of(new PortalFrame(
            scanResult.frameBlocks(),
            scanResult.innerBlocks(),
            scanResult.axis(),
            scanResult.innerWidth(),
            scanResult.innerHeight(),
            aprilFool
        ));
    }
}
