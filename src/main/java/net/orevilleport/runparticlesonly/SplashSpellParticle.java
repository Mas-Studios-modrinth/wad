package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class SplashSpellParticle extends SingleQuadParticle {
   private static final int TEXTURE_WIDTH = 144;
   private static final int TEXTURE_HEIGHT = 32;
   private static final int FRAME_WIDTH = 16;
   private static final int FRAME_HEIGHT = 16;
   private static final int FRAME_STEP_U = 16;
   private static final int FLIPBOOK_FRAMES = 9;
   private static final float SCALE = 0.3F;
   private static final float VELOCITY_MULTIPLIER = (float)Math.exp(-0.125);
   private final float targetRed;
   private final float targetGreen;
   private final float targetBlue;
   private final float accelerationY;

   private SplashSpellParticle(
      ClientLevel world,
      double x,
      double y,
      double z,
      double xd,
      double yd,
      double zd,
      SpriteSet spriteProvider,
      ColorParticleOption effect,
      boolean strongVertical,
      RandomSource random
   ) {
      super(world, x, y, z, xd, yd, zd, spriteProvider.get(random));
      this.targetRed = effect.getRed();
      this.targetGreen = effect.getGreen();
      this.targetBlue = effect.getBlue();
      this.accelerationY = (strongVertical ? 0.9F : 0.1F) * 1.5F / 400.0F;
      this.hasPhysics = true;
      this.gravity = 0.0F;
      this.lifetime = Math.max(1, Math.round((0.5F + random.nextFloat() * 1.3F) * 20.0F));
      this.quadSize = 0.3F;
      this.alpha = 1.0F;
      this.roll = (float)Math.toRadians(random.nextInt(13) * 7.5);
      this.oRoll = this.roll;
      this.updateColor();
   }

   protected float getU0() {
      return this.sprite.getU(this.currentFlipbookU() / 144.0F);
   }

   protected float getU1() {
      return this.sprite.getU((this.currentFlipbookU() + 16) / 144.0F);
   }

   protected float getV0() {
      return this.sprite.getV(0.0F);
   }

   protected float getV1() {
      return this.sprite.getV(0.5F);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.oRoll = this.roll;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.updateColor();
         this.yd = this.yd + this.accelerationY;
         this.move(this.xd, this.yd, this.zd);
         this.xd = this.xd * VELOCITY_MULTIPLIER;
         this.yd = this.yd * VELOCITY_MULTIPLIER;
         this.zd = this.zd * VELOCITY_MULTIPLIER;
      }
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   private void updateColor() {
      float ageSeconds = this.age / 20.0F;
      float whiteMix = Mth.clamp(2.0F - ageSeconds * 4.0F, 0.0F, 1.0F);
      this.rCol = Mth.lerp(whiteMix, this.targetRed, 1.0F);
      this.gCol = Mth.lerp(whiteMix, this.targetGreen, 1.0F);
      this.bCol = Mth.lerp(whiteMix, this.targetBlue, 1.0F);
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
         if (!RunParticlesConfig.get().splashPotionLandingParticles) {
            return null;
         }

         boolean strongVertical = Math.abs(yd) > Math.max(Math.abs(xd), Math.abs(zd)) * 2.0;
         return new SplashSpellParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, parameters, strongVertical, random);
      }
   }
}
