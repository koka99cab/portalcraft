package dev.botsu.portalcraft.portal.activation;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.portal.analysis.PortalStructure;
import dev.botsu.portalcraft.portal.analysis.PortalStructureExtractor;
import dev.botsu.portalcraft.portal.block.ModBlocks;
import dev.botsu.portalcraft.portal.block.PortalBlock;
import dev.botsu.portalcraft.portal.block.PortalColor;
import dev.botsu.portalcraft.portal.canonical.CanonicalPortalSignature;
import dev.botsu.portalcraft.portal.canonical.PortalCanonicalizer;
import dev.botsu.portalcraft.portal.data.PortalDataStore;
import dev.botsu.portalcraft.portal.data.PortalRecord;
import dev.botsu.portalcraft.portal.data.PortalWorldData;
import dev.botsu.portalcraft.portal.frame.DefaultPortalFrameDetector;
import dev.botsu.portalcraft.portal.frame.DefaultPortalFrameValidator;
import dev.botsu.portalcraft.portal.frame.PortalFrame;
import dev.botsu.portalcraft.portal.frame.PortalFrameDetector;
import dev.botsu.portalcraft.portal.frame.PortalFrameScanResult;
import dev.botsu.portalcraft.portal.frame.PortalFrameValidator;
import dev.botsu.portalcraft.portal.identity.PortalIdentity;
import dev.botsu.portalcraft.portal.identity.PortalIdentityDeriver;
import dev.botsu.portalcraft.portal.integration.PortalRuntime;
import dev.botsu.portalcraft.portal.resolver.PortalResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Default implementation of {@link PortalActivationService}.
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li>Try X axis, then Z axis, at the given seed position.</li>
 *   <li>Scan for a rectangular frame ({@link DefaultPortalFrameDetector}).</li>
 *   <li>Validate frame block tags ({@link DefaultPortalFrameValidator}).</li>
 *   <li>[Stage 1] {@link PortalStructureExtractor} reads material counts → {@code PortalStructure}.</li>
 *   <li>[Stage 2] {@link PortalCanonicalizer} normalises → {@code CanonicalPortalSignature}.</li>
 *   <li>[Stage 3] {@link PortalIdentityDeriver} hashes → {@code PortalIdentity}.</li>
 *   <li>Fill interior with {@link PortalBlock} states.</li>
 *   <li>Store the portal in {@link PortalDataStore}.</li>
 *   <li>Play activation sound and notify {@link PortalRuntime}.</li>
 * </ol>
 */
public class DefaultPortalActivationService implements PortalActivationService {

    private final PortalFrameDetector detector;
    private final PortalFrameValidator validator;
    private final PortalRuntime runtime;

    public DefaultPortalActivationService(PortalRuntime runtime) {
        this.detector = new DefaultPortalFrameDetector();
        this.validator = new DefaultPortalFrameValidator();
        this.runtime = runtime;
    }

    @Override
    public boolean tryActivate(Level level, BlockPos seedPos, Player player) {
        if (level.isClientSide()) return false;
        ServerLevel serverLevel = (ServerLevel) level;

        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            if (tryActivateAxis(serverLevel, seedPos, axis)) {
                return true;
            }
        }
        return false;
    }

    // ── Per-axis activation ───────────────────────────────────────────────────

    private boolean tryActivateAxis(ServerLevel level, BlockPos seedPos, Direction.Axis axis) {
        PortalcraftConstants.LOGGER.info("[Portalcraft] tryActivateAxis seed={} axis={}", seedPos, axis);

        // 1. Geometry scan
        PortalFrameScanResult scan = detector.scan(level, seedPos, axis);
        if (!scan.valid()) {
            PortalcraftConstants.LOGGER.info("[Portalcraft] Scan FAILED (axis={})", axis);
            return false;
        }

        // 2. Tag validation
        Optional<PortalFrame> frameOpt = validator.validate(level, scan);
        if (frameOpt.isEmpty()) {
            PortalcraftConstants.LOGGER.info("[Portalcraft] Validation FAILED (axis={})", axis);
            return false;
        }
        PortalFrame frame = frameOpt.get();
        PortalcraftConstants.LOGGER.info("[Portalcraft] Validation OK: {}x{} (axis={})",
            frame.innerWidth(), frame.innerHeight(), axis);

        // 3. Stage 1 — structural extraction
        PortalStructure structure = PortalStructureExtractor.extract(level, frame);

        // 4. Stage 2 — canonical normalisation
        CanonicalPortalSignature canonical = PortalCanonicalizer.canonicalize(structure);

        // 5. Stage 3 — stable identity derivation
        PortalIdentity identity = PortalIdentityDeriver.derive(canonical);

        // 6. Fill interior with portal blocks (color derived from dominant family)
        PortalColor color = PortalResolver.resolveColor(canonical);
        BlockState portalState = ModBlocks.PORTALCRAFT_PORTAL
            .defaultBlockState()
            .setValue(PortalBlock.AXIS, axis)
            .setValue(PortalBlock.COLOR, color);
        for (BlockPos inner : frame.innerBlocks()) {
            level.setBlock(inner, portalState, Block.UPDATE_ALL);
        }

        // 7. Store in data store and persist to disk
        PortalRecord record = new PortalRecord(frame, canonical);
        PortalDataStore.register(level, record);
        PortalWorldData.getOrCreate(level).addRecord(record);

        // 8. Play activation sound
        BlockPos center = frame.innerBlocks().get(frame.innerBlocks().size() / 2);
        level.playSound(null, center, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0f, 1.0f);

        // 9. Log + notify runtime
        PortalcraftConstants.LOGGER.info(
            "[Portalcraft] Portal activated at {} ({}×{} axis={} id={} canonical=[{}] april_fool={})",
            center, frame.innerWidth(), frame.innerHeight(), axis,
            Long.toHexString(identity.stableId()), canonical.canonicalString(), canonical.aprilFool());

        runtime.onPortalActivated(level, center, identity, canonical);
        return true;
    }
}
