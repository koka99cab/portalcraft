package dev.botsu.portalcraft.client;

import dev.botsu.portalcraft.PortalcraftConstants;
import dev.botsu.portalcraft.client.particle.ColoredPortalParticle;
import dev.botsu.portalcraft.portal.block.ModBlocks;
import dev.botsu.portalcraft.portal.block.PortalBlock;
import dev.botsu.portalcraft.portal.particle.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * Client-only entrypoint for Portalcraft.
 *
 * <p>Registers:
 * <ul>
 *   <li>Translucent render layer for the portal block.</li>
 *   <li>Block colour provider — tints the portal texture from the {@link PortalBlock#COLOR}
 *       block state, giving each portal family its own hue without extra textures.</li>
 *   <li>Particle factory for {@link ModParticles#COLORED_PORTAL} — the vanilla-style
 *       swirling particle with a family-specific RGB tint.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class PortalcraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Translucent render layer so the portal blends with its surroundings
        BlockRenderLayerMap.putBlock(ModBlocks.PORTALCRAFT_PORTAL, ChunkSectionLayer.TRANSLUCENT);

        // Block colour provider: returns the packed RGB from the COLOR block state
        ColorProviderRegistry.BLOCK.register(
            (state, world, pos, tintIndex) -> state.getValue(PortalBlock.COLOR).getRgb(),
            ModBlocks.PORTALCRAFT_PORTAL
        );

        // Colored portal particle: vanilla portal motion + family-specific RGB tint
        ParticleFactoryRegistry.getInstance().register(
            ModParticles.COLORED_PORTAL,
            ColoredPortalParticle.Provider::new
        );

        PortalcraftConstants.LOGGER.info("{} client initialized.", PortalcraftConstants.MOD_NAME);
    }
}
