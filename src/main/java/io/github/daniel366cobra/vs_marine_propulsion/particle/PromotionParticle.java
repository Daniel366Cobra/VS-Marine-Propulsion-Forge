package io.github.daniel366cobra.vs_marine_propulsion.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class PromotionParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected PromotionParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z);

        this.lifetime = 40;
        this.quadSize = 0.5f;
        this.sprites = spriteSet;

        this.setSpriteFromAge(spriteSet);

        this.rCol = 1.0f;
        this.gCol = 0.8f;
        this.bCol = 0.0f;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.xd = 0.0f;
        this.zd = 0.0f;
        this.yd = 0.02f;

        this.move(this.xd, this.yd, this.zd);
        this.setSpriteFromAge(this.sprites);

        // Pulsing
        this.quadSize = 0.5f + 0.05f * Mth.sin((float)this.age / 3.0f);

        if (this.age > this.lifetime - 20) { // Last 20 ticks (1 second)
            this.alpha = (float)(this.lifetime - this.age) / 20.0f;
        }

        if (this.age++ > this.lifetime)
            this.remove();

    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }


    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new PromotionParticle(level, x, y, z, this.sprites);
        }
    }
}