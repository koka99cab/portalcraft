package dev.botsu.portalcraft.bootstrap;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.portal.activation.PortalActivationService;
import dev.botsu.portalcraft.portal.block.PortalBlock;
import dev.botsu.portalcraft.portal.data.PortalDataStore;
import dev.botsu.portalcraft.portal.data.PortalRecord;
import dev.botsu.portalcraft.portal.data.PortalWorldData;
import dev.botsu.portalcraft.portal.dimension.DynamicDimensionManager;
import dev.botsu.portalcraft.portal.dimension.FamilyPresetRegistry;
import dev.botsu.portalcraft.portal.dimension.PortalDimensionTypeRegistry;
import dev.botsu.portalcraft.portal.dimension.PortalGenerationPreset;
import dev.botsu.portalcraft.portal.registry.DimensionRecord;
import dev.botsu.portalcraft.portal.registry.DimensionRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Registers all Portalcraft world-interaction event hooks.
 *
 * <h2>Registered hooks</h2>
 * <ul>
 *   <li><b>UseBlockCallback</b> — intercepts flint-and-steel right-clicks and triggers
 *       the portal activation pipeline.</li>
 *   <li><b>PlayerBlockBreakEvents.AFTER</b> — deactivates any portal whose frame includes
 *       the broken block.</li>
 *   <li><b>ServerWorldEvents.LOAD</b> — restores active portals and procedural dimensions
 *       on server startup.</li>
 * </ul>
 */
public final class PortalcraftEvents {

    private PortalcraftEvents() {}

    public static void register(PortalActivationService activationService) {
        registerWorldLoadHook();
        registerFlintAndSteelHook(activationService);
        registerFrameBreakHook();
    }

    // ── World load ────────────────────────────────────────────────────────────

    private static void registerWorldLoadHook() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            // Always repopulate the in-memory portal store from disk
            PortalWorldData.getOrCreate(world).populateDataStore(world);

            // On Overworld load: restore all procedurally created dimensions from DimensionRegistry.
            // The Overworld is always loaded first, so this runs before players can connect.
            if (world.dimension().equals(Level.OVERWORLD)) {
                // P2: Register all family DimensionTypes before any player connects so that
                // the minecraft:login registry snapshot includes them and the minecraft:respawn
                // packet can encode them by integer ID without crashing.
                PortalDimensionTypeRegistry.registerAll(server);

                DimensionRegistry registry = DimensionRegistry.getOrCreate(world);
                int restored = 0;
                int failed   = 0;

                for (DimensionRecord record : registry.getAllRecords()) {
                    ResourceKey<Level> key = record.dimensionKey();
                    if (server.getLevel(key) != null) continue; // already loaded

                    PortalGenerationPreset preset = FamilyPresetRegistry.getPreset(record.family());
                    ServerLevel level = DynamicDimensionManager.getOrCreate(
                        server, key, preset,
                        record.stableId(),        // B1: restore with the original per-recipe seed
                        record.canonicalSource()  // B2: diversity score re-inferred from stored string
                    );
                    if (level != null) {
                        restored++;
                    } else {
                        failed++;
                        PortalcraftConstants.LOGGER.warn(
                            "[Portalcraft] Could not restore dimension {} from registry.",
                            key.identifier());
                    }
                }

                if (restored > 0 || failed > 0) {
                    PortalcraftConstants.LOGGER.info(
                        "[Portalcraft] Dimension restore: {} ok, {} failed.", restored, failed);
                }
            }
        });
    }

    // ── Flint-and-steel activation ────────────────────────────────────────────

    private static void registerFlintAndSteelHook(PortalActivationService activationService) {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!(player.getItemInHand(hand).getItem() instanceof FlintAndSteelItem)) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            BlockPos clicked  = hitResult.getBlockPos();
            Direction hitFace = hitResult.getDirection();
            BlockPos seedFar  = clicked.relative(hitFace.getOpposite());
            BlockPos seedNear = clicked.relative(hitFace);

            PortalcraftConstants.LOGGER.info(
                "[Portalcraft] UseBlockCallback: clicked={} face={} seedFar={} seedNear={}",
                clicked, hitFace, seedFar, seedNear);

            boolean activated = activationService.tryActivate(level, seedFar, player)
                             || activationService.tryActivate(level, seedNear, player);
            if (!activated) {
                PortalcraftConstants.LOGGER.info("[Portalcraft] No portal activated at either seed.");
                return InteractionResult.PASS;
            }

            if (!player.isCreative()) {
                EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
                player.getItemInHand(hand).hurtAndBreak(1, player, slot);
            }

            return InteractionResult.SUCCESS;
        });
    }

    // ── Frame break deactivation ──────────────────────────────────────────────

    private static void registerFrameBreakHook() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide()) return;
            ServerLevel serverLevel = (ServerLevel) level;

            PortalDataStore.getByFramePos(serverLevel, pos).ifPresent(record ->
                deactivatePortal(serverLevel, record)
            );
        });
    }

    // ── Portal deactivation ───────────────────────────────────────────────────

    static void deactivatePortal(ServerLevel level, PortalRecord record) {
        PortalDataStore.deactivate(level, record);
        PortalWorldData.getOrCreate(level).removeRecord(record);
        for (BlockPos inner : record.frame().innerBlocks()) {
            if (level.getBlockState(inner).getBlock() instanceof PortalBlock) {
                level.setBlock(inner, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        PortalcraftConstants.LOGGER.debug("[Portalcraft] Portal deactivated.");
    }
}
