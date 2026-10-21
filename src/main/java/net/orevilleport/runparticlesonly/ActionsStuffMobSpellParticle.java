package net.orevilleport.runparticlesonly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class ActionsStuffMobSpellParticle extends SingleQuadParticle {
   private static final float TICKS_PER_SECOND = 20.0F;
   private static final float LIFETIME_SECONDS = 1.25F;
   private static final float LINEAR_ACCELERATION_Y = 0.5F;
   private static final float LINEAR_DRAG_COEFFICIENT = 2.0F;
   private static final float INITIAL_SIZE = 0.25F;
   private static final float[] SIZE_CURVE = new float[]{0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.85F, 0.25F, 0.0F};
   private static final float DRAG_PER_TICK = (float)Math.exp(-0.1F);
   private static final double LOCAL_STATUS_HORIZONTAL_RADIUS = 0.75;
   private final SpriteSet spriteProvider;
   private final float effectRed;
   private final float effectGreen;
   private final float effectBlue;
   private final float effectAlpha;
   private final boolean localPlayerColumn;
   private final double localPlayerOffsetX;
   private final double localPlayerOffsetZ;
   private static SpriteSet actionsStuffSpriteSet;

   private ActionsStuffMobSpellParticle(
      ClientLevel world, double x, double y, double z, ColorParticleOption effect, SpriteSet spriteProvider, RandomSource random
   ) {
      super(world, x, y, z, 0.0, random.nextFloat() / 20.0F, 0.0, spriteProvider.get(random));
      this.spriteProvider = spriteProvider;
      this.effectRed = effect.getRed();
      this.effectGreen = effect.getGreen();
      this.effectBlue = effect.getBlue();
      this.effectAlpha = effect.getAlpha();
      this.localPlayerColumn = isLocalPlayerStatus(x, z);
      LocalPlayer player = Minecraft.getInstance().player;
      this.localPlayerOffsetX = this.localPlayerColumn && player != null ? x - player.getX() : 0.0;
      this.localPlayerOffsetZ = this.localPlayerColumn && player != null ? z - player.getZ() : 0.0;
      this.gravity = 0.0F;
      this.friction = 1.0F;
      this.hasPhysics = false;
      this.lifetime = Math.round(25.0F);
      this.applySourceAppearance(0.0F);
   }

   public void tick() {
      this.lockToLocalPlayerColumn();
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.oRoll = this.roll;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.yd += 0.00125F;
         this.move(this.xd, this.yd, this.zd);
         this.lockToLocalPlayerColumn();
         this.xd = this.xd * DRAG_PER_TICK;
         this.yd = this.yd * DRAG_PER_TICK;
         this.zd = this.zd * DRAG_PER_TICK;
         this.applySourceAppearance(this.age / 20.0F);
         this.setSpriteFromAge(this.spriteProvider);
      }
   }

   public float getQuadSize(float tickProgress) {
      float progress = Mth.clamp((this.age + tickProgress) / this.lifetime, 0.0F, 1.0F);
      return 0.25F * linearCurve(progress);
   }

   public Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   protected int getLightCoords(float tint) {
      return 15728880;
   }

   private void applySourceAppearance(float ageSeconds) {
      this.setColor(
         Math.max(0.0F, Mth.lerp(ageSeconds, 1.0F, this.effectRed)),
         Math.max(0.0F, Mth.lerp(ageSeconds, 1.0F, this.effectGreen)),
         Math.max(0.0F, Mth.lerp(ageSeconds, 1.0F, this.effectBlue))
      );
      this.setAlpha(Math.max(0.0F, 1.0F - ageSeconds * 0.5F) * this.effectAlpha);
   }

   private static float linearCurve(float progress) {
      float position = progress * (SIZE_CURVE.length - 1);
      int lower = Math.min((int)position, SIZE_CURVE.length - 1);
      int upper = Math.min(lower + 1, SIZE_CURVE.length - 1);
      return Mth.lerp(position - lower, SIZE_CURVE[lower], SIZE_CURVE[upper]);
   }

   private static double horizontalDistanceSquared(LocalPlayer player, double x, double z) {
      double dx = player.getX() - x;
      double dz = player.getZ() - z;
      return dx * dx + dz * dz;
   }

   private static boolean isLocalPlayerStatus(double x, double z) {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && horizontalDistanceSquared(player, x, z) <= 0.5625;
   }

   private void lockToLocalPlayerColumn() {
      if (this.localPlayerColumn) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            this.x = player.getX() + this.localPlayerOffsetX;
            this.z = player.getZ() + this.localPlayerOffsetZ;
         }
      }
   }

   public static final class Factory implements ParticleProvider<ColorParticleOption> {
      private final SpriteSet spriteProvider;

      public Factory(SpriteSet spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         ColorParticleOption effect, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         if (ActionsStuffMobSpellParticle.isLocalPlayerStatus(x, z) && xd == 1.0 && yd == 1.0 && zd == 1.0) {
            SpriteSet sourceProvider = ActionsStuffMobSpellParticle.actionsStuffSpriteSet != null
               ? ActionsStuffMobSpellParticle.actionsStuffSpriteSet
               : this.spriteProvider;
            return new ActionsStuffMobSpellParticle(world, x, y, z, effect, sourceProvider, random);
         } else {
            return null;
         }
      }
   }

   public static final class SourceSpriteFactory implements ParticleProvider<SimpleParticleType> {
      public SourceSpriteFactory(SpriteSet spriteProvider) {
         ActionsStuffMobSpellParticle.actionsStuffSpriteSet = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType effect, ClientLevel world, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return null;
      }
   }
}
