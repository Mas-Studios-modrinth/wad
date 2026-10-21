package net.orevilleport.runparticlesonly;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class XpGlowParticle extends SingleQuadParticle {
   private static final float TICKS_PER_SECOND = 20.0F;
   private final SpriteSet spriteProvider;
   private final BedrockXpOrbParticles source;
   private final float frame;
   private final float lifeSeconds;

   private XpGlowParticle(
      ClientLevel world, double x, double y, double z, double xd, double yd, double zd, float frame, SpriteSet spriteProvider, RandomSource random
   ) {
      super(world, x, y, z, xd, yd, zd, spriteProvider.get(random));
      this.spriteProvider = spriteProvider;
      this.source = BedrockXpOrbParticles.get();
      this.frame = frame;
      this.gravity = 0.0F;
      this.friction = 1.0F;
      this.hasPhysics = false;
      this.lifeSeconds = this.source.sparkLifetime(frame, random.nextFloat());
      this.lifetime = Math.max(1, Math.round(this.lifeSeconds * 20.0F));
      this.applySourceAppearance();
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         BedrockXpOrbDefinition.Vec3 acceleration = this.source.sparkAcceleration(this.frame);
         this.xd = this.xd + acceleration.x() / 400.0F;
         this.yd = this.yd + acceleration.y() / 400.0F;
         this.zd = this.zd + acceleration.z() / 400.0F;
         this.applySourceAppearance();
         this.setSpriteFromAge(this.spriteProvider);
      }
   }

   private void applySourceAppearance() {
      float ageSeconds = this.age / 20.0F;
      this.quadSize = this.source.sparkScale(this.frame, ageSeconds, this.lifeSeconds);
      BedrockXpOrbDefinition.Vec3 colour = this.source.sparkColour(this.frame, ageSeconds, this.lifeSeconds);
      this.setColor(colour.x(), colour.y(), colour.z());
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteProvider;

      public Factory(SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType parameters, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return !RunParticlesConfig.get().xpParticles ? null : new XpGlowParticle(world, x, y, z, xd, yd, 0.0, (float)zd, this.spriteProvider, random);
      }
   }
}
