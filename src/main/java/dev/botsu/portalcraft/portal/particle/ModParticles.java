package dev.botsu.portalcraft.portal.particle;

import dev.botsu.portalcraft.PortalcraftConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Registers all Portalcraft particle types.
 *
 * <p>Call {@link #register()} exactly once from
 * {@link dev.botsu.portalcraft.Portalcraft#onInitialize()}, after block registration.
 */
public final class ModParticles {

    private ModParticles() {}

    /**
     * Colored portal particle — same swirling motion as the vanilla Nether portal particle
     * but tinted with the RGB color encoded in {@link ColoredPortalParticleOptions}.
     */
    public static final ParticleType<ColoredPortalParticleOptions> COLORED_PORTAL =
        new ParticleType<>(false) {
            @Override
            public com.mojang.serialization.MapCodec<ColoredPortalParticleOptions> codec() {
                return ColoredPortalParticleOptions.CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ColoredPortalParticleOptions> streamCodec() {
                return ColoredPortalParticleOptions.STREAM_CODEC;
            }
        };

    /** Registers all particle types with the game registry. */
    public static void register() {
        Registry.register(
            BuiltInRegistries.PARTICLE_TYPE,
            Identifier.fromNamespaceAndPath(PortalcraftConstants.MOD_ID, "colored_portal"),
            COLORED_PORTAL
        );
        PortalcraftConstants.LOGGER.debug("Portalcraft particle types registered.");
    }
}
