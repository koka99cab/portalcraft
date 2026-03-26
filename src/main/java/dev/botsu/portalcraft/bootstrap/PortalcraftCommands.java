package dev.botsu.portalcraft.bootstrap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.mixin.MinecraftServerAccessor;
import dev.botsu.portalcraft.portal.dimension.DynamicDimensionManager;
import dev.botsu.portalcraft.portal.registry.DimensionRecord;
import dev.botsu.portalcraft.portal.registry.DimensionRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Registers all Portalcraft server-side commands.
 *
 * <h2>G — Admin commands</h2>
 * <ul>
 *   <li>{@code /portalcraft ping}         — sanity-check; replies "pong".</li>
 *   <li>{@code /portalcraft list [page]}  — paginated list of all registered procedural dimensions.</li>
 *   <li>{@code /portalcraft info <dim>}   — detailed report for one dimension key.</li>
 *   <li>{@code /portalcraft remove <dim>} — evict players, unload level, purge from registry.</li>
 *   <li>{@code /portalcraft tp <dim>}     — teleport the operator to the surface spawn of a dimension.</li>
 * </ul>
 *
 * <p>All commands except {@code ping} require {@link Permissions#COMMANDS_GAMEMASTER} (op level 2).
 * The {@code <dim>} argument uses the {@code dimensionKeyPath} stored in
 * {@link DimensionRecord} (e.g. {@code dim_1a2b3c4d5e6f7a8b}) and supports
 * tab-completion from the live {@link DimensionRegistry}.
 */
public final class PortalcraftCommands {

    private PortalcraftCommands() {}

    private static final int PAGE_SIZE = 10;

    /** ISO-8601 timestamp formatter (UTC) used in list and info output. */
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    // ── Registration ──────────────────────────────────────────────────────────

    /** Called once from {@link dev.botsu.portalcraft.Portalcraft#onInitialize()}. */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            registerAll(dispatcher)
        );
    }

    private static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("portalcraft")

                // ── ping ─────────────────────────────────────────────────────
                .then(Commands.literal("ping")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("[" + PortalcraftConstants.MOD_NAME + "] pong!"),
                            false
                        );
                        return 1;
                    })
                )

                // ── list [page] ───────────────────────────────────────────────
                .then(Commands.literal("list")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .executes(ctx -> cmdList(ctx.getSource(), 1))
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> cmdList(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "page")))
                    )
                )

                // ── info <dim> ────────────────────────────────────────────────
                .then(Commands.literal("info")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("dim", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestDimKeys(ctx.getSource(), builder))
                        .executes(ctx -> cmdInfo(ctx.getSource(),
                            StringArgumentType.getString(ctx, "dim")))
                    )
                )

                // ── remove <dim> ──────────────────────────────────────────────
                .then(Commands.literal("remove")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("dim", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestDimKeys(ctx.getSource(), builder))
                        .executes(ctx -> cmdRemove(ctx.getSource(),
                            StringArgumentType.getString(ctx, "dim")))
                    )
                )

                // ── tp <dim> ──────────────────────────────────────────────────
                .then(Commands.literal("tp")
                    .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("dim", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestDimKeys(ctx.getSource(), builder))
                        .executes(ctx -> cmdTp(ctx.getSource(),
                            StringArgumentType.getString(ctx, "dim")))
                    )
                )
        );
    }

    // ── /portalcraft list [page] ──────────────────────────────────────────────

    /**
     * Prints a paginated list of all registered procedural dimensions,
     * sorted by creation date (newest first). Marks currently loaded dimensions with {@code [loaded]}.
     */
    private static int cmdList(CommandSourceStack source, int page) {
        MinecraftServer server = source.getServer();
        DimensionRegistry registry = DimensionRegistry.getOrCreate(server.overworld());
        List<DimensionRecord> records = new ArrayList<>(registry.getAllRecords());
        records.sort(Comparator.comparingLong(DimensionRecord::createdAtEpoch).reversed());

        int total = records.size();
        if (total == 0) {
            source.sendSuccess(() -> Component.literal(
                "[Portalcraft] No procedural dimensions registered."), false);
            return 0;
        }

        int totalPages  = (total + PAGE_SIZE - 1) / PAGE_SIZE;
        int clampedPage = Math.max(1, Math.min(page, totalPages));
        int from        = (clampedPage - 1) * PAGE_SIZE;
        int to          = Math.min(from + PAGE_SIZE, total);

        source.sendSuccess(() -> Component.literal(String.format(
            "[Portalcraft] Dimensions — page %d/%d (%d total):",
            clampedPage, totalPages, total)), false);

        for (int i = from; i < to; i++) {
            DimensionRecord rec    = records.get(i);
            String          ts     = DATE_FMT.format(Instant.ofEpochSecond(rec.createdAtEpoch()));
            String          loaded = server.getLevel(rec.dimensionKey()) != null ? " [loaded]" : "";
            source.sendSuccess(() -> Component.literal(String.format(
                "  %-28s  family=%-10s  %s%s",
                rec.dimensionKeyPath(), rec.family(), ts, loaded)), false);
        }

        int finalPage = clampedPage;
        if (finalPage < totalPages) {
            source.sendSuccess(() -> Component.literal(
                "  … /portalcraft list " + (finalPage + 1) + " for more"), false);
        }
        return clampedPage;
    }

    // ── /portalcraft info <dim> ───────────────────────────────────────────────

    /**
     * Prints detailed information about a single procedural dimension:
     * family, load status, creation timestamp, diversity score, stable-id hex,
     * and the full canonical source string.
     */
    private static int cmdInfo(CommandSourceStack source, String dimKeyPath) {
        DimensionRecord rec = findRecord(source, dimKeyPath);
        if (rec == null) return 0;

        MinecraftServer server    = source.getServer();
        boolean         loaded    = server.getLevel(rec.dimensionKey()) != null;
        int             diversity = DynamicDimensionManager.inferDiversityScore(rec.canonicalSource());
        String          ts        = DATE_FMT.format(Instant.ofEpochSecond(rec.createdAtEpoch()));

        source.sendSuccess(() -> Component.literal("[Portalcraft] " + dimKeyPath),                       false);
        source.sendSuccess(() -> Component.literal("  Family:    " + rec.family()),                      false);
        source.sendSuccess(() -> Component.literal("  Created:   " + ts + " UTC"),                       false);
        source.sendSuccess(() -> Component.literal("  Loaded:    " + loaded),                            false);
        source.sendSuccess(() -> Component.literal("  Diversity: " + diversity + " block type(s)"),      false);
        source.sendSuccess(() -> Component.literal("  StableId:  " + Long.toHexString(rec.stableId())),  false);
        source.sendSuccess(() -> Component.literal("  Canonical: " + rec.canonicalSource()),             false);
        return 1;
    }

    // ── /portalcraft remove <dim> ─────────────────────────────────────────────

    /**
     * Removes a procedural dimension:
     * <ol>
     *   <li>Any players still inside are teleported to the Overworld spawn.</li>
     *   <li>The {@link ServerLevel} is unloaded from the server's level map via
     *       {@link MinecraftServerAccessor} and the Fabric {@link ServerWorldEvents#UNLOAD} event
     *       is fired so other mods can clean up.</li>
     *   <li>The {@link DimensionRecord} is purged from {@link DimensionRegistry}
     *       so the dimension will not be re-created on the next server start.</li>
     * </ol>
     */
    private static int cmdRemove(CommandSourceStack source, String dimKeyPath) {
        DimensionRecord    rec    = findRecord(source, dimKeyPath);
        if (rec == null) return 0;

        MinecraftServer    server = source.getServer();
        ResourceKey<Level> key    = rec.dimensionKey();

        // ── Evict players ──────────────────────────────────────────────────
        ServerLevel dimLevel = server.getLevel(key);
        if (dimLevel != null) {
            ServerLevel overworld = server.overworld();
            BlockPos    spawnPos  = server.getRespawnData().pos();
            int sy = overworld.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnPos.getX(), spawnPos.getZ());

            for (ServerPlayer player : new ArrayList<>(dimLevel.players())) {
                player.teleport(new TeleportTransition(
                    overworld,
                    new Vec3(spawnPos.getX() + 0.5, sy, spawnPos.getZ() + 0.5),
                    Vec3.ZERO,
                    player.getYRot(),
                    player.getXRot(),
                    TeleportTransition.DO_NOTHING
                ));
            }

            // ── Unload level ───────────────────────────────────────────────
            MinecraftServerAccessor accessor = (MinecraftServerAccessor) server;
            ServerWorldEvents.UNLOAD.invoker().onWorldUnload(server, dimLevel);
            accessor.portalcraft$getLevels().remove(key);
        }

        // ── Purge from registry ────────────────────────────────────────────
        DimensionRegistry.getOrCreate(server.overworld()).remove(rec.stableId());

        PortalcraftConstants.LOGGER.info(
            "[Portalcraft] Admin removed dimension {} (family={})", dimKeyPath, rec.family());
        source.sendSuccess(() -> Component.literal(
            "[Portalcraft] Dimension " + dimKeyPath + " removed."), true);
        return 1;
    }

    // ── /portalcraft tp <dim> ─────────────────────────────────────────────────

    /**
     * Teleports the executing player to the surface of a registered procedural dimension.
     *
     * <p>The destination Y is resolved from the
     * {@link Heightmap.Types#MOTION_BLOCKING_NO_LEAVES} heightmap at (0, 0) so the
     * player lands on solid ground regardless of the dimension's terrain profile.
     * The dimension must be loaded (i.e. have been visited at least once since the last
     * server start).
     */
    private static int cmdTp(CommandSourceStack source, String dimKeyPath)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        DimensionRecord rec = findRecord(source, dimKeyPath);
        if (rec == null) return 0;

        MinecraftServer server = source.getServer();
        ServerLevel     target = server.getLevel(rec.dimensionKey());
        if (target == null) {
            source.sendFailure(Component.literal(
                "[Portalcraft] Dimension " + dimKeyPath + " is not loaded. " +
                "Trigger the portal once to load it, or wait for server-start restoration."));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();

        // Land on the surface at the world origin — suitable for any terrain profile.
        int sy = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        player.teleport(new TeleportTransition(
            target,
            new Vec3(0.5, sy, 0.5),
            Vec3.ZERO,
            player.getYRot(),
            player.getXRot(),
            TeleportTransition.DO_NOTHING
        ));

        source.sendSuccess(() -> Component.literal(
            "[Portalcraft] Teleported to " + dimKeyPath + " at 0 " + sy + " 0."), false);
        return 1;
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /**
     * Looks up a {@link DimensionRecord} by its {@code dimensionKeyPath}.
     * Sends a failure message and returns {@code null} if not found.
     */
    private static DimensionRecord findRecord(CommandSourceStack source, String dimKeyPath) {
        DimensionRegistry registry = DimensionRegistry.getOrCreate(source.getServer().overworld());
        for (DimensionRecord rec : registry.getAllRecords()) {
            if (rec.dimensionKeyPath().equals(dimKeyPath)) return rec;
        }
        source.sendFailure(Component.literal("[Portalcraft] Unknown dimension: " + dimKeyPath));
        return null;
    }

    /**
     * Suggests registered dimension key paths that start with the current input fragment.
     * Used for tab-completion on the {@code info}, {@code remove}, and {@code tp} sub-commands.
     */
    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestDimKeys(
            CommandSourceStack source,
            SuggestionsBuilder builder) {
        String            remaining = builder.getRemaining().toLowerCase();
        DimensionRegistry registry  = DimensionRegistry.getOrCreate(source.getServer().overworld());
        for (DimensionRecord rec : registry.getAllRecords()) {
            if (rec.dimensionKeyPath().startsWith(remaining)) {
                builder.suggest(rec.dimensionKeyPath());
            }
        }
        return builder.buildFuture();
    }
}
