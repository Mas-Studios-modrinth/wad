package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class RunDustMediumParticle extends SingleQuadParticle {
   private static final int TEXTURE_WIDTH = 144;
   private static final int FRAME_WIDTH = 16;
   private static final int FRAMES = 9;
   private final int startFrame;
   private final float initialScale;
   private final float initialAlpha;

   private RunDustMediumParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, RandomSource random) {
      super(level, x, y, z, xd, yd, zd, sprites.get(random));
      RunParticlesConfig config = RunParticlesConfig.get();
      this.startFrame = random.nextInt(9);
      this.hasPhysics = true;
      this.friction = 0.8F;
      this.gravity = 0.0F;
      this.lifetime = 10 + random.nextInt(6);
      this.initialScale = (0.3F + random.nextFloat() * 0.14F) * config.runDustScaleMultiplier();
      this.initialAlpha = Math.min(1.0F, 0.72F * config.runDustAlphaMultiplier());
      this.quadSize = this.initialScale;
      this.alpha = this.initialAlpha;
      this.roll = random.nextFloat() * (float) (Math.PI * 2);
      this.oRoll = this.roll;
      this.setColor(config.runDustRed(), config.runDustGreen(), config.runDustBlue());
   }

   protected float getU0() {
      return this.sprite.getU(this.frame() * 16.0F / 144.0F);
   }

   protected float getU1() {
      return this.sprite.getU((this.frame() + 1) * 16.0F / 144.0F);
   }

   protected float getV0() {
      return this.sprite.getV(0.0F);
   }

   protected float getV1() {
      return this.sprite.getV(1.0F);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.oRoll = this.roll;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float life = (float)this.age / this.lifetime;
         this.alpha = this.initialAlpha * (1.0F - life * life);
         this.roll += 0.035F;
         this.move(this.xd, this.yd, this.zd);
         this.xd = this.xd * this.friction;
         this.yd = this.yd * this.friction;
         this.zd = this.zd * this.friction;
      }
   }

   public float getQuadSize(float tickProgress) {
      return this.initialScale * Math.min(1.0F, (this.age + tickProgress) / this.lifetime * 4.0F);
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   private int frame() {
      int progress = Math.min(8, (int)((float)this.age / Math.max(1, this.lifetime) * 9.0F));
      return (this.startFrame + progress) % 9;
   }

   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Factory(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new RunDustMediumParticle(level, x, y, z, xd, yd, zd, this.sprites, random);
      }
   }
}
