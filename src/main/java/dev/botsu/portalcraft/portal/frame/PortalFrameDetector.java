package dev.botsu.portalcraft.portal.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Detects a portal frame structure in the world starting from a seed position.
 *
 * <p>Implementations walk outward from {@code seed} along the given {@code axis}
 * and attempt to trace a closed rectangular frame. The result captures the frame
 * block positions and interior positions.
 *
 * <h2>Design constraints (from spec)</h2>
 * <ul>
 *   <li>Frame must be a closed rectangle, vertical, aligned to X or Z.</li>
 *   <li>Interior must be empty (air or replaceable blocks).</li>
 *   <li>Interior width: 2–21 blocks, interior height: 3–21 blocks.</li>
 *   <li>Frame blocks must belong to the {@code portalcraft:frame_blocks} tag.</li>
 * </ul>
 *
 * <p>This interface is not yet implemented; see milestone list in README.
 */
public interface PortalFrameDetector {

    /**
     * Scans the world for a valid portal frame touching {@code seed}.
     *
     * @param level the server level to scan
     * @param seed  a block position known to be part of or adjacent to the frame
     * @param axis  the horizontal axis to scan along ({@link Direction.Axis#X} or {@link Direction.Axis#Z})
     * @return a {@link PortalFrameScanResult} — check {@link PortalFrameScanResult#valid()} before use
     */
    PortalFrameScanResult scan(Level level, BlockPos seed, Direction.Axis axis);
}
