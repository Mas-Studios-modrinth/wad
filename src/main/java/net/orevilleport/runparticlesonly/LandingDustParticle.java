package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class LandingDustParticle extends SingleQuadParticle {
   private static final int TEXTURE_WIDTH = 144;
   private static final int TEXTURE_HEIGHT = 32;
   private static final int FRAME_U = 16;
   private static final int FRAME_V = 16;
   private static final int FRAME_WIDTH = 16;
   private static final int FRAME_HEIGHT = 16;
   private static final int FRAME_STEP_U = 16;
   private static final int FLIPBOOK_FRAMES = 8;
   private static final float SCALE = 0.3F;
   private static final float VELOCITY_MULTIPLIER = 0.7F;

   private LandingDustParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteProvider, RandomSource random) {
      super(world, x, y, z, spriteProvider.get(random));
      this.setParticleSpeed(xd, yd, zd);
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.lifetime = 8 + random.nextInt(9);
      this.quadSize = 0.3F;
      this.alpha = 0.45F;
      this.setColor(0.82745F, 0.77255F, 0.68235F);
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
      } else if (!this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isAir()) {
         this.remove();
      } else {
         this.xd *= 0.7F;
         this.yd *= 0.7F;
         this.zd *= 0.7F;
         this.move(this.xd, this.yd, this.zd);
      }
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   private int currentFlipbookU() {
      return 16 + this.currentFlipbookFrame() * 16;
   }

   private int currentFlipbookFrame() {
      float life = Math.min(0.999F, (float)this.age / Math.max(1, this.lifetime));
      return Math.min(7, (int)(life * 8.0F));
   }

   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteProvider;

      public Factory(SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new LandingDustParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, random);
      }
   }
}
