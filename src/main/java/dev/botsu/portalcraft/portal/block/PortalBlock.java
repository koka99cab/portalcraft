package dev.botsu.portalcraft.portal.block;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.portal.data.PortalDataStore;
import dev.botsu.portalcraft.portal.data.PortalRecord;
import dev.botsu.portalcraft.portal.data.PortalWorldData;
import dev.botsu.portalcraft.portal.resolver.PortalResolver;
import dev.botsu.portalcraft.portal.spawn.ReturnPortalBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import dev.botsu.portalcraft.portal.particle.ColoredPortalParticleOptions;
import dev.botsu.portalcraft.portal.particle.ModParticles;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/**
 * The Portalcraft portal interior block — placed by
 * {@link dev.botsu.portalcraft.portal.activation.DefaultPortalActivationService}
 * when a valid frame is activated.
 *
 * <p>Implements {@link Portal} so that the vanilla entity-inside-portal mechanism
 * handles the teleport timer and transition. The destination is resolved by
 * {@link PortalResolver} using the {@link PortalDataStore}.
 *
 * <h2>Block states</h2>
 * <ul>
 *   <li>{@code axis=x/z} — portal plane orientation.</li>
 *   <li>{@code color=violet/amber/…} — visual tint applied client-side by the
 *       block colour provider registered in {@code PortalcraftClient}.</li>
 * </ul>
 *
 * <h2>Collapse behaviour</h2>
 * <p>When any structural neighbour (along the portal axis or vertically) changes to a
 * non-portal block, {@link #updateShape} returns air, cascading across all interior blocks.
 * {@link #affectNeighborsAfterRemoval} cleans up the {@link PortalDataStore}.
 */
public class PortalBlock extends Block implements Portal {

    /** Horizontal axis of the portal plane. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    /** Visual color family; tinted client-side via ColorProviderRegistry. */
    public static final EnumProperty<PortalColor> COLOR =
        EnumProperty.create("color", PortalColor.class);

    private static final VoxelShape SHAPE_X =
        Shapes.create(0.0, 0.0, 6.0 / 16.0, 1.0, 1.0, 10.0 / 16.0);
    private static final VoxelShape SHAPE_Z =
        Shapes.create(6.0 / 16.0, 0.0, 0.0, 10.0 / 16.0, 1.0, 1.0);

    PortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(AXIS, Direction.Axis.X)
            .setValue(COLOR, PortalColor.VIOLET));
    }

    // ── Block state ───────────────────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, COLOR);
    }

    // ── Shape ─────────────────────────────────────────────────────────────────

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    // ── Client ambience ───────────────────────────────────────────────────────

    /**
     * Spawns portal particles and plays the ambient hum on the client.
     * Called once per tick per visible portal block on the client thread only.
     *
     * <p>Uses {@link ModParticles#COLORED_PORTAL} — the vanilla portal swirl motion with the
     * family {@link PortalColor} RGB baked into the vertex color. No dust particles.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Ambient portal sound (~1% chance per tick)
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                0.5f, random.nextFloat() * 0.4f + 0.8f, false);
        }

        // Colored portal particle: vanilla swirl motion, family-tinted sprite
        ColoredPortalParticleOptions options =
            new ColoredPortalParticleOptions(state.getValue(COLOR).getRgb());

        for (int i = 0; i < 4; i++) {
            double px = pos.getX() + random.nextDouble();
            double py = pos.getY() + random.nextDouble();
            double pz = pos.getZ() + random.nextDouble();
            double vx = (random.nextDouble() - 0.5) * 0.5;
            double vy = (random.nextDouble() - 0.5) * 0.5;
            double vz = (random.nextDouble() - 0.5) * 0.5;
            level.addParticle(options, px, py, pz, vx, vy, vz);
        }
    }

    // ── Entity teleportation ──────────────────────────────────────────────────

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos,
                                Entity entity, InsideBlockEffectApplier applier, boolean isPrimary) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    // ── Portal interface ──────────────────────────────────────────────────────

    @Override
    public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        Optional<PortalRecord> recordOpt = PortalDataStore.getByInnerPos(level, pos);
        if (recordOpt.isEmpty()) {
            PortalcraftConstants.LOGGER.warn(
                "[Portalcraft] getPortalDestination: no record at {} in {}",
                pos, level.dimension().identifier());
            return noMove(level, entity);
        }

        PortalRecord record = recordOpt.get();
        ResourceKey<Level> destKey = PortalResolver.resolveDestination(record, level);
        ServerLevel destLevel = level.getServer().getLevel(destKey);

        if (destLevel == null) {
            PortalcraftConstants.LOGGER.warn(
                "[Portalcraft] Dimension {} not found; falling back to overworld.", destKey.identifier());
            destLevel = level.getServer().getLevel(Level.OVERWORLD);
        }
        if (destLevel == null) {
            return noMove(level, entity);
        }

        // Scale coordinates for the dimension transition (Overworld ↔ Nether: 8:1 ratio)
        Vec3 scaledPos = PortalResolver.scaleCoordinates(
            entity.position(), level.dimension(), destKey);
        BlockPos arrivalHint = BlockPos.containing(scaledPos);

        // Find or build the return portal, then arrive at its bottom-centre
        BlockPos arrivalPos = ReturnPortalBuilder.findOrBuild(destLevel, arrivalHint, record);
        Vec3 arrivalVec = Vec3.atBottomCenterOf(arrivalPos);

        return new TeleportTransition(destLevel, arrivalVec, Vec3.ZERO,
            entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND);
    }

    /** Teleport transition that leaves the entity in place (used on error). */
    private static TeleportTransition noMove(ServerLevel level, Entity entity) {
        return new TeleportTransition(level, entity.position(), Vec3.ZERO,
            entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING);
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 80; // 4 seconds, same as Nether portal
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    // ── Collapse propagation ──────────────────────────────────────────────────

    /**
     * Returns air when a structural neighbour (along the portal axis or vertically)
     * changes to a non-portal block, triggering cascade collapse across the interior.
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level,
                                     ScheduledTickAccess tickAccess, BlockPos pos,
                                     Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (isStructuralDirection(direction, state.getValue(AXIS)) && !neighborState.is(this)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, tickAccess, pos, direction,
            neighborPos, neighborState, random);
    }

    /**
     * Returns {@code true} for directions along the portal axis and for vertical directions,
     * which are the directions that hold the portal intact.
     */
    private static boolean isStructuralDirection(Direction dir, Direction.Axis axis) {
        return dir == Direction.UP || dir == Direction.DOWN || dir.getAxis() == axis;
    }

    // ── Data-store cleanup ────────────────────────────────────────────────────

    /**
     * When a portal block is removed (cascade from a frame break, or direct destruction),
     * deactivates the portal's data-store entry so that remaining interior blocks collapse
     * via their own {@link #updateShape} calls, and cleans up any residual portal blocks.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level,
                                               BlockPos pos, boolean movedByPiston) {
        PortalDataStore.getByInnerPos(level, pos).ifPresent(record -> {
            // Deactivate first so re-entrant lookups return empty
            PortalDataStore.deactivate(level, record);
            PortalWorldData.getOrCreate(level).removeRecord(record);
            // Replace any remaining portal blocks with air
            for (BlockPos inner : record.frame().innerBlocks()) {
                if (level.getBlockState(inner).is(this)) {
                    level.setBlock(inner, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        });
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
