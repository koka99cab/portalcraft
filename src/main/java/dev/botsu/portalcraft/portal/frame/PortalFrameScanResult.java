package dev.botsu.portalcraft.portal.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

/**
 * Immutable geometric result produced by {@link PortalFrameDetector}.
 *
 * <p>A scan result is either valid or invalid. When valid it carries:
 * <ul>
 *   <li>{@code frameBlocks} — the positions forming the outer contour.</li>
 *   <li>{@code innerBlocks} — the positions of the hollow interior.</li>
 *   <li>{@code axis} — the horizontal axis the portal extends along ({@code X} or {@code Z}).</li>
 *   <li>{@code innerWidth} — interior width in blocks (2–21).</li>
 *   <li>{@code innerHeight} — interior height in blocks (3–21).</li>
 * </ul>
 *
 * <p>When {@code valid} is {@code false} all other fields are undefined.
 * Tag validation is <em>not</em> performed here — that is the responsibility of
 * {@link PortalFrameValidator}.
 */
public record PortalFrameScanResult(
        boolean valid,
        List<BlockPos> frameBlocks,
        List<BlockPos> innerBlocks,
        Direction.Axis axis,
        int innerWidth,
        int innerHeight
) {

    /** Convenience factory for a failed scan. */
    public static PortalFrameScanResult invalid() {
        return new PortalFrameScanResult(false, List.of(), List.of(), Direction.Axis.X, 0, 0);
    }
}
