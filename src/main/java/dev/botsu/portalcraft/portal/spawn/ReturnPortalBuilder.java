package dev.botsu.portalcraft.portal.spawn;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.portal.block.ModBlocks;
import dev.botsu.portalcraft.portal.block.PortalBlock;
import dev.botsu.portalcraft.portal.block.PortalColor;
import dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature;
import dev.botsu.portalcraft.portal.data.PortalDataStore;
import dev.botsu.portalcraft.portal.data.PortalRecord;
import dev.botsu.portalcraft.portal.data.PortalWorldData;
import dev.botsu.portalcraft.portal.frame.PortalFrame;
import dev.botsu.portalcraft.portal.resolver.PortalResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Creates or locates the return portal at a teleport destination.
 *
 * <h2>Lookup strategy</h2>
 * <ol>
 *   <li>Search {@link PortalDataStore#getBySeed} in the destination level for a portal
 *       with the same {@link PortalRecord#stableId()} as the source. If found,
 *       the player arrives at its interior centre.</li>
 *   <li>If not found, a new return portal is built near the scaled arrival position:
 *       <ol>
 *         <li>{@link PortalSpawnFinder} locates a safe two-block-tall air column.</li>
 *         <li>A frame is placed using the source signature's dominant block.</li>
 *         <li>The interior is filled with {@link PortalBlock} states.</li>
 *         <li>The new record is registered under the <em>same</em> {@code stableId}
 *             so that the return trip links back.</li>
 *       </ol>
 *   </li>
 * </ol>
 */
public final class ReturnPortalBuilder {

    private ReturnPortalBuilder() {}

    /**
     * Returns the feet-level BlockPos where the player should arrive in
     * {@code destLevel}, reusing an existing portal or constructing a new one.
     *
     * @param destLevel    the destination dimension
     * @param arrivalHint  scaled world position (used as the XZ search centre)
     * @param sourceRecord the portal the entity is exiting
     * @return feet-level arrival position inside the (found or built) return portal
     */
    public static BlockPos findOrBuild(ServerLevel destLevel, BlockPos arrivalHint,
                                       PortalRecord sourceRecord) {
        long stableId = sourceRecord.stableId();

        // 1. Reuse existing paired portal
        Optional<PortalRecord> existing = PortalDataStore.getBySeed(destLevel, stableId);
        if (existing.isPresent()) {
            BlockPos arrival = bottomCenter(existing.get());
            PortalcraftConstants.LOGGER.info(
                "[Portalcraft] Reusing existing return portal at {} in {}",
                arrival, destLevel.dimension().identifier());
            return arrival;
        }

        // 2. Build a new return portal
        PortalRecord returnRecord = buildReturnPortal(destLevel, arrivalHint, sourceRecord);
        BlockPos arrival = bottomCenter(returnRecord);
        PortalcraftConstants.LOGGER.info(
            "[Portalcraft] Built return portal at {} in {}",
            arrival, destLevel.dimension().identifier());
        return arrival;
    }

    // ── Portal placement ──────────────────────────────────────────────────────

    private static PortalRecord buildReturnPortal(ServerLevel level, BlockPos arrivalHint,
                                                   PortalRecord sourceRecord) {
        CanonicalPortalSignature canonical = sourceRecord.canonical();
        int w    = canonical.innerWidth();
        int h    = canonical.innerHeight();
        // Use the source frame's axis for the return portal orientation
        Direction.Axis axis = sourceRecord.frame().axis();

        // Find a safe floor position near the scaled arrival XZ
        BlockPos spawnFeet = PortalSpawnFinder.findSafeSpawn(
            level, arrivalHint.getX(), arrivalHint.getZ());

        int x0 = spawnFeet.getX();
        int y0 = spawnFeet.getY();
        int z0 = spawnFeet.getZ();

        List<BlockPos> frameBlocks = new ArrayList<>();
        List<BlockPos> innerBlocks = new ArrayList<>();

        if (axis == Direction.Axis.X) {
            for (int dy = 0; dy < h; dy++) {
                for (int dx = 0; dx < w; dx++) {
                    innerBlocks.add(new BlockPos(x0 + dx, y0 + dy, z0));
                }
            }
            for (int dx = -1; dx <= w; dx++) frameBlocks.add(new BlockPos(x0 + dx, y0 - 1, z0));
            for (int dx = -1; dx <= w; dx++) frameBlocks.add(new BlockPos(x0 + dx, y0 + h, z0));
            for (int dy = 0; dy < h; dy++) frameBlocks.add(new BlockPos(x0 - 1, y0 + dy, z0));
            for (int dy = 0; dy < h; dy++) frameBlocks.add(new BlockPos(x0 + w, y0 + dy, z0));
        } else {
            for (int dy = 0; dy < h; dy++) {
                for (int dz = 0; dz < w; dz++) {
                    innerBlocks.add(new BlockPos(x0, y0 + dy, z0 + dz));
                }
            }
            for (int dz = -1; dz <= w; dz++) frameBlocks.add(new BlockPos(x0, y0 - 1, z0 + dz));
            for (int dz = -1; dz <= w; dz++) frameBlocks.add(new BlockPos(x0, y0 + h, z0 + dz));
            for (int dy = 0; dy < h; dy++) frameBlocks.add(new BlockPos(x0, y0 + dy, z0 - 1));
            for (int dy = 0; dy < h; dy++) frameBlocks.add(new BlockPos(x0, y0 + dy, z0 + w));
        }

        // Place frame blocks (dominant block from source canonical)
        Block frameBlock = resolveFrameBlock(canonical.dominantBlock());
        BlockState frameState = frameBlock.defaultBlockState();
        for (BlockPos fp : frameBlocks) {
            level.setBlock(fp, frameState, Block.UPDATE_ALL);
        }

        // Fill interior with portal blocks (same color as the source portal)
        PortalColor color = PortalResolver.resolveColor(canonical);
        BlockState portalState = ModBlocks.PORTALCRAFT_PORTAL
            .defaultBlockState()
            .setValue(PortalBlock.AXIS, axis)
            .setValue(PortalBlock.COLOR, color);
        for (BlockPos ip : innerBlocks) {
            level.setBlock(ip, portalState, Block.UPDATE_ALL);
        }

        // Register in data store with the same canonical (same stableId → links back)
        PortalFrame returnFrame = new PortalFrame(
            List.copyOf(frameBlocks), List.copyOf(innerBlocks),
            axis, w, h, canonical.aprilFool());
        PortalRecord returnRecord = new PortalRecord(returnFrame, canonical);
        PortalDataStore.register(level, returnRecord);
        PortalWorldData.getOrCreate(level).addRecord(returnRecord);

        BlockPos centre = innerBlocks.get(innerBlocks.size() / 2);
        level.playSound(null, centre, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0f, 1.0f);

        return returnRecord;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static BlockPos bottomCenter(PortalRecord record) {
        List<BlockPos> inner = record.frame().innerBlocks();
        int w = record.frame().innerWidth();
        return inner.get(w / 2);
    }

    private static Block resolveFrameBlock(String blockId) {
        if (blockId == null || blockId.isEmpty()) return Blocks.STONE;
        try {
            Identifier id = Identifier.parse(blockId);
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null && block != Blocks.AIR) {
                return block;
            }
        } catch (Exception e) {
            PortalcraftConstants.LOGGER.warn(
                "[Portalcraft] Could not resolve frame block '{}', falling back to stone", blockId);
        }
        return Blocks.STONE;
    }
}
