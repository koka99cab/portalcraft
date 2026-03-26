package dev.botsu.portalcraft.portal.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link PortalFrameDetector}.
 *
 * <h2>Scan algorithm</h2>
 * <ol>
 *   <li>Starting from {@code seed} (expected to be an interior or adjacent-interior position),
 *       walk outward along {@code ±axis} until hitting a non-air block — these are the
 *       left/right walls of the frame.</li>
 *   <li>Walk {@code ±Y} from seed until hitting a non-air block — these are the
 *       bottom/top walls.</li>
 *   <li>Validate interior dimensions: width ∈ [2, 21], height ∈ [3, 21].</li>
 *   <li>Collect frame contour positions (bottom row, top row, left column, right column).</li>
 *   <li>Collect interior positions and verify they are all air.</li>
 * </ol>
 *
 * <p>Tag validation is <em>not</em> performed here; that is the responsibility of
 * {@link PortalFrameValidator}.
 *
 * <p>This implementation does not handle a seed that sits exactly on a frame block.
 * The activation service is expected to resolve a neighbouring interior cell first.
 */
public class DefaultPortalFrameDetector implements PortalFrameDetector {

    /** Minimum interior width (spec §8.3). */
    public static final int MIN_WIDTH = 2;
    /** Maximum interior width (spec §8.3). */
    public static final int MAX_WIDTH = 21;
    /** Minimum interior height (spec §8.3). */
    public static final int MIN_HEIGHT = 3;
    /** Maximum interior height (spec §8.3). */
    public static final int MAX_HEIGHT = 21;

    @Override
    public PortalFrameScanResult scan(Level level, BlockPos seed, Direction.Axis axis) {
        // Positive and negative horizontal directions for this axis
        Direction positive = axis == Direction.Axis.X ? Direction.EAST  : Direction.SOUTH;
        Direction negative = axis == Direction.Axis.X ? Direction.WEST  : Direction.NORTH;

        // ── 1. Find horizontal interior extent ───────────────────────────────
        int leftOffset  = scanToWall(level, seed, negative, MAX_WIDTH + 1);
        int rightOffset = scanToWall(level, seed, positive, MAX_WIDTH + 1);

        // Wall not found within limits → not a valid frame
        if (leftOffset < 0 || rightOffset < 0) {
            return PortalFrameScanResult.invalid();
        }

        // Interior starts one step inside the wall
        BlockPos leftInner  = seed.relative(negative, leftOffset - 1);
        BlockPos rightInner = seed.relative(positive, rightOffset - 1);

        int innerWidth = distance(leftInner, rightInner, axis) + 1;
        if (innerWidth < MIN_WIDTH || innerWidth > MAX_WIDTH) {
            return PortalFrameScanResult.invalid();
        }

        // ── 2. Find vertical interior extent ─────────────────────────────────
        int downOffset = scanToWall(level, seed, Direction.DOWN, MAX_HEIGHT + 1);
        int upOffset   = scanToWall(level, seed, Direction.UP,   MAX_HEIGHT + 1);

        if (downOffset < 0 || upOffset < 0) {
            return PortalFrameScanResult.invalid();
        }

        int bottomY = seed.getY() - (downOffset - 1);
        int topY    = seed.getY() + (upOffset   - 1);

        int innerHeight = topY - bottomY + 1;
        if (innerHeight < MIN_HEIGHT || innerHeight > MAX_HEIGHT) {
            return PortalFrameScanResult.invalid();
        }

        // ── 3. Determine the fixed coordinate for the other axis ─────────────
        int fixedX = seed.getX();
        int fixedZ = seed.getZ();

        int minH = axis == Direction.Axis.X ? leftInner.getX()  : leftInner.getZ();
        int maxH = axis == Direction.Axis.X ? rightInner.getX() : rightInner.getZ();

        // ── 4. Collect frame contour ──────────────────────────────────────────
        List<BlockPos> frameBlocks = new ArrayList<>();

        // Frame extends one block outside the interior
        int frameMinH = minH - 1;
        int frameMaxH = maxH + 1;
        int frameBottomY = bottomY - 1;
        int frameTopY    = topY + 1;

        if (axis == Direction.Axis.X) {
            // Bottom row (full width including corners)
            for (int x = frameMinH; x <= frameMaxH; x++) {
                frameBlocks.add(new BlockPos(x, frameBottomY, fixedZ));
            }
            // Top row (full width including corners)
            for (int x = frameMinH; x <= frameMaxH; x++) {
                frameBlocks.add(new BlockPos(x, frameTopY, fixedZ));
            }
            // Left column (excluding corners already added)
            for (int y = bottomY; y <= topY; y++) {
                frameBlocks.add(new BlockPos(frameMinH, y, fixedZ));
            }
            // Right column (excluding corners already added)
            for (int y = bottomY; y <= topY; y++) {
                frameBlocks.add(new BlockPos(frameMaxH, y, fixedZ));
            }
        } else { // Z axis
            // Bottom row
            for (int z = frameMinH; z <= frameMaxH; z++) {
                frameBlocks.add(new BlockPos(fixedX, frameBottomY, z));
            }
            // Top row
            for (int z = frameMinH; z <= frameMaxH; z++) {
                frameBlocks.add(new BlockPos(fixedX, frameTopY, z));
            }
            // Left column (excluding corners)
            for (int y = bottomY; y <= topY; y++) {
                frameBlocks.add(new BlockPos(fixedX, y, frameMinH));
            }
            // Right column (excluding corners)
            for (int y = bottomY; y <= topY; y++) {
                frameBlocks.add(new BlockPos(fixedX, y, frameMaxH));
            }
        }

        // ── 5. Collect interior and verify it is empty ────────────────────────
        List<BlockPos> innerBlocks = new ArrayList<>(innerWidth * innerHeight);

        for (int y = bottomY; y <= topY; y++) {
            for (int h = minH; h <= maxH; h++) {
                BlockPos pos = axis == Direction.Axis.X
                    ? new BlockPos(h, y, fixedZ)
                    : new BlockPos(fixedX, y, h);
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    return PortalFrameScanResult.invalid();
                }
                innerBlocks.add(pos);
            }
        }

        return new PortalFrameScanResult(true, List.copyOf(frameBlocks), List.copyOf(innerBlocks),
            axis, innerWidth, innerHeight);
    }

    /**
     * Walks from {@code origin} in {@code direction} until the block is not air,
     * returning the number of steps taken to reach the wall block.
     *
     * @param maxSteps maximum allowed steps (prevents runaway scanning)
     * @return number of steps to the first non-air block, or {@code -1} if not found within limit
     */
    private int scanToWall(Level level, BlockPos origin, Direction direction, int maxSteps) {
        for (int i = 1; i <= maxSteps; i++) {
            BlockPos candidate = origin.relative(direction, i);
            if (!level.getBlockState(candidate).isAir()) {
                return i;
            }
        }
        return -1; // no wall found within limit
    }

    /**
     * Returns the distance (inclusive) between two positions along the given axis.
     */
    private int distance(BlockPos a, BlockPos b, Direction.Axis axis) {
        return switch (axis) {
            case X -> Math.abs(b.getX() - a.getX());
            case Y -> Math.abs(b.getY() - a.getY());
            case Z -> Math.abs(b.getZ() - a.getZ());
        };
    }
}
