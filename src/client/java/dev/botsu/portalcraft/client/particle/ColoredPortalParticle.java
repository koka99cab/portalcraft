package dev.botsu.portalcraft.client.particle;

import dev.botsu.portalcraft.portal.particle.ColoredPortalParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

/**
 * Colored portal particle — replicates the exact motion model of the vanilla
 * {@code PortalParticle} but tints the sprite with the family RGB color.
 *
 * <h2>Motion model (reverse-engineered from PortalParticle bytecode)</h2>
 * <p>The particle does NOT use physics-based velocity. Instead it stores
 * {@code xStart/yStart/zStart} (the spawn position) and, each tick, sets its
 * absolute position using a smoothed interpolation factor:
 * <pre>
 *   f  = age / lifetime                 (0 → 1)
 *   t  = 1 − (2f² − f) = 1 − 2f² + f  (eases in, peaks near f=0)
 *   x  = xStart + xd · t
 *   y  = yStart + yd · t + (1 − f)     ← vertical drift: starts 1 block above, falls to 0
 *   z  = zStart + zd · t
 * </pre>
 * The particle converges to {@code (xStart, yStart, zStart)} at end of life and
 * starts one block above it, creating the characteristic "sucked into the portal" look.
 *
 * <h2>Size model</h2>
 * <pre>
 *   quadSize(f) = 0.1 × max(0, 1 − 4f²)   → shrinks to 0 at f = 0.5, invisible after
 * </pre>
 */
public class ColoredPortalParticle extends SingleQuadParticle {

    private final SpriteSet sprites;
    /** Spawn position — particle converges here over its lifetime. */
    private final double xStart, yStart, zStart;

    ColoredPortalParticle(ClientLevel level, double x, double y, double z,
                          double vx, double vy, double vz, int color, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;

        // Store initial offset velocities (small ±0.25 values from animateTick)
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        // Converge-to point = the spawn position
        this.x      = x;
        this.xStart = x;
        this.yStart = y;
        this.zStart = z;

        // Apply family color as vertex tint
        this.rCol = ((color >> 16) & 0xFF) / 255.0f;
        this.gCol = ((color >>  8) & 0xFF) / 255.0f;
        this.bCol = ( color        & 0xFF) / 255.0f;

        // Vanilla portal particle size and lifetime (from PortalParticle bytecode)
        this.quadSize = 0.1f * (random.nextFloat() * 0.2f + 0.5f);
        this.lifetime = (int)(random.nextFloat() * 10.0f) + 40;
        this.setSpriteFromAge(sprites);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    /**
     * Replicates vanilla {@code PortalParticle.getQuadSize}:
     * particle shrinks to invisible at the midpoint of its life.
     */
    @Override
    public float getQuadSize(float partialTick) {
        float f = ((float) this.age + partialTick) / (float) this.lifetime;
        return 0.1f * Math.max(0.0f, 1.0f - f * f * 4.0f);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    /**
     * Exact replication of vanilla {@code PortalParticle.tick()}.
     */
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.setSpriteFromAge(sprites);

        float f  = (float) this.age / (float) this.lifetime; // 0 → 1
        float f2 = f;                                         // original ratio (for y drift)
        f = 2.0f * f * f - f;                                 // = f*(2f−1)
        f = 1.0f - f;                                         // = 1 − f*(2f−1) = 1 − 2f² + f

        this.x = this.xStart + this.xd * f;
        this.y = this.yStart + this.yd * f + (1.0 - f2);     // starts 1 block above, falls to 0
        this.z = this.zStart + this.zd * f;
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static class Provider implements ParticleProvider<ColoredPortalParticleOptions> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColoredPortalParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz, RandomSource random) {
            return new ColoredPortalParticle(level, x, y, z, vx, vy, vz, options.color(), sprites);
        }
    }
}
