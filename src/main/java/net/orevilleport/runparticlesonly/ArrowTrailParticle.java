package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

public final class ArrowTrailParticle extends SingleQuadParticle {
   private static final int TEXTURE_WIDTH = 144;
   private static final int TEXTURE_HEIGHT = 32;
   private static final int FRAME_V = 16;
   private static final int FRAME_WIDTH = 16;
   private static final int FRAME_HEIGHT = 16;
   private static final int FRAME_STEP_U = 16;
   private static final int FLIPBOOK_FRAMES = 9;
   private static final float SCALE = 0.125F;
   private static final float VELOCITY_MULTIPLIER = (float)Math.exp(-0.1);

   private ArrowTrailParticle(
      ClientLevel world,
      double x,
      double y,
      double z,
      double xd,
      double yd,
      double zd,
      SpriteSet spriteProvider,
      ColorParticleOption effect,
      RandomSource random
   ) {
      super(world, x, y, z, xd, yd, zd, spriteProvider.get(random));
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.lifetime = 6 + random.nextInt(15);
      this.quadSize = 0.125F;
      this.alpha = 1.0F;
      this.roll = (float)Math.toRadians(random.nextInt(4) * 90.0);
      this.oRoll = this.roll;
      this.setColor(effect.getRed(), effect.getGreen(), effect.getBlue());
   }

   protected float getU0() {
      return this.sprite.getU(this.currentFlipbookU() / 144.0F);
   }

   protected float getU1() {
      return this.sprite.getU((this.currentFlipbookU() + 16) / 144.0F);
   }

   protected float getV0() {
      return this.sprite.getV(0.5F);
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
         this.move(this.xd, this.yd, this.zd);
         this.xd = this.xd * VELOCITY_MULTIPLIER;
         this.yd = this.yd * VELOCITY_MULTIPLIER;
         this.zd = this.zd * VELOCITY_MULTIPLIER;
      }
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   private int currentFlipbookU() {
      return this.currentFlipbookFrame() * 16;
   }

   private int currentFlipbookFrame() {
      float life = Math.min(0.999F, (float)this.age / Math.max(1, this.lifetime));
      return Math.min(8, (int)(life * 9.0F));
   }

   public static final class Factory implements ParticleProvider<ColorParticleOption> {
      private final SpriteSet spriteProvider;

      public Factory(SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         ColorParticleOption parameters, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return !RunParticlesConfig.get().arrowTrailParticles
            ? null
            : new ArrowTrailParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, parameters, random);
      }
   }
}
