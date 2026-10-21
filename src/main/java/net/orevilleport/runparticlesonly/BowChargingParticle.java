package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class BowChargingParticle extends SingleQuadParticle {
   private static final int TEXTURE_WIDTH = 11;
   private static final int TEXTURE_HEIGHT = 11;
   private static final int UV_SIZE = 1;
   private static final int MAX_AGE = 6;
   private final float randomSize;

   private BowChargingParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteProvider, RandomSource random) {
      super(world, x, y, z, xd, yd, zd, spriteProvider.get(random));
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.lifetime = 6;
      this.randomSize = random.nextFloat();
      this.alpha = 1.0F;
      this.setColor(this.randomSize, 1.0F, 1.0F);
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

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.oRoll = this.roll;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.move(this.xd, this.yd, this.zd);
      }
   }

   public float getQuadSize(float tickProgress) {
      float ageSeconds = (this.age + tickProgress) / 20.0F;
      float size = clamp(this.randomSize * ageSeconds * 16.0F, 0.2F, Math.max(0.2F, this.randomSize));
      return size * 0.03F;
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteProvider;

      public Factory(SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new BowChargingParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, random);
      }
   }
}
