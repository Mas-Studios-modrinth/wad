package net.orevilleport.runparticlesonly;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

final class ActionsStuffLandingSystem {
   private static final int PARTICLE_COUNT = 7;
   private static final double DISC_RADIUS = 0.1;
   private static final double INITIAL_SPEED_PER_TICK = 0.35;
   private static final double LOCAL_PLAYER_Y_OFFSET = -1.56;
   private static final double HARD_LANDING_DROP = 3.333;
   private static final int MOVING_LANDING_DELAY_TICKS = Math.max(1, Math.round(1.25F));
   private static final int IDLE_LANDING_DELAY_TICKS = Math.max(1, Math.round(1.666F));
   private static final int HARD_SPRINT_RING_DELAY_TICKS = Math.max(1, Math.round(2.5F));
   private static final double MOVING_SPEED_EPSILON = 0.01;
   private static boolean wasOnGround = true;
   private static double airborneStartY = Double.NaN;
   private static boolean airborneWasSprinting;
   private static int pendingDustTicks = -1;
   private static int pendingHardSprintRingTicks = -1;

   private ActionsStuffLandingSystem() {
   }

   static void tick(LocalPlayer player) {
      boolean onGround = player.onGround();
      if (pendingDustTicks >= 0) {
         if (!onGround) {
            pendingDustTicks = -1;
         } else if (--pendingDustTicks <= 0) {
            spawnSourceDust(player);
            pendingDustTicks = -1;
         }
      }

      if (pendingHardSprintRingTicks >= 0) {
         if (!onGround) {
            pendingHardSprintRingTicks = -1;
         } else if (--pendingHardSprintRingTicks <= 0) {
            spawnHardSprintRing(player);
            pendingHardSprintRingTicks = -1;
         }
      }

      if (!onGround) {
         if (wasOnGround || !Double.isFinite(airborneStartY)) {
            airborneStartY = player.getY();
            airborneWasSprinting = player.isSprinting();
         } else if (player.isSprinting()) {
            airborneWasSprinting = true;
         }
      } else if (!wasOnGround && player.tickCount > 20) {
         double drop = Double.isFinite(airborneStartY) ? airborneStartY - player.getY() : 0.0;
         if (!(drop > 3.333)) {
            Vec3 velocity = player.getDeltaMovement();
            double horizontalSpeed = Math.hypot(velocity.x, velocity.z) * 20.0;
            pendingDustTicks = horizontalSpeed > 0.01 ? MOVING_LANDING_DELAY_TICKS : IDLE_LANDING_DELAY_TICKS;
         } else {
            spawnSourceDust(player);
            pendingHardSprintRingTicks = !airborneWasSprinting && !player.isSprinting() ? -1 : HARD_SPRINT_RING_DELAY_TICKS;
            pendingDustTicks = -1;
         }

         airborneStartY = Double.NaN;
         airborneWasSprinting = false;
      }

      wasOnGround = onGround;
   }

   static void reset(boolean onGround) {
      wasOnGround = onGround;
      airborneStartY = Double.NaN;
      airborneWasSprinting = false;
      pendingDustTicks = -1;
      pendingHardSprintRingTicks = -1;
   }

   private static void spawnSourceDust(LocalPlayer player) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      double centerX = player.getX();
      double centerY = player.getEyeY() + -1.56;
      double centerZ = player.getZ();

      for (int i = 0; i < 7; i++) {
         double angle = random.nextDouble(0.0, Math.PI * 2);
         double radius = Math.sqrt(random.nextDouble()) * 0.1;
         double offsetX = Math.cos(angle) * radius;
         double offsetZ = Math.sin(angle) * radius;
         double length = Math.hypot(offsetX, offsetZ);
         double velocityX = length > 1.0E-12 ? -offsetX / length * 0.35 : 0.0;
         double velocityZ = length > 1.0E-12 ? -offsetZ / length * 0.35 : 0.0;
         player.level().addParticle(RunParticlesOnlyClient.LANDING_DUST, centerX + offsetX, centerY, centerZ + offsetZ, velocityX, 0.0, velocityZ);
      }
   }

   private static void spawnHardSprintRing(LocalPlayer player) {
      player.level().addParticle(RunParticlesOnlyClient.LANDING_ROLL_RING, player.getX(), player.getEyeY() - 1.25, player.getZ(), 0.0, 0.0, 0.0);
   }
}
