package dev.botsu.portalcraft.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Accessor mixin for {@link MinecraftServer} — exposes private/protected fields
 * needed by {@link dev.botsu.portalcraft.portal.dimension.DynamicDimensionManager}
 * to register procedurally generated {@link ServerLevel}s at runtime.
 */
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {

    /** The live map of all loaded levels. Write here to register a new dimension. */
    @Accessor("levels")
    Map<ResourceKey<Level>, ServerLevel> portalcraft$getLevels();

    /**
     * The level storage access used for all levels on this server.
     * Secondary dimensions re-use the same access as the Overworld;
     * {@link ServerLevel} derives its on-disk subdirectory from the dimension key.
     */
    @Accessor("storageSource")
    LevelStorageSource.LevelStorageAccess portalcraft$getStorageSource();

    /**
     * The background executor used by the server for async work (chunk I/O, etc.).
     * Passed as the {@code dispatcher} argument to the {@link ServerLevel} constructor.
     */
    @Accessor("executor")
    Executor portalcraft$getExecutor();
}
