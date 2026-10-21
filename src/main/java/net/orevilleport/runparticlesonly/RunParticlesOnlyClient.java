package net.orevilleport.runparticlesonly;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.AfterTranslucentFeatures;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class RunParticlesOnlyClient implements ClientModInitializer {
   public static final String MOD_ID = "run_particles_only";
   public static final SimpleParticleType RUN_DUST = FabricParticleTypes.simple();
   public static final SimpleParticleType RUN_DUST_MEDIUM = FabricParticleTypes.simple();
   public static final SimpleParticleType LANDING_DUST = FabricParticleTypes.simple();
   public static final SimpleParticleType LANDING_ROLL_RING = FabricParticleTypes.simple();
   public static final SimpleParticleType BOW_CHARGING = FabricParticleTypes.simple();
   public static final SimpleParticleType ARROW_CHARGE = FabricParticleTypes.simple();
   public static final SimpleParticleType NORMAL_HIT = FabricParticleTypes.simple();
   public static final SimpleParticleType PLAYER_HURT = FabricParticleTypes.simple();
   public static final SimpleParticleType HEALING_RING = FabricParticleTypes.simple();
   public static final SimpleParticleType POTION_SPARKLE = FabricParticleTypes.simple();
   public static final SimpleParticleType STATUS_EFFECT_SPRITE = FabricParticleTypes.simple();
   public static final ParticleType<ColorParticleOption> SPLASH_SPELL = tintedParticleType();
   public static final ParticleType<ColorParticleOption> ARROW_TRAIL = tintedParticleType();
   private static final Category KEY_CATEGORY = Category.register(Identifier.fromNamespaceAndPath("run_particles_only", "general"));
   public static final KeyMapping OPEN_CONFIG_KEY = KeyMappingHelper.registerKeyMapping(
      new KeyMapping("key.run_particles_only.open_config", Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), KEY_CATEGORY)
   );
   private static final int ACTIONS_STUFF_NORMAL_HIT_PARTICLE_COUNT = 6;
   private static final int ACTIONS_STUFF_SPLASH_SPELL_PARTICLE_COUNT = 18;
   private static final double ACTIONS_STUFF_SPLASH_SPELL_INITIAL_SPEED = 0.2;
   private static final int ACTIONS_STUFF_ARROW_TRAIL_BLUE = 7985151;
   private static final int ACTIONS_STUFF_ELYTRA_TRAIL_COLOUR = 13100799;
   private static final double ACTIONS_STUFF_HEALING_RING_SPAWN_RATE = 0.275;
   private static final double ACTIONS_STUFF_POTION_SPARKLE_SPAWN_RATE = 1.35;
   private static final double ACTIONS_STUFF_HEALING_RING_INITIAL_SPEED = 0.175;
   private static final double ACTIONS_STUFF_HEALING_RING_LOCAL_Y_OFFSET = -1.55;
   private static final double ACTIONS_STUFF_POTION_SPARKLE_LOCAL_Y_OFFSET = -1.2;
   private static final int ACTIONS_STUFF_POTION_EFFECT_EVENT_TICK = Math.max(1, Math.round(17.5F));
   private static final int ACTIONS_STUFF_ARROW_CHARGE_EVENT_TICK = Math.round(15.834F);
   private static final double ACTIONS_STUFF_BOW_CHARGING_SPAWN_RATE = 1.1;
   private static final double ACTIONS_STUFF_BOW_CHARGING_RADIUS = 1.0;
   private static final double ACTIONS_STUFF_BOW_CHARGING_INITIAL_SPEED = 0.15;
   private static final double ACTIONS_STUFF_BOW_LEAD_FORWARD_OFFSET = 0.52;
   private static final double ACTIONS_STUFF_BOW_LEAD_Y_OFFSET = 0.98;
   private static final double ACTIONS_STUFF_BOW_LEAD_SIDE_OFFSET = 0.08;
   private static final double ACTIONS_STUFF_BOW_LEAD_PITCH_Y_OFFSET = 0.18;
   private static int sprintDustCooldown;
   private static int elytraTrailCooldown;
   private static int previousPlayerHurtTime;
   private static int bowChargeTicks;
   private static double bowChargingEmitterRemainder;
   private static boolean arrowChargeSpawned;
   private static boolean wasDrinkingPotion;
   private static int potionUseTicks;
   private static boolean potionRingOriginSet;
   private static double potionRingOriginX;
   private static double potionRingOriginY;
   private static double potionRingOriginZ;
   private static double healingRingEmitterRemainder;
   private static double potionSparkleEmitterRemainder;
   private static final Map<Integer, RunParticlesOnlyClient.SplashPotionSnapshot> trackedSplashPotions = new HashMap<>();

   public void onInitializeClient() {
      RunParticlesConfig.load();
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("run_dust"), RUN_DUST);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("run_dust_medium"), RUN_DUST_MEDIUM);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("landing_dust"), LANDING_DUST);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("landing_roll_ring"), LANDING_ROLL_RING);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("bow_charging"), BOW_CHARGING);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("arrow_charge"), ARROW_CHARGE);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("normal_hit"), NORMAL_HIT);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("player_hurt"), PLAYER_HURT);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("healing_ring"), HEALING_RING);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("potion_sparkle"), POTION_SPARKLE);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("status_effect_sprite"), STATUS_EFFECT_SPRITE);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("splash_spell"), SPLASH_SPELL);
      Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("arrow_trail"), ARROW_TRAIL);
      ParticleProviderRegistry.getInstance().register(RUN_DUST, RunDustParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(RUN_DUST_MEDIUM, RunDustMediumParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(LANDING_DUST, LandingDustParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(LANDING_ROLL_RING, LandingRollRingParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(BOW_CHARGING, BowChargingParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(ARROW_CHARGE, ArrowChargeParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(NORMAL_HIT, CriticalHitParticle.WhiteFactory::new);
      ParticleProviderRegistry.getInstance().register(PLAYER_HURT, PlayerHurtParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(HEALING_RING, HealingRingParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(POTION_SPARKLE, PotionSparkleParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(STATUS_EFFECT_SPRITE, ActionsStuffMobSpellParticle.SourceSpriteFactory::new);
      ParticleProviderRegistry.getInstance().register(SPLASH_SPELL, SplashSpellParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(ARROW_TRAIL, ArrowTrailParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(ParticleTypes.CRIT, CriticalHitParticle.Factory::new);
      ParticleProviderRegistry.getInstance().register(ParticleTypes.ENTITY_EFFECT, ActionsStuffMobSpellParticle.Factory::new);
      ClientTickEvents.END_CLIENT_TICK.register(RunParticlesOnlyClient::tick);
      LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register((AfterTranslucentFeatures)context -> {
         XpOrbEffects.renderTrails(context);
         XpBottleEffects.renderTrails(context);
         ToolTrailEffects.render(context);
      });
      AttackEntityCallback.EVENT.register(RunParticlesOnlyClient::onAttackEntity);
   }

   private static void tick(Minecraft client) {
      while (OPEN_CONFIG_KEY.consumeClick()) {
         if (client.gui.screen() == null) {
            client.gui.setScreen(new RunParticlesConfigScreen(null));
         }
      }

      LocalPlayer player = client.player;
      if (player != null && client.level != null) {
         RunParticlesConfig config = RunParticlesConfig.get();
         Vec3 velocity = player.getDeltaMovement();
         tickPlayerDamageParticles(player, config);
         if (config.landingDustParticles) {
            ActionsStuffLandingSystem.tick(player);
         } else {
            ActionsStuffLandingSystem.reset(player.onGround());
         }

         if (config.sprintDustParticles) {
            tickSprintDust(player, velocity);
         } else {
            sprintDustCooldown = 0;
         }

         if (!config.bowChargingParticles && !config.arrowChargeParticles) {
            resetBowCharge();
         } else {
            tickBowCharge(client, player);
         }

         tickPotionHealing(player);
         tickSplashPotions(client.level, player);
         tickArrowTrails(client.level, player);
         tickElytraTrail(player, velocity);
         XpOrbEffects.tick(client.level, player);
         XpBottleEffects.tick(client.level, player);
         ToolTrailEffects.tick(player);
      } else {
         resetTransientState();
      }
   }

   private static void tickPlayerDamageParticles(LocalPlayer player, RunParticlesConfig config) {
      int hurtTime = player.hurtTime;
      if (hurtTime > 0 && previousPlayerHurtTime == 0 && config.playerHurtParticles) {
         ThreadLocalRandom random = ThreadLocalRandom.current();

         for (int i = 0; i < 4; i++) {
            double upward = random.nextDouble();
            double speed = (6.0 + upward * 10.0) / 20.0;
            double xDirection = (random.nextDouble() - 0.5) * 0.5;
            double zDirection = (random.nextDouble() - 0.5) * 0.5;
            player.level().addParticle(PLAYER_HURT, player.getX(), player.getY(), player.getZ(), xDirection * speed, upward * speed, zDirection * speed);
         }
      }

      previousPlayerHurtTime = hurtTime;
   }

   private static void tickElytraTrail(LocalPlayer player, Vec3 velocity) {
      if (RunParticlesConfig.get().elytraTrailParticles && player.isFallFlying()) {
         double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
         if (!(horizontalSpeed < 0.12)) {
            if (--elytraTrailCooldown <= 0) {
               elytraTrailCooldown = horizontalSpeed > 1.35 ? 0 : 1;
               double inverseSpeed = 1.0 / horizontalSpeed;
               double behindX = -velocity.x * inverseSpeed;
               double behindZ = -velocity.z * inverseSpeed;
               double sideX = -behindZ;
               double sideZ = behindX;
               ThreadLocalRandom random = ThreadLocalRandom.current();

               for (int wing = -1; wing <= 1; wing += 2) {
                  double side = wing * 0.32 + random.nextDouble(-0.06, 0.06);
                  double x = player.getX() + behindX * 0.3 + sideX * side;
                  double y = player.getY() + 0.58 + random.nextDouble(-0.08, 0.08);
                  double z = player.getZ() + behindZ * 0.3 + sideZ * side;
                  player.level().addParticle(ColorParticleOption.create(ARROW_TRAIL, 13100799), x, y, z, behindX * 0.018, -0.006, behindZ * 0.018);
               }
            }
         }
      } else {
         elytraTrailCooldown = 0;
      }
   }

   private static void tickSprintDust(LocalPlayer player, Vec3 velocity) {
      double horizontalSpeedSquared = velocity.x * velocity.x + velocity.z * velocity.z;
      boolean runningOnGround = player.onGround() && player.isSprinting() && !player.isSwimming() && horizontalSpeedSquared > 0.004;
      if (!runningOnGround) {
         sprintDustCooldown = 0;
      } else {
         sprintDustCooldown--;
         if (sprintDustCooldown <= 0) {
            sprintDustCooldown = RunParticlesConfig.get().runDustSpawnMultiplier() > 1.5F ? 1 : 2;
            spawnRunDust(player, velocity);
         }
      }
   }

   private static void spawnRunDust(LocalPlayer player, Vec3 playerVelocity) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      double horizontalSpeed = Math.sqrt(playerVelocity.x * playerVelocity.x + playerVelocity.z * playerVelocity.z) * 20.0;
      double yaw = Math.toRadians(player.getYRot());
      double backX = Math.sin(yaw);
      double backZ = -Math.cos(yaw);
      double rightX = Math.cos(yaw);
      double rightZ = Math.sin(yaw);
      int count = Math.max(1, Math.round((random.nextBoolean() ? 2 : 3) * RunParticlesConfig.get().runDustSpawnMultiplier()));

      for (int i = 0; i < count; i++) {
         double back = random.nextDouble(0.16, 0.42);
         double side = random.nextDouble(-0.34, 0.34);
         double x = player.getX() + backX * back + rightX * side;
         double z = player.getZ() + backZ * back + rightZ * side;
         double fromCenterX = x - player.getX();
         double fromCenterZ = z - player.getZ();
         double length = Math.max(0.001, Math.sqrt(fromCenterX * fromCenterX + fromCenterZ * fromCenterZ));
         double spread = random.nextDouble(0.035, 0.085);
         double xd = fromCenterX / length * spread - playerVelocity.x * 0.08 + random.nextDouble(-0.014, 0.014);
         double zd = fromCenterZ / length * spread - playerVelocity.z * 0.08 + random.nextDouble(-0.014, 0.014);
         SimpleParticleType particle = horizontalSpeed > 4.2 && random.nextFloat() < 0.42F ? RUN_DUST_MEDIUM : RUN_DUST;
         player.level().addParticle(particle, x, player.getY() + 0.035, z, xd, random.nextDouble(0.018, 0.045), zd);
      }
   }

   private static void tickBowCharge(Minecraft client, LocalPlayer player) {
      if (client.gui.screen() == null && isChargingBow(player)) {
         int useTime = Math.max(player.getTicksUsingItem(), bowChargeTicks + 1);
         bowChargeTicks = useTime;
         if (RunParticlesConfig.get().bowChargingParticles) {
            spawnBowChargingParticles(player);
         } else {
            bowChargingEmitterRemainder = 0.0;
         }

         if (RunParticlesConfig.get().arrowChargeParticles && !arrowChargeSpawned && useTime >= ACTIONS_STUFF_ARROW_CHARGE_EVENT_TICK) {
            arrowChargeSpawned = true;
            spawnArrowChargeParticle(player);
         }
      } else {
         resetBowCharge();
      }
   }

   private static boolean isChargingBow(LocalPlayer player) {
      if (!player.isUsingItem()) {
         return false;
      }

      ItemStack activeItem = player.getActiveItem();
      return !activeItem.isEmpty() && activeItem.is(Items.BOW);
   }

   private static void resetBowCharge() {
      bowChargeTicks = 0;
      bowChargingEmitterRemainder = 0.0;
      arrowChargeSpawned = false;
   }

   private static void resetTransientState() {
      sprintDustCooldown = 0;
      elytraTrailCooldown = 0;
      previousPlayerHurtTime = 0;
      ActionsStuffLandingSystem.reset(true);
      resetBowCharge();
      wasDrinkingPotion = false;
      potionUseTicks = 0;
      potionRingOriginSet = false;
      healingRingEmitterRemainder = 0.0;
      potionSparkleEmitterRemainder = 0.0;
      trackedSplashPotions.clear();
      XpOrbEffects.clear();
      XpBottleEffects.clear();
      ToolTrailEffects.clear();
   }

   private static InteractionResult onAttackEntity(Player player, Level world, InteractionHand hand, Entity target, EntityHitResult hitResult) {
      if (world.isClientSide() && hand == InteractionHand.MAIN_HAND && world instanceof ClientLevel clientWorld) {
         if (RunParticlesConfig.get().normalHitParticles && !isCriticalHit(player, target)) {
            spawnNormalHitParticles(clientWorld, target);
            return InteractionResult.PASS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private static boolean isCriticalHit(Player player, Entity target) {
      return target instanceof LivingEntity
         && player.fallDistance > 0.0
         && !player.onGround()
         && !player.onClimbable()
         && !player.isInWater()
         && !player.hasEffect(MobEffects.BLINDNESS)
         && !player.isPassenger()
         && !player.isSprinting();
   }

   private static void spawnNormalHitParticles(ClientLevel world, Entity target) {
      ThreadLocalRandom random = ThreadLocalRandom.current();

      for (int i = 0; i < 6; i++) {
         Vec3 direction = randomUnitSphere(random);
         world.addParticle(
            NORMAL_HIT,
            target.getX() + direction.x / 4.0,
            target.getY() + target.getBbHeight() * (0.5 + direction.y / 4.0),
            target.getZ() + direction.z / 4.0,
            direction.x,
            direction.y + 0.2,
            direction.z
         );
      }
   }

   private static void tickPotionHealing(LocalPlayer player) {
      RunParticlesConfig config = RunParticlesConfig.get();
      boolean drinkingPotion = player.isUsingItem() && player.getActiveItem().is(Items.POTION);
      if (drinkingPotion && config.healingPotionRings) {
         potionUseTicks++;
         if (!wasDrinkingPotion || potionUseTicks == ACTIONS_STUFF_POTION_EFFECT_EVENT_TICK) {
            healingRingEmitterRemainder = Math.max(healingRingEmitterRemainder, 1.0);
         }

         if (potionUseTicks >= ACTIONS_STUFF_POTION_EFFECT_EVENT_TICK) {
            tickPotionUseEffects(player);
         }
      } else {
         potionUseTicks = 0;
         potionRingOriginSet = false;
         healingRingEmitterRemainder = 0.0;
         potionSparkleEmitterRemainder = 0.0;
      }

      wasDrinkingPotion = drinkingPotion;
   }

   private static void tickPotionUseEffects(LocalPlayer player) {
      ensurePotionRingOrigin(player);
      healingRingEmitterRemainder += 0.275;
      int rings = (int)healingRingEmitterRemainder;
      healingRingEmitterRemainder -= rings;

      for (int i = 0; i < rings; i++) {
         spawnPotionRing(player);
      }

      potionSparkleEmitterRemainder += 1.35;
      int sparkles = (int)potionSparkleEmitterRemainder;
      potionSparkleEmitterRemainder -= sparkles;
      ThreadLocalRandom random = ThreadLocalRandom.current();

      for (int i = 0; i < sparkles; i++) {
         double x = player.getX() + random.nextDouble(-0.4, 0.4);
         double y = player.getEyeY() + -1.2 + random.nextDouble(-0.2, 0.2);
         double z = player.getZ() + random.nextDouble(-0.4, 0.4);
         player.level().addParticle(POTION_SPARKLE, x, y, z, random.nextDouble(-0.025, 0.025), 0.15, random.nextDouble(-0.025, 0.025));
      }
   }

   private static void spawnPotionRing(LocalPlayer player) {
      ensurePotionRingOrigin(player);
      player.level().addParticle(HEALING_RING, potionRingOriginX, potionRingOriginY, potionRingOriginZ, 0.0, 0.175, 0.0);
   }

   private static void ensurePotionRingOrigin(LocalPlayer player) {
      if (!potionRingOriginSet) {
         potionRingOriginSet = true;
         potionRingOriginX = player.getX();
         potionRingOriginY = player.getEyeY() + -1.55;
         potionRingOriginZ = player.getZ();
      }
   }

   private static void tickSplashPotions(ClientLevel world, LocalPlayer player) {
      Set<Integer> seen = new HashSet<>();

      for (Entity entity : world.entitiesForRendering()) {
         if (entity instanceof AbstractThrownPotion potion && (potion instanceof ThrownSplashPotion || potion instanceof ThrownLingeringPotion)) {
            int id = potion.getId();
            seen.add(id);
            trackedSplashPotions.put(
               id, new RunParticlesOnlyClient.SplashPotionSnapshot(potion.getX(), potion.getY(), potion.getZ(), potionColor(potion.getItem()))
            );
         }
      }

      Iterator<Entry<Integer, RunParticlesOnlyClient.SplashPotionSnapshot>> iterator = trackedSplashPotions.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<Integer, RunParticlesOnlyClient.SplashPotionSnapshot> entry = iterator.next();
         if (!seen.contains(entry.getKey())) {
            RunParticlesOnlyClient.SplashPotionSnapshot snapshot = entry.getValue();
            if (RunParticlesConfig.get().splashPotionLandingParticles) {
               spawnSplashPotionBurst(world, snapshot);
            }

            iterator.remove();
         }
      }
   }

   private static int potionColor(ItemStack stack) {
      PotionContents contents = (PotionContents)stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      return contents.getColorOr(-13083194);
   }

   private static void spawnSplashPotionBurst(ClientLevel world, RunParticlesOnlyClient.SplashPotionSnapshot snapshot) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      ColorParticleOption effect = ColorParticleOption.create(SPLASH_SPELL, snapshot.color);

      for (int i = 0; i < 18; i++) {
         boolean strongVertical = random.nextDouble() > 0.7;
         Vec3 direction = new Vec3(
            (strongVertical ? 0.35 : 1.0) * random.nextDouble(-1.0, 1.0),
            (strongVertical ? 4.0 : 0.25) * random.nextDouble(-1.0, 1.0),
            (strongVertical ? 0.35 : 1.0) * random.nextDouble(-1.0, 1.0)
         );
         if (direction.lengthSqr() < 1.0E-6) {
            direction = new Vec3(0.0, 1.0, 0.0);
         } else {
            direction = direction.normalize();
         }

         Vec3 velocity = direction.scale(0.2);
         world.addParticle(effect, snapshot.x, snapshot.y, snapshot.z, velocity.x, velocity.y, velocity.z);
      }
   }

   private static void tickArrowTrails(ClientLevel world, LocalPlayer player) {
      if (RunParticlesConfig.get().arrowTrailParticles) {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         ColorParticleOption effect = ColorParticleOption.create(ARROW_TRAIL, 7985151);

         for (Entity entity : world.entitiesForRendering()) {
            if (entity instanceof Arrow arrow && arrow.getOwner() == player && arrow.tickCount >= 2) {
               Vec3 arrowVelocity = arrow.getDeltaMovement();
               if (!(arrowVelocity.lengthSqr() < 0.0025)) {
                  Vec3 direction = arrowVelocity.normalize();
                  double trailBack = random.nextDouble(0.12, 0.36);
                  world.addParticle(
                     effect,
                     arrow.getX() - direction.x * trailBack + random.nextDouble(-0.025, 0.025),
                     arrow.getY() - direction.y * trailBack + random.nextDouble(-0.025, 0.025),
                     arrow.getZ() - direction.z * trailBack + random.nextDouble(-0.025, 0.025),
                     -direction.x * random.nextDouble(0.01, 0.035) + random.nextDouble(-0.01, 0.01),
                     -direction.y * random.nextDouble(0.01, 0.035) + random.nextDouble(-0.01, 0.01),
                     -direction.z * random.nextDouble(0.01, 0.035) + random.nextDouble(-0.01, 0.01)
                  );
               }
            }
         }
      }
   }

   private static void spawnBowChargingParticles(LocalPlayer player) {
      bowChargingEmitterRemainder += 1.1;
      int particles = (int)bowChargingEmitterRemainder;
      bowChargingEmitterRemainder -= particles;
      ThreadLocalRandom random = ThreadLocalRandom.current();
      RunParticlesOnlyClient.BowEmitterFrame frame = RunParticlesOnlyClient.BowEmitterFrame.of(player, 0.16, 0.2, -0.06);

      for (int i = 0; i < particles; i++) {
         Vec3 local = randomSphereSurface(random).scale(1.0);
         Vec3 worldOffset = frame.localToWorld(local);
         Vec3 position = frame.origin.add(worldOffset);
         Vec3 velocity = worldOffset.normalize().scale(-0.15);
         player.level().addParticle(BOW_CHARGING, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
      }
   }

   private static void spawnArrowChargeParticle(LocalPlayer player) {
      RunParticlesOnlyClient.BowEmitterFrame frame = RunParticlesOnlyClient.BowEmitterFrame.of(player, 0.25, 0.35, -0.06);
      Vec3 position = frame.origin;
      player.level().addParticle(ARROW_CHARGE, position.x, position.y, position.z, 0.0, 0.0, 0.0);
   }

   private static Vec3 randomSphereSurface(ThreadLocalRandom random) {
      double y = random.nextDouble(-1.0, 1.0);
      double angle = random.nextDouble(0.0, Math.PI * 2);
      double radius = Math.sqrt(Math.max(0.0, 1.0 - y * y));
      return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
   }

   private static Vec3 randomUnitSphere(ThreadLocalRandom random) {
      for (int i = 0; i < 8; i++) {
         Vec3 vector = new Vec3(random.nextDouble(-1.0, 1.0), random.nextDouble(-1.0, 1.0), random.nextDouble(-1.0, 1.0));
         if (vector.lengthSqr() <= 1.0 && vector.lengthSqr() > 1.0E-6) {
            return vector.normalize();
         }
      }

      return randomSphereSurface(random);
   }

   private static ParticleType<ColorParticleOption> tintedParticleType() {
      return new ParticleType<ColorParticleOption>(false) {
         public MapCodec<ColorParticleOption> codec() {
            return ColorParticleOption.codec(this);
         }

         public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
            return ColorParticleOption.streamCodec(this);
         }
      };
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("run_particles_only", path);
   }

   private record BowEmitterFrame(Vec3 origin, Vec3 right, Vec3 up, Vec3 forward) {
      static RunParticlesOnlyClient.BowEmitterFrame of(LocalPlayer player, double offsetX, double offsetY, double offsetZ) {
         double yaw = Math.toRadians(player.getYRot());
         double pitch = Math.toRadians(player.getXRot());
         Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)).normalize();
         Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).normalize();
         Vec3 up = forward.cross(right).normalize();
         double handSide = player.getUsedItemHand() == InteractionHand.OFF_HAND ? 1.0 : -1.0;
         Vec3 leadHold = new Vec3(player.getX(), player.getY() + 0.98, player.getZ())
            .add(forward.scale(0.52))
            .add(right.scale(0.08 * handSide))
            .add(0.0, forward.y * 0.18, 0.0);
         Vec3 origin = leadHold.add(right.scale(offsetX * handSide)).add(up.scale(offsetY)).add(forward.scale(offsetZ));
         return new RunParticlesOnlyClient.BowEmitterFrame(origin, right.scale(handSide), up, forward);
      }

      Vec3 localToWorld(Vec3 local) {
         return this.right.scale(local.x).add(this.up.scale(local.y)).add(this.forward.scale(local.z));
      }
   }

   private record SplashPotionSnapshot(double x, double y, double z, int color) {
   }
}
