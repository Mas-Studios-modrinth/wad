package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class PotionSparkleParticle extends SingleQuadParticle {
   private static final float VELOCITY_MULTIPLIER = (float)Math.exp(-0.05);
   private final double accelerationX;
   private final double accelerationZ;

   private PotionSparkleParticle(
      ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteProvider, RandomSource random
   ) {
      super(world, x, y, z, xd, yd, zd, spriteProvider.get(random));
      this.accelerationX = (random.nextDouble() * 6.0 - 3.0) / 400.0;
      this.accelerationZ = (random.nextDouble() * 6.0 - 3.0) / 400.0;
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.lifetime = Math.max(1, Math.round((0.4F + random.nextFloat() * 0.8F) * 20.0F));
      this.quadSize = 0.2F;
      this.setColor(1.0F, 1.0F, 1.0F);
      this.alpha = 1.0F;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.oRoll = this.roll;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float life = (float)this.age / Math.max(1, this.lifetime);
         this.alpha = life < 0.7F ? 1.0F : Mth.lerp((life - 0.7F) / 0.3F, 1.0F, 0.45882353F);
         this.xd = this.xd + this.accelerationX;
         this.zd = this.zd + this.accelerationZ;
         this.move(this.xd, this.yd, this.zd);
         this.xd = this.xd * VELOCITY_MULTIPLIER;
         this.yd = this.yd * VELOCITY_MULTIPLIER;
         this.zd = this.zd * VELOCITY_MULTIPLIER;
      }
   }

   public float getQuadSize(float tickProgress) {
      float life = Math.min(0.999F, (this.age + tickProgress) / Math.max(1, this.lifetime));
      return this.quadSize * actionsStuffSizeCurve(life);
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public int getLightCoords(float tint) {
      return 15728880;
   }

   private static float actionsStuffSizeCurve(float life) {
      if (life < 0.1F) {
         return Mth.lerp(life / 0.1F, 0.5F, 1.0F);
      } else {
         return life < 0.7F ? 1.0F : Mth.lerp((life - 0.7F) / 0.3F, 1.0F, 0.25F);
      }
   }

   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteProvider;

      public Factory(SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return !RunParticlesConfig.get().healingPotionRings ? null : new PotionSparkleParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, random);
      }
   }
}
