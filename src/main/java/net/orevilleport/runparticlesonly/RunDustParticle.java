package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class RunDustParticle extends SingleQuadParticle {
   private static final int TEXTURE_WIDTH = 144;
   private static final int TEXTURE_HEIGHT = 32;
   private static final int FRAME_U = 0;
   private static final int FRAME_V = 0;
   private static final int FRAME_WIDTH = 16;
   private static final int FRAME_HEIGHT = 16;
   private static final int FRAME_STEP_U = 16;
   private static final int FLIPBOOK_FRAMES = 9;
   private final int flipbookStartOffset;
   private final float initialScale;
   private final float initialAlpha;

   private RunDustParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteProvider, RandomSource random) {
      super(world, x, y, z, xd, yd, zd, spriteProvider.get(random));
      RunParticlesConfig config = RunParticlesConfig.get();
      this.flipbookStartOffset = random.nextInt(9);
      this.hasPhysics = true;
      this.friction = 0.82F;
      this.gravity = 0.0F;
      this.lifetime = 12 + random.nextInt(7);
      this.initialScale = (0.23F + random.nextFloat() * 0.11F) * config.runDustScaleMultiplier();
      this.initialAlpha = Math.min(1.0F, 0.78F * config.runDustAlphaMultiplier());
      this.quadSize = this.initialScale;
      this.alpha = this.initialAlpha;
      this.roll = random.nextFloat() * (float) (Math.PI * 2);
      this.oRoll = this.roll;
      this.setColor(config.runDustRed(), config.runDustGreen(), config.runDustBlue());
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
         float life = (float)this.age / this.lifetime;
         this.alpha = this.initialAlpha * (1.0F - life * life);
         this.roll += 0.045F;
         this.move(this.xd, this.yd, this.zd);
         this.xd = this.xd * this.friction;
         this.yd = this.yd * this.friction;
         this.zd = this.zd * this.friction;
      }
   }

   public float getQuadSize(float tickProgress) {
      float life = (this.age + tickProgress) / this.lifetime;
      float grow = Math.min(1.0F, life * 5.0F);
      return this.initialScale * grow;
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   private int currentFlipbookU() {
      int frame = this.currentFlipbookFrame();
      return 0 + frame * 16;
   }

   private int currentFlipbookFrame() {
      float life = Math.min(0.999F, (float)this.age / Math.max(1, this.lifetime));
      int frame = Math.min(8, (int)(life * 9.0F));
      return (frame + this.flipbookStartOffset) % 9;
   }

   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteProvider;

      public Factory(SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new RunDustParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, random);
      }
   }
}
