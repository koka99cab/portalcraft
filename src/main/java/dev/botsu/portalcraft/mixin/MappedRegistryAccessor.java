package dev.botsu.portalcraft.mixin;

import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin for {@link MappedRegistry} — exposes the {@code frozen} flag
 * so that {@link dev.botsu.portalcraft.portal.dimension.PortalDimensionTypeRegistry}
 * can temporarily unfreeze the DimensionType registry to register custom entries
 * before the first player connects.
 */
@Mixin(MappedRegistry.class)
public interface MappedRegistryAccessor {

    @Accessor("frozen")
    boolean portalcraft$isFrozen();

    @Accessor("frozen")
    void portalcraft$setFrozen(boolean frozen);
}
