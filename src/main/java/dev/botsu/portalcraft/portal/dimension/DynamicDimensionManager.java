package dev.botsu.portalcraft.portal.dimension;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.mixin.MinecraftServerAccessor;
import dev.botsu.portalcraft.portal.resolver.PortalFamily;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.CheckerboardColumnBiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and registers procedurally generated {@link ServerLevel}s at runtime.
 *
 * <p>Each Portalcraft portal produces a deterministic {@link PortalGenerationPreset}
 * which this class compiles into a custom {@link DimensionType}
 * + {@link NoiseBasedChunkGenerator} pair, wraps in a {@link LevelStem}, and
 * registers directly into the server's level map via {@link MinecraftServerAccessor}.
 *
 * <h2>DimensionType (V2)</h2>
 * <p>Each portal family has a dedicated {@link DimensionType} registered at world-load by
 * {@link PortalDimensionTypeRegistry}. Using a pre-registered holder avoids the
 * {@code "Can't find id for Direct{DimensionType[...]}"} crash that occurred when trying to
 * encode an unregistered holder in the {@code minecraft:respawn} packet.
 *
 * <h2>Surface rules (P3)</h2>
 * <p>{@link FamilySurfaceRules} injects per-family surface and default-block overrides into
 * a copy of the vanilla {@link NoiseGeneratorSettings} template.
 * The settings holder is {@link Holder#direct} (server-side only, never synced to clients).
 *
 * <h2>Block-driven uniqueness (B1 + B2)</h2>
 * <ul>
 *   <li><b>B1 — Unique seed:</b> generation seed = {@code worldSeed XOR stableId}.
 *       Two portals of the same family but different materials produce different terrain.</li>
 *   <li><b>B2 — diversityScore modulation:</b> the number of distinct block types in the
 *       portal frame shifts the noise template between OVERWORLD and AMPLIFIED.
 *       Mono-block frames ({@code diversityScore == 1}) always produce calm terrain;
 *       rich palettes ({@code diversityScore >= 5}) always produce amplified terrain.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * <p>{@link #getOrCreate} is synchronized on the server instance to prevent duplicate
 * creation if two players traverse the same portal simultaneously.
 */
public final class DynamicDimensionManager {

    private DynamicDimensionManager() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the existing {@link ServerLevel} for {@code key}, or creates and
     * registers a new one from {@code preset} if it does not yet exist.
     *
     * @param stableId       64-bit portal identity hash — XOR-ed into the world seed so
     *                       every unique portal recipe produces visually distinct terrain.
     * @param canonicalSource canonical string from Stage 2 — used to infer
     *                       {@code diversityScore} without storing it separately.
     * @return the level, or {@code null} if creation failed
     */
    public static ServerLevel getOrCreate(MinecraftServer server,
                                          ResourceKey<Level> key,
                                          PortalGenerationPreset preset,
                                          long stableId,
                                          String canonicalSource) {
        ServerLevel existing = server.getLevel(key);
        if (existing != null) return existing;

        synchronized (server) {
            existing = server.getLevel(key);
            if (existing != null) return existing;
            return createLevel(server, key, preset, stableId, canonicalSource);
        }
    }

    // ── Level creation ────────────────────────────────────────────────────────

    /**
     * Creates and registers a new {@link ServerLevel} from the given preset.
     * Persistence across restarts is handled by {@link dev.botsu.portalcraft.portal.registry.DimensionRegistry}.
     *
     * @return the newly created level, or {@code null} on failure
     */
    static ServerLevel createLevel(MinecraftServer server,
                                   ResourceKey<Level> key,
                                   PortalGenerationPreset preset,
                                   long stableId,
                                   String canonicalSource) {
        try {
            MinecraftServerAccessor accessor = (MinecraftServerAccessor) server;
            var registries = server.registryAccess();

            // ── DimensionType (registered per-family, serialisable over the network) ──
            Holder<DimensionType> dimTypeHolder = PortalDimensionTypeRegistry.getHolder(server, preset.family());

            // ── B2: diversity score — number of distinct block types in the frame ──
            int diversityScore = inferDiversityScore(canonicalSource);

            // ── ChunkGenerator ────────────────────────────────────────────────
            NoiseBasedChunkGenerator generator = buildChunkGenerator(registries, preset, diversityScore, canonicalSource);

            // ── LevelStem ─────────────────────────────────────────────────────
            LevelStem stem = new LevelStem(dimTypeHolder, generator);

            // ── ServerLevelData (delegate to Overworld's PrimaryLevelData) ────
            ServerLevelData primaryData = server.getWorldData().overworldData();
            DerivedLevelData levelData = new DerivedLevelData(server.getWorldData(), primaryData);

            // ── B1: unique seed = worldSeed XOR stableId ──────────────────────
            // Same family, different portal recipe → different terrain.
            long seed = server.getWorldData().worldGenOptions().seed() ^ stableId;

            // ── Construct ServerLevel ─────────────────────────────────────────
            ServerLevel newLevel = new ServerLevel(
                server,
                accessor.portalcraft$getExecutor(),
                accessor.portalcraft$getStorageSource(),
                levelData,
                key,
                stem,
                false,             // not a debug world
                seed,
                List.<CustomSpawner>of(),
                false,             // don't auto-advance time
                null               // RandomSequences — created on demand
            );

            // ── Register in the server's level map ────────────────────────────
            accessor.portalcraft$getLevels().put(key, newLevel);

            // ── Fire Fabric lifecycle event ───────────────────────────────────
            ServerWorldEvents.LOAD.invoker().onWorldLoad(server, newLevel);

            PortalcraftConstants.LOGGER.info(
                "[Portalcraft] Created procedural dimension {} (family={}, diversity={}, seed={})",
                key.identifier(), preset.family(), diversityScore, Long.toHexString(seed));

            return newLevel;

        } catch (Exception e) {
            PortalcraftConstants.LOGGER.error(
                "[Portalcraft] Failed to create dimension {}: {}",
                key.identifier(), e.getMessage(), e);
            return null;
        }
    }

    // ── ChunkGenerator construction ────────────────────────────────────────────

    /**
     * Builds a {@link NoiseBasedChunkGenerator} for the given preset and diversity.
     *
     * <h3>F — Novelty path (COLOR family)</h3>
     * <p>COLOR portals bypass all palette/biome logic and are handled by
     * {@link NoveltyGenerator}:
     * <ul>
     *   <li>Surface blocks extracted from the actual wool/glass frame, applied in
     *       altitude bands so AMPLIFIED terrain shows vivid horizontal colour stripes.</li>
     *   <li>{@link net.minecraft.world.level.biome.CheckerboardColumnBiomeSource}
     *       with 5 colourful biomes at 32-block resolution.</li>
     *   <li>Forced AMPLIFIED noise, sea-level 63, aquifers on, ore veins off.</li>
     * </ul>
     *
     * <h3>C — Data-driven palettes (all other families)</h3>
     * <p>Block selection is resolved through {@link BlockPaletteRegistry} via
     * {@code preset.blockPalette()}.
     */
    private static NoiseBasedChunkGenerator buildChunkGenerator(
            net.minecraft.core.RegistryAccess registries,
            PortalGenerationPreset preset,
            int diversityScore,
            String canonicalSource) {

        HolderGetter<NoiseGeneratorSettings> noiseGetter =
            registries.lookup(Registries.NOISE_SETTINGS).orElseThrow();
        HolderGetter<Biome> biomeGetter =
            registries.lookup(Registries.BIOME).orElseThrow();

        // ── F: Novelty path — COLOR family only ────────────────────────────────
        if (preset.family() == PortalFamily.COLOR) {
            return buildNoveltyChunkGenerator(noiseGetter, biomeGetter, preset, canonicalSource);
        }

        // ── C/P3/E: Standard path for all other families ───────────────────────
        // Template provides terrain shape / noise router / ore-vein rules
        NoiseGeneratorSettings template =
            noiseGetter.getOrThrow(selectNoiseSettings(preset, diversityScore)).value();

        // C: palette-keyed overrides — sea level, aquifers, ore veins
        String palette = preset.blockPalette();
        int seaLevel = switch (palette) {
            case "marine", "jungle" -> 63;
            case "desert"           -> 30;
            default                 -> 0;
        };
        boolean aquifers = switch (palette) {
            case "stone", "desert", "end", "crystal", "dark", "light", "ancient", "ice", "infernal" -> false;
            default -> template.aquifersEnabled();
        };
        boolean oreVeins = switch (palette) {
            case "end", "crystal" -> false;
            default               -> template.oreVeinsEnabled();
        };

        // C: data-driven surface rule + defaultBlock from BlockPaletteRegistry
        NoiseGeneratorSettings custom = new NoiseGeneratorSettings(
            template.noiseSettings(),
            FamilySurfaceRules.defaultBlockForPreset(preset),
            template.defaultFluid(),
            template.noiseRouter(),
            FamilySurfaceRules.buildForPreset(preset),
            template.spawnTarget(),
            seaLevel,
            template.disableMobGeneration(),
            aquifers,
            oreVeins,
            template.useLegacyRandomSource()
        );

        BiomeSource biomeSource = buildBiomeSource(biomeGetter, preset, canonicalSource);
        return new NoiseBasedChunkGenerator(biomeSource, Holder.direct(custom));
    }

    /**
     * Novelty chunk generator for the COLOR (April Fool) family.
     *
     * <ul>
     *   <li>Noise: always AMPLIFIED (ignores diversity score).</li>
     *   <li>Surface: altitude-banded wool/glass blocks from the canonical frame.</li>
     *   <li>Default block: stone (underground stays neutral).</li>
     *   <li>Sea level: 63, aquifers: on, ore veins: off.</li>
     *   <li>Biome source: {@link CheckerboardColumnBiomeSource} at 32-block scale.</li>
     * </ul>
     */
    private static NoiseBasedChunkGenerator buildNoveltyChunkGenerator(
            HolderGetter<NoiseGeneratorSettings> noiseGetter,
            HolderGetter<Biome> biomeGetter,
            PortalGenerationPreset preset,
            String canonicalSource) {

        // Always AMPLIFIED — maximum chaos
        NoiseGeneratorSettings template =
            noiseGetter.getOrThrow(NoiseGeneratorSettings.AMPLIFIED).value();

        // F: extract actual wool/glass blocks from the portal frame
        List<net.minecraft.world.level.block.state.BlockState> frameBlocks =
            NoveltyGenerator.extractFrameBlocks(canonicalSource);

        NoiseGeneratorSettings custom = new NoiseGeneratorSettings(
            template.noiseSettings(),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), // neutral underground
            template.defaultFluid(),
            template.noiseRouter(),
            NoveltyGenerator.buildSurfaceRules(frameBlocks), // altitude-banded wool
            template.spawnTarget(),
            63,                          // sea level (water between coloured mountains)
            template.disableMobGeneration(),
            template.aquifersEnabled(),  // aquifers on (dramatic water pockets)
            false,                       // ore veins off (not thematic for colour worlds)
            template.useLegacyRandomSource()
        );

        net.minecraft.world.level.biome.CheckerboardColumnBiomeSource biomeSource =
            NoveltyGenerator.buildCheckerboardBiomeSource(biomeGetter);

        return new NoiseBasedChunkGenerator(biomeSource, Holder.direct(custom));
    }

    /**
     * Maps a preset + diversity score to the most appropriate vanilla noise-settings key.
     *
     * <h3>Selection rules (in priority order)</h3>
     * <ol>
     *   <li>Ceiling dimensions (MARINE, INFERNAL) → NETHER.</li>
     *   <li>Per-family overrides: STONE → CAVES, END → END, LIGHT → FLOATING_ISLANDS.</li>
     *   <li>B2 — diversityScore:
     *     <ul>
     *       <li>{@code == 1} (mono-block) → always OVERWORLD (calm, peaceful).</li>
     *       <li>{@code >= 5} (rich palette) → always AMPLIFIED (chaotic, dramatic).</li>
     *       <li>2–4 → driven by {@code terrainScale}: {@code > 1.3} → AMPLIFIED, else OVERWORLD.</li>
     *     </ul>
     *   </li>
     * </ol>
     */
    private static ResourceKey<NoiseGeneratorSettings> selectNoiseSettings(
            PortalGenerationPreset preset, int diversityScore) {
        if (preset.hasCeiling()) return NoiseGeneratorSettings.NETHER;
        return switch (preset.family()) {
            case STONE -> NoiseGeneratorSettings.CAVES;
            case END   -> NoiseGeneratorSettings.END;
            case LIGHT -> NoiseGeneratorSettings.FLOATING_ISLANDS;
            default -> {
                if (diversityScore == 1) yield NoiseGeneratorSettings.OVERWORLD;  // mono → calm
                if (diversityScore >= 5) yield NoiseGeneratorSettings.AMPLIFIED;  // rich → dramatic
                // 2–4 blocks: let terrainScale decide
                yield preset.terrainScale() > 1.3f
                    ? NoiseGeneratorSettings.AMPLIFIED
                    : NoiseGeneratorSettings.OVERWORLD;
            }
        };
    }

    /**
     * Infers the diversity score (number of distinct block types) from the canonical
     * source string stored in {@link dev.botsu.portalcraft.portal.registry.DimensionRecord}.
     *
     * <p>Format: {@code "blockA:count,blockB:count,...,w:W,h:H"}.
     * The {@code w:} and {@code h:} tokens are dimension entries, not block entries.
     * Returns at least 1 even for malformed or empty strings.
     */
    public static int inferDiversityScore(String canonicalSource) {
        if (canonicalSource == null || canonicalSource.isEmpty()) return 1;
        int count = 0;
        for (String part : canonicalSource.split(",")) {
            if (!part.startsWith("w:") && !part.startsWith("h:")) count++;
        }
        return Math.max(1, count);
    }

    /**
     * Builds the {@link BiomeSource} for the given preset.
     *
     * <h3>E — Multi-biome per dimension</h3>
     * <ul>
     *   <li><b>COLOR family</b> — single biome computed by {@link ColorBiomeSelector} from the
     *       HSV signature of the portal frame (D milestone).</li>
     *   <li><b>Families with one biome</b> (END, UNKNOWN) — {@link FixedBiomeSource}.</li>
     *   <li><b>Families with two or more biomes</b> — {@link MultiNoiseBiomeSource}: the family's
     *       biome list is spread uniformly along the temperature axis [−1, +1] so that
     *       noise-driven temperature variation produces natural biome transitions.</li>
     * </ul>
     *
     * @param biomeGetter     holder getter for the current registry access
     * @param preset          generation preset for this dimension
     * @param canonicalSource canonical string from Stage 2; used only for COLOR family
     */
    private static BiomeSource buildBiomeSource(HolderGetter<Biome> biomeGetter,
                                                 PortalGenerationPreset preset,
                                                 String canonicalSource) {
        // D: COLOR — per-portal colour-driven single biome
        if (preset.family() == PortalFamily.COLOR) {
            ResourceKey<Biome> colorBiome = ColorBiomeSelector.selectBiome(canonicalSource);
            return new FixedBiomeSource(biomeGetter.getOrThrow(colorBiome));
        }

        // E: all other families — look up biome set
        List<ResourceKey<Biome>> biomes = FamilyBiomeSetRegistry.getBiomes(preset.family());
        if (biomes.size() == 1) {
            return new FixedBiomeSource(biomeGetter.getOrThrow(biomes.get(0)));
        }
        return buildMultiNoiseBiomeSource(biomeGetter, biomes);
    }

    /**
     * Builds a {@link MultiNoiseBiomeSource} distributing {@code biomes} uniformly along
     * the temperature axis [−1, +1].
     *
     * <p>All other climate axes (humidity, continentalness, erosion, depth, weirdness) are
     * set to the full range so that every position in the world matches at least one entry,
     * and only temperature drives biome selection.
     *
     * <p>The temperature noise is provided by the dimension's {@link net.minecraft.world.level.levelgen.NoiseRouter}
     * (from the chosen {@link NoiseGeneratorSettings} template), which varies spatially
     * and produces smooth transitions across large distances.
     *
     * @param biomes ordered list (cold → warm); must have at least 2 entries
     */
    private static MultiNoiseBiomeSource buildMultiNoiseBiomeSource(HolderGetter<Biome> biomeGetter,
                                                                     List<ResourceKey<Biome>> biomes) {
        Climate.Parameter full = Climate.Parameter.span(-1f, 1f);
        float step = 2f / biomes.size();
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> pairs = new ArrayList<>(biomes.size());

        for (int i = 0; i < biomes.size(); i++) {
            float lo = -1f + i * step;
            float hi = lo + step;
            Climate.ParameterPoint point = Climate.parameters(
                Climate.Parameter.span(lo, hi), // temperature band for this biome
                full,                           // humidity — any
                full,                           // continentalness — any
                full,                           // erosion — any
                full,                           // depth — any
                full,                           // weirdness — any
                0f                              // offset
            );
            pairs.add(Pair.of(point, biomeGetter.getOrThrow(biomes.get(i))));
        }

        return MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(pairs));
    }

    /** Returns a stable {@link Identifier} key path for a given seed. */
    public static Identifier dimensionIdentifier(long stableSeed) {
        return Identifier.fromNamespaceAndPath("portalcraft", "dim_" + Long.toHexString(stableSeed));
    }
}
