package dev.botsu.portalcraft.portal.dimension;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.mixin.MappedRegistryAccessor;
import dev.botsu.portalcraft.portal.resolver.PortalFamily;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registers one {@link DimensionType} per {@link PortalFamily} into the server's
 * dynamic registry <em>before</em> the first player connects.
 *
 * <h2>Why this is necessary</h2>
 * <p>When the server sends the {@code minecraft:respawn} (or {@code minecraft:login})
 * packet it serialises the {@link DimensionType} of the destination level by its
 * integer registry ID. An unregistered {@code Holder.direct()} has no such ID and
 * causes an {@link IllegalArgumentException} that disconnects the player.
 *
 * <h2>Registration window</h2>
 * <p>{@link #registerAll} must be called from the Overworld
 * {@link net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents#LOAD} callback,
 * which fires before any player can send a login request.
 * All 13 family dimension types are registered exactly once per world-load.
 *
 * <h2>Registry unfreezing</h2>
 * <p>Dynamic registries are frozen after datapack loading. We temporarily unfreeze
 * the {@link Registries#DIMENSION_TYPE} registry via {@link MappedRegistryAccessor}
 * and restore the frozen state afterwards.
 */
public final class PortalDimensionTypeRegistry {

    private PortalDimensionTypeRegistry() {}

    /** One stable {@code ResourceKey<DimensionType>} per family (e.g. {@code portalcraft:stone}). */
    private static final Map<PortalFamily, ResourceKey<DimensionType>> KEYS = buildKeys();

    private static Map<PortalFamily, ResourceKey<DimensionType>> buildKeys() {
        Map<PortalFamily, ResourceKey<DimensionType>> map = new EnumMap<>(PortalFamily.class);
        for (PortalFamily family : PortalFamily.values()) {
            map.put(family, ResourceKey.create(
                Registries.DIMENSION_TYPE,
                Identifier.fromNamespaceAndPath("portalcraft", family.name().toLowerCase())));
        }
        return Map.copyOf(map);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Registers all family {@link DimensionType}s in the server's dynamic registry.
     * Safe to call multiple times — already-registered families are skipped.
     *
     * @param server the running server (provides registry access)
     */
    public static void registerAll(MinecraftServer server) {
        // lookup() returns HolderLookup.RegistryLookup<T>; the underlying implementation in
        // ImmutableRegistryAccess calls MappedRegistry.asLookup() which returns the MappedRegistry
        // itself. Casting to WritableRegistry<T> therefore works in practice.
        @SuppressWarnings("unchecked")
        WritableRegistry<DimensionType> registry = (WritableRegistry<DimensionType>) server.registryAccess()
            .lookup(Registries.DIMENSION_TYPE)
            .orElseThrow(() -> new IllegalStateException("[Portalcraft] DimensionType registry not found"));

        MappedRegistryAccessor accessor = (MappedRegistryAccessor) registry;
        boolean wasFrozen = accessor.portalcraft$isFrozen();
        if (wasFrozen) accessor.portalcraft$setFrozen(false);

        try {
            int registered = 0;
            for (PortalFamily family : PortalFamily.values()) {
                ResourceKey<DimensionType> key = KEYS.get(family);
                if (registry.containsKey(key)) continue;

                PortalGenerationPreset preset = FamilyPresetRegistry.getPreset(family);
                DimensionType dimType = buildDimensionType(registry, preset);
                registry.register(key, dimType, RegistrationInfo.BUILT_IN);
                registered++;
            }

            if (registered > 0) {
                PortalcraftConstants.LOGGER.info(
                    "[Portalcraft] Registered {} custom DimensionType(s) for portal families.", registered);
            }
        } finally {
            if (wasFrozen) accessor.portalcraft$setFrozen(true);
        }
    }

    /**
     * Returns the registered {@link Holder}{@code <DimensionType>} for the given family.
     * {@link #registerAll} must have been called first.
     *
     * @throws IllegalStateException if the type has not been registered yet
     */
    public static Holder<DimensionType> getHolder(MinecraftServer server, PortalFamily family) {
        HolderGetter<DimensionType> getter = server.registryAccess()
            .lookup(Registries.DIMENSION_TYPE).orElseThrow();
        return getter.getOrThrow(KEYS.get(family));
    }

    // ── DimensionType construction ────────────────────────────────────────────

    private static DimensionType buildDimensionType(
            HolderGetter<DimensionType> registry,
            PortalGenerationPreset preset) {

        DimensionType template = registry.getOrThrow(selectVanillaTemplate(preset)).value();

        boolean hasCeiling  = preset.hasCeiling();
        boolean hasSkyLight = preset.hasSkyLight();

        double coordinateScale = hasCeiling ? 8.0  : 1.0;
        int    minY            = hasSkyLight ? -64  : 0;
        int    height          = hasSkyLight ? 384  : 256;
        int    logicalHeight   = hasCeiling  ? 128  : height;

        // A2: ambientLight derived from preset.
        // Dimensions with a sky (hasSkyLight) receive natural light → 0.0.
        // Ceiling dimensions (MARINE, INFERNAL) match vanilla Nether → 0.1.
        // Other underground dims: perceptual luminance of fogColor drives ambient
        // (range 0.02–0.15 so dark dims are dark, bright dims glow slightly).
        // Full skyColor/fogColor → biome special_effects is deferred to Milestone E.
        float ambientLight;
        if (hasSkyLight) {
            ambientLight = 0.0f;
        } else if (hasCeiling) {
            ambientLight = 0.1f;
        } else {
            int fog = preset.fogColor();
            float r = ((fog >> 16) & 0xFF) / 255f;
            float g = ((fog >>  8) & 0xFF) / 255f;
            float b = ( fog        & 0xFF) / 255f;
            float luma = 0.299f * r + 0.587f * g + 0.114f * b; // perceived brightness 0..1
            ambientLight = 0.02f + luma * 0.13f; // maps to 0.02 (pitch black) … 0.15 (bright fog)
        }

        DimensionType.Skybox skybox = hasSkyLight ? DimensionType.Skybox.OVERWORLD
                                    : (hasCeiling  ? DimensionType.Skybox.NONE
                                                   : DimensionType.Skybox.END);
        DimensionType.CardinalLightType lightType = hasCeiling
            ? DimensionType.CardinalLightType.NETHER
            : DimensionType.CardinalLightType.DEFAULT;

        return new DimensionType(
            false,
            hasSkyLight,
            hasCeiling,
            coordinateScale,
            minY,
            height,
            logicalHeight,
            template.infiniburn(),
            ambientLight,
            template.monsterSettings(),
            skybox,
            lightType,
            template.attributes(),
            template.timelines()
        );
    }

    private static ResourceKey<DimensionType> selectVanillaTemplate(PortalGenerationPreset preset) {
        if (preset.hasCeiling())    return BuiltinDimensionTypes.NETHER;
        if (!preset.hasSkyLight())  return BuiltinDimensionTypes.END;
        return BuiltinDimensionTypes.OVERWORLD;
    }
}
