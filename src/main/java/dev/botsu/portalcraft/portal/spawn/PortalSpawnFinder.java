package dev.botsu.portalcraft.portal.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;

/**
 * Finds a safe spawn position for a player arriving through a Portalcraft portal.
 *
 * <p>A "safe" position satisfies:
 * <ul>
 *   <li>The block below (floor) is solid.</li>
 *   <li>The block at feet level and the block at head level are both air.</li>
 * </ul>
 *
 * <p>The search strategy is dimension-aware:
 * <ul>
 *   <li><b>Overworld / End</b>: start from the MOTION_BLOCKING_NO_LEAVES heightmap
 *       (surface) and scan downward.</li>
 *   <li><b>Nether</b>: start from Y=96 (below the bedrock roof) and scan downward,
 *       avoiding both the lava floor and the bedrock ceiling.</li>
 * </ul>
 */
public final class PortalSpawnFinder {

    private PortalSpawnFinder() {}

    /** Minimum player body height required above the floor. */
    private static final int PLAYER_HEIGHT = 2;

    /**
     * Returns a BlockPos where it is safe to stand (feet position).
     * Falls back to a clamped Y position if no ideal spot is found.
     */
    public static BlockPos findSafeSpawn(ServerLevel level, double targetX, double targetZ) {
        int x = (int) Math.floor(targetX);
        int z = (int) Math.floor(targetZ);
        int minY = level.getMinY() + 1;
        int maxY = level.getMaxY() - PLAYER_HEIGHT - 1;

        int startY;
        if (level.dimension().equals(Level.NETHER)) {
            // Stay well below the bedrock ceiling and above the lava sea
            startY = Math.min(96, maxY);
        } else {
            // Overworld and End: start one block above the surface solid block
            BlockPos surfacePos = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z));
            startY = Math.min(Math.max(surfacePos.getY() + 1, minY), maxY);
        }

        // Scan downward first (prefer lower, more accessible positions)
        for (int y = startY; y >= minY; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (isSafe(level, candidate)) {
                return candidate;
            }
        }
        // Scan upward as fallback
        for (int y = minY; y <= maxY; y++) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (isSafe(level, candidate)) {
                return candidate;
            }
        }
        // Last-resort fallback: clamped startY (frame will be placed here anyway)
        return new BlockPos(x, Math.max(minY, startY), z);
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid()
            && level.getBlockState(pos).isAir()
            && level.getBlockState(pos.above()).isAir();
    }
}
