package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class LandingRollRingParticle extends SingleQuadParticle {
   private static final int FLIPBOOK_FRAMES = 12;
   private final SpriteSet sprites;

   private LandingRollRingParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
      super(level, x, y, z, sprites.get(0, 11));
      this.sprites = sprites;
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.lifetime = 8;
      this.quadSize = 1.5F;
      this.alpha = 0.45F;
      this.setColor(0.82745F, 0.77255F, 0.68235F);
      this.setSpriteFromAge(sprites);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.oRoll = this.roll;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.setSpriteFromAge(this.sprites);
      }
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Factory(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new LandingRollRingParticle(level, x, y, z, this.sprites);
      }
   }
}
