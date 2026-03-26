package dev.botsu.portalcraft.portal;

import dev.botsu.portalcraft.portal.frame.DefaultPortalFrameDetector;
import dev.botsu.portalcraft.portal.frame.PortalFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for portal frame shape logic.
 *
 * <p>These tests validate the geometric constraints and block-count arithmetic
 * of portal frames. They use {@link BlockPos} and {@link Direction.Axis} as
 * plain value objects — no Minecraft registry is accessed.
 *
 * <p>A synthetic frame builder ({@link #buildFrame}) constructs the expected block lists
 * for a given inner dimension using the same layout logic as
 * {@link dev.botsu.portalcraft.portal.frame.DefaultPortalFrameDetector}.
 */
class PortalFrameShapeTest {

    // ── Dimension validation ─────────────────────────────────────────────────

    @Test
    void minValidDimensions() {
        PortalFrame frame = buildFrame(2, 3, Direction.Axis.X);
        assertEquals(2, frame.innerWidth());
        assertEquals(3, frame.innerHeight());
    }

    @Test
    void maxValidDimensions() {
        PortalFrame frame = buildFrame(21, 21, Direction.Axis.X);
        assertEquals(21, frame.innerWidth());
        assertEquals(21, frame.innerHeight());
    }

    @Test
    void tooNarrowIsRejected() {
        assertFalse(isValidWidth(1), "width 1 should be invalid");
    }

    @Test
    void tooShortIsRejected() {
        assertFalse(isValidHeight(2), "height 2 should be invalid");
    }

    @Test
    void tooWideIsRejected() {
        assertFalse(isValidWidth(22), "width 22 should be invalid");
    }

    @Test
    void tooTallIsRejected() {
        assertFalse(isValidHeight(22), "height 22 should be invalid");
    }

    // ── Frame block count ─────────────────────────────────────────────────────

    @Test
    void frameBlockCount_4x5() {
        // Contour = 2 horizontal rows of (w+2) + 2 vertical columns of h
        // = 2*(4+2) + 2*5 = 12 + 10 = 22
        PortalFrame frame = buildFrame(4, 5, Direction.Axis.X);
        assertEquals(22, frame.frameBlockCount(),
            "4-wide × 5-tall frame should have 22 contour positions");
    }

    @Test
    void frameBlockCount_2x3() {
        // 2*(2+2) + 2*3 = 8 + 6 = 14
        PortalFrame frame = buildFrame(2, 3, Direction.Axis.X);
        assertEquals(14, frame.frameBlockCount(),
            "2-wide × 3-tall frame should have 14 contour positions");
    }

    @Test
    void innerBlockCount_4x5() {
        PortalFrame frame = buildFrame(4, 5, Direction.Axis.X);
        assertEquals(20, frame.innerBlockCount(), "4×5 interior should contain 20 blocks");
    }

    @Test
    void innerBlockCount_2x3() {
        PortalFrame frame = buildFrame(2, 3, Direction.Axis.X);
        assertEquals(6, frame.innerBlockCount(), "2×3 interior should contain 6 blocks");
    }

    // ── Axis symmetry ─────────────────────────────────────────────────────────

    @Test
    void sameCountsRegardlessOfAxis() {
        PortalFrame xFrame = buildFrame(5, 7, Direction.Axis.X);
        PortalFrame zFrame = buildFrame(5, 7, Direction.Axis.Z);
        assertEquals(xFrame.frameBlockCount(), zFrame.frameBlockCount(),
            "Frame block count should be the same for X and Z axis portals of equal size");
        assertEquals(xFrame.innerBlockCount(), zFrame.innerBlockCount(),
            "Inner block count should be the same for X and Z axis portals of equal size");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a synthetic {@link PortalFrame} with the given inner dimensions without
     * a real Minecraft world. Uses the same contour layout as
     * {@link DefaultPortalFrameDetector}.
     */
    static PortalFrame buildFrame(int innerWidth, int innerHeight, Direction.Axis axis) {
        int originX = 0, originY = 64, originZ = 0;
        int minH = 0;
        int maxH = innerWidth - 1;
        int bottomY = originY;
        int topY = originY + innerHeight - 1;
        int frameMinH = minH - 1;
        int frameMaxH = maxH + 1;
        int frameBottomY = bottomY - 1;
        int frameTopY = topY + 1;
        int fixed = 0; // fixed coordinate on the other axis

        List<BlockPos> frameBlocks = new ArrayList<>();
        List<BlockPos> innerBlocks = new ArrayList<>();

        if (axis == Direction.Axis.X) {
            for (int x = frameMinH; x <= frameMaxH; x++) frameBlocks.add(new BlockPos(x, frameBottomY, fixed));
            for (int x = frameMinH; x <= frameMaxH; x++) frameBlocks.add(new BlockPos(x, frameTopY,    fixed));
            for (int y = bottomY;   y <= topY;       y++) frameBlocks.add(new BlockPos(frameMinH, y, fixed));
            for (int y = bottomY;   y <= topY;       y++) frameBlocks.add(new BlockPos(frameMaxH, y, fixed));
            for (int y = bottomY; y <= topY; y++)
                for (int x = minH; x <= maxH; x++)
                    innerBlocks.add(new BlockPos(x, y, fixed));
        } else {
            for (int z = frameMinH; z <= frameMaxH; z++) frameBlocks.add(new BlockPos(fixed, frameBottomY, z));
            for (int z = frameMinH; z <= frameMaxH; z++) frameBlocks.add(new BlockPos(fixed, frameTopY,    z));
            for (int y = bottomY;   y <= topY;       y++) frameBlocks.add(new BlockPos(fixed, y, frameMinH));
            for (int y = bottomY;   y <= topY;       y++) frameBlocks.add(new BlockPos(fixed, y, frameMaxH));
            for (int y = bottomY; y <= topY; y++)
                for (int z = minH; z <= maxH; z++)
                    innerBlocks.add(new BlockPos(fixed, y, z));
        }

        return new PortalFrame(List.copyOf(frameBlocks), List.copyOf(innerBlocks),
            axis, innerWidth, innerHeight, false);
    }

    /** Overload that lets callers set the {@code aprilFool} flag explicitly. */
    static PortalFrame buildFrame(int innerWidth, int innerHeight, Direction.Axis axis, boolean aprilFool) {
        PortalFrame base = buildFrame(innerWidth, innerHeight, axis);
        return new PortalFrame(base.frameBlocks(), base.innerBlocks(), axis, innerWidth, innerHeight, aprilFool);
    }

    private boolean isValidWidth(int w) {
        return w >= DefaultPortalFrameDetector.MIN_WIDTH && w <= DefaultPortalFrameDetector.MAX_WIDTH;
    }

    private boolean isValidHeight(int h) {
        return h >= DefaultPortalFrameDetector.MIN_HEIGHT && h <= DefaultPortalFrameDetector.MAX_HEIGHT;
    }
}
