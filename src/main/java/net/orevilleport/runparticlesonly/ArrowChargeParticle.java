package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class ArrowChargeParticle extends SingleQuadParticle {
   private static final int TEXTURE_WIDTH = 12;
   private static final int TEXTURE_HEIGHT = 72;
   private static final int FRAME_HEIGHT = 12;
   private static final int FLIPBOOK_FRAMES = 6;

   private ArrowChargeParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteProvider, RandomSource random) {
      super(world, x, y, z, xd, yd, zd, spriteProvider.get(random));
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.lifetime = 5;
      this.quadSize = 0.32F;
      this.alpha = 1.0F;
   }

   protected float getU0() {
      return this.sprite.getU(0.0F);
   }

   protected float getU1() {
      return this.sprite.getU(1.0F);
   }

   protected float getV0() {
      return this.sprite.getV(this.currentFrameV() / 72.0F);
   }

   protected float getV1() {
      return this.sprite.getV((this.currentFrameV() + 12) / 72.0F);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.oRoll = this.roll;
      if (this.age++ >= this.lifetime) {
         this.remove();
      }
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   private int currentFrameV() {
      float life = Math.min(0.999F, (float)this.age / Math.max(1, this.lifetime));
      return Math.min(5, (int)(life * 6.0F)) * 12;
   }

   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteProvider;

      public Factory(SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new ArrowChargeParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, random);
      }
   }
}
