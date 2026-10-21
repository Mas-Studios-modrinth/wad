package net.orevilleport.runparticlesonly;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class HealingRingParticle extends SingleQuadParticle {
   private static final float SCALE = 0.75F;
   private static final int MAX_AGE = 10;

   private HealingRingParticle(ClientLevel world, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteProvider, RandomSource random) {
      super(world, x, y, z, xd, yd, zd, spriteProvider.get(random));
      this.xd = xd;
      this.yd = yd;
      this.zd = zd;
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.lifetime = 10;
      this.quadSize = 0.75F;
      this.updateColor(0.0F);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.oRoll = this.roll;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.updateColor((float)this.age / this.lifetime);
         this.move(this.xd, this.yd, this.zd);
      }
   }

   public void extract(QuadParticleRenderState state, Camera camera, float tickProgress) {
      Vec3 cameraPosition = camera.position();
      float renderX = (float)(Mth.lerp(tickProgress, this.xo, this.x) - cameraPosition.x);
      float renderY = (float)(Mth.lerp(tickProgress, this.yo, this.y) - cameraPosition.y);
      float renderZ = (float)(Mth.lerp(tickProgress, this.zo, this.z) - cameraPosition.z);
      this.extractRotatedQuad(state, new Quaternionf().rotateX((float) (-Math.PI / 2)), renderX, renderY, renderZ, tickProgress);
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public int getLightCoords(float tint) {
      return 15728880;
   }

   private void updateColor(float progress) {
      if (progress < 0.5F) {
         this.alpha = Mth.lerp(progress / 0.5F, 0.0F, 1.0F);
         this.rCol = Mth.lerp(progress / 0.5F, 1.0F, 0.7411765F);
         this.gCol = Mth.lerp(progress / 0.5F, 1.0F, 0.8666667F);
         this.bCol = 1.0F;
      } else {
         float out = Mth.clamp((progress - 0.5F) / 0.5F, 0.0F, 1.0F);
         this.alpha = 1.0F - out;
         this.rCol = Mth.lerp(out, 0.7411765F, 0.11764706F);
         this.gCol = Mth.lerp(out, 0.8666667F, 0.5294118F);
         this.bCol = 1.0F;
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
         return !RunParticlesConfig.get().healingPotionRings ? null : new HealingRingParticle(world, x, y, z, xd, yd, zd, this.spriteProvider, random);
      }
   }
}
