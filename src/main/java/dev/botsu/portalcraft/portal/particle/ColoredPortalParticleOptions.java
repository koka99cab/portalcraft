package dev.botsu.portalcraft.portal.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

/**
 * Particle options for the Portalcraft colored portal particle.
 *
 * <p>Carries a single packed {@code 0xRRGGBB} color that the client-side
 * {@link dev.botsu.portalcraft.client.particle.ColoredPortalParticle} uses to tint the
 * portal sprite, giving each portal family its own distinct particle hue.
 */
public record ColoredPortalParticleOptions(int color) implements ParticleOptions {

    /** Codec for serialization to disk / command arguments. */
    public static final MapCodec<ColoredPortalParticleOptions> CODEC =
        com.mojang.serialization.Codec.INT
            .fieldOf("color")
            .xmap(ColoredPortalParticleOptions::new, ColoredPortalParticleOptions::color);

    /** Stream codec for network transmission. */
    public static final StreamCodec<ByteBuf, ColoredPortalParticleOptions> STREAM_CODEC =
        ByteBufCodecs.INT.map(ColoredPortalParticleOptions::new, ColoredPortalParticleOptions::color);

    @Override
    public ParticleType<ColoredPortalParticleOptions> getType() {
        return ModParticles.COLORED_PORTAL;
    }
}
