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
import net.minecraft.world.phys.Vec3;

public final class CriticalHitParticle extends SingleQuadParticle {
   private static final int TEXTURE_WIDTH = 11;
   private static final int TEXTURE_HEIGHT = 11;
   private static final int UV_SIZE = 1;
   private static final double INITIAL_SPEED = 0.45;
   private static final double LINEAR_DRAG_MULTIPLIER = Math.exp(-0.22000000000000003);
   private static final float PARTICLE_HEIGHT = 0.026F;
   private static final int MAX_LINE_SEGMENTS = 18;
   private final double directionX;
   private final double directionY;
   private final double directionZ;
   private final boolean whiteOnly;

   private CriticalHitParticle(
      ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteProvider, RandomSource random, boolean whiteOnly
   ) {
      super(world, x, y, z, xd, yd, zd, spriteProvider.get(random));
      Vec3 direction = normalizedDirection(xd, yd, zd, random);
      this.directionX = direction.x;
      this.directionY = direction.y;
      this.directionZ = direction.z;
      this.whiteOnly = whiteOnly;
      this.xd = direction.x * 0.45;
      this.yd = direction.y * 0.45;
      this.zd = direction.z * 0.45;
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.lifetime = Math.max(1, Math.round((0.2F + random.nextFloat() * 0.25F) * 20.0F));
      this.quadSize = 0.026F;
      this.alpha = 1.0F;
      this.updateActionsStuffColor(0.0F);
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
         this.updateActionsStuffColor((float)this.age / this.lifetime);
         this.move(this.xd, this.yd, this.zd);
         this.xd = this.xd * LINEAR_DRAG_MULTIPLIER;
         this.yd = this.yd * LINEAR_DRAG_MULTIPLIER;
         this.zd = this.zd * LINEAR_DRAG_MULTIPLIER;
      }
   }

   public Layer getLayer() {
      return Layer.OPAQUE;
   }

   private void updateActionsStuffColor(float progress) {
      if (this.whiteOnly) {
         this.rCol = 1.0F;
         this.gCol = 1.0F;
         this.bCol = 1.0F;
      } else {
         float tintProgress = Mth.clamp((progress - 0.2F) / 0.4F, 0.0F, 1.0F);
         this.rCol = 1.0F;
         this.gCol = Mth.lerp(tintProgress, 1.0F, 0.7294118F);
         this.bCol = Mth.lerp(tintProgress, 1.0F, 0.0F);
      }
   }

   private static Vec3 normalizedDirection(double xd, double yd, double zd, RandomSource random) {
      double lengthSquared = xd * xd + yd * yd + zd * zd;
      if (lengthSquared > 1.0E-7) {
         double inverseLength = 1.0 / Math.sqrt(lengthSquared);
         return new Vec3(xd * inverseLength, yd * inverseLength, zd * inverseLength);
      } else {
         double y = random.nextDouble() * 2.0 - 1.0;
         double angle = random.nextDouble() * Math.PI * 2.0;
         double radius = Math.sqrt(Math.max(0.0, 1.0 - y * y));
         return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
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
         return !RunParticlesConfig.get().criticalHitParticles ? null : new CriticalHitParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, random, false);
      }
   }

   public static final class WhiteFactory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteProvider;

      public WhiteFactory(SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return !RunParticlesConfig.get().normalHitParticles ? null : new CriticalHitParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, random, true);
      }
   }
}
