package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class PlayerHurtParticle extends SingleQuadParticle {
   private final float initialAlpha;

   private PlayerHurtParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, RandomSource random) {
      super(level, x, y, z, xd, yd, zd, sprites.get(random));
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.friction = (float)Math.exp(-0.25);
      this.lifetime = 8;
      this.quadSize = 0.05F;
      this.initialAlpha = 1.0F;
      this.alpha = this.initialAlpha;
      this.roll = random.nextFloat() * (float) (Math.PI * 2);
      this.oRoll = this.roll;
      this.setColor(1.0F, 1.0F, 1.0F);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.oRoll = this.roll;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.alpha = this.initialAlpha;
         this.quadSize = Math.max(0.0F, 0.05F - this.age / 160.0F);
         this.move(this.xd, this.yd, this.zd);
         double ageSeconds = this.age / 20.0;
         this.yd -= (12.0 + ageSeconds * 6.0) / 400.0;
      }
   }

   public Layer getLayer() {
      return Layer.OPAQUE;
   }

   protected float getU0() {
      return this.sprite.getU(0.0F);
   }

   protected float getU1() {
      return this.sprite.getU(0.09090909F);
   }

   protected float getV0() {
      return this.sprite.getV(0.0F);
   }

   protected float getV1() {
      return this.sprite.getV(0.09090909F);
   }

   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Factory(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new PlayerHurtParticle(level, x, y, z, xd, yd, zd, this.sprites, random);
      }
   }
}
