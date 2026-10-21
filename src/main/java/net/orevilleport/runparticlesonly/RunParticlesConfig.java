package net.orevilleport.runparticlesonly;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class RunParticlesConfig {
   public static final int FIRE_ACTIONS_AND_STUFF = 0;
   public static final int FIRE_VANILLA = 1;
   public static final int FIRE_OFF = 2;
   private static final int FIRE_MODE_COUNT = 3;
   public static final int HURT_EFFECT_RED = 0;
   public static final int HURT_EFFECT_WHITE = 1;
   public static final int HURT_EFFECT_OFF = 2;
   private static final int HURT_EFFECT_COUNT = 3;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("run-particles-only.json");
   private static final int[] RUN_DUST_COLORS = new int[]{15913369, 16777215, 11053224, 7985151, 9306010, 16747898};
   private static final float[] RUN_DUST_SPAWN_MULTIPLIERS = new float[]{1.0F, 0.55F, 1.6F, 2.25F};
   private static final float[] RUN_DUST_SCALE_MULTIPLIERS = new float[]{1.0F, 0.82F, 1.16F, 1.34F};
   private static final float[] RUN_DUST_ALPHA_MULTIPLIERS = new float[]{1.0F, 0.72F, 1.14F, 1.28F};
   private static RunParticlesConfig instance = new RunParticlesConfig();
   public boolean sprintDustParticles = true;
   public int runDustColorMode = 0;
   public int runDustIntensity = 0;
   public boolean landingDustParticles = true;
   public boolean bowChargingParticles = true;
   public boolean arrowChargeParticles = true;
   public boolean criticalHitParticles = true;
   public boolean normalHitParticles = true;
   public boolean playerHurtParticles = true;
   public boolean healingPotionRings = true;
   public boolean splashPotionLandingParticles = true;
   public boolean arrowTrailParticles = true;
   public boolean elytraTrailParticles = true;
   public boolean xpParticles = true;
   public boolean xpTrailParticles = true;
   public boolean throwSpin = true;
   public boolean toolTrailParticles = true;
   public int fireMode = 0;
   public int playerHurtEffect = 1;
   public int mobHurtEffect = 0;

   private RunParticlesConfig() {
   }

   public static RunParticlesConfig get() {
      return instance;
   }

   public static void load() {
      if (!Files.exists(CONFIG_PATH)) {
         save();
      } else {
         try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            RunParticlesConfig loaded = (RunParticlesConfig)GSON.fromJson(reader, RunParticlesConfig.class);
            if (loaded != null) {
               instance = loaded;
               instance.sanitize();
            }
         } catch (IOException | RuntimeException ignored) {
            instance = new RunParticlesConfig();
         }

         save();
      }
   }

   public static void save() {
      try {
         Files.createDirectories(CONFIG_PATH.getParent());

         try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, writer);
         }
      } catch (IOException var5) {
      }
   }

   public void cycleRunDustColorMode() {
      this.runDustColorMode = (safeIndex(this.runDustColorMode, RUN_DUST_COLORS.length) + 1) % RUN_DUST_COLORS.length;
   }

   public void cycleRunDustIntensity() {
      this.runDustIntensity = (safeIndex(this.runDustIntensity, RUN_DUST_SPAWN_MULTIPLIERS.length) + 1) % RUN_DUST_SPAWN_MULTIPLIERS.length;
   }

   public String runDustColorTranslationKey() {
      return "value.run_particles_only.run_dust_color." + safeIndex(this.runDustColorMode, RUN_DUST_COLORS.length);
   }

   public String runDustIntensityTranslationKey() {
      return "value.run_particles_only.run_dust_intensity." + safeIndex(this.runDustIntensity, RUN_DUST_SPAWN_MULTIPLIERS.length);
   }

   public float runDustRed() {
      return (this.runDustColor() >> 16 & 0xFF) / 255.0F;
   }

   public float runDustGreen() {
      return (this.runDustColor() >> 8 & 0xFF) / 255.0F;
   }

   public float runDustBlue() {
      return (this.runDustColor() & 0xFF) / 255.0F;
   }

   public float runDustSpawnMultiplier() {
      return RUN_DUST_SPAWN_MULTIPLIERS[safeIndex(this.runDustIntensity, RUN_DUST_SPAWN_MULTIPLIERS.length)];
   }

   public float runDustScaleMultiplier() {
      return RUN_DUST_SCALE_MULTIPLIERS[safeIndex(this.runDustIntensity, RUN_DUST_SCALE_MULTIPLIERS.length)];
   }

   public float runDustAlphaMultiplier() {
      return RUN_DUST_ALPHA_MULTIPLIERS[safeIndex(this.runDustIntensity, RUN_DUST_ALPHA_MULTIPLIERS.length)];
   }

   public void cyclePlayerHurtEffect() {
      this.playerHurtEffect = (safeIndex(this.playerHurtEffect, 3) + 1) % 3;
   }

   public void cycleMobHurtEffect() {
      this.mobHurtEffect = (safeIndex(this.mobHurtEffect, 3) + 1) % 3;
   }

   public void cycleFireMode() {
      this.fireMode = (safeIndex(this.fireMode, 3) + 1) % 3;
   }

   public String fireModeTranslationKey() {
      return "value.run_particles_only.fire_mode." + safeIndex(this.fireMode, 3);
   }

   public static String fireModeName() {
      return switch (safeIndex(get().fireMode, 3)) {
         case 1 -> "VANILLA";
         case 2 -> "OFF";
         default -> "A&S";
      };
   }

   public String playerHurtEffectTranslationKey() {
      return hurtEffectTranslationKey(this.playerHurtEffect);
   }

   public String mobHurtEffectTranslationKey() {
      return hurtEffectTranslationKey(this.mobHurtEffect);
   }

   public static String hurtEffectTranslationKey(int effect) {
      return "value.run_particles_only.hurt_effect." + safeIndex(effect, 3);
   }

   private int runDustColor() {
      return RUN_DUST_COLORS[safeIndex(this.runDustColorMode, RUN_DUST_COLORS.length)];
   }

   private void sanitize() {
      this.runDustColorMode = safeIndex(this.runDustColorMode, RUN_DUST_COLORS.length);
      this.runDustIntensity = safeIndex(this.runDustIntensity, RUN_DUST_SPAWN_MULTIPLIERS.length);
      this.playerHurtEffect = safeIndex(this.playerHurtEffect, 3);
      this.mobHurtEffect = safeIndex(this.mobHurtEffect, 3);
      this.fireMode = safeIndex(this.fireMode, 3);
   }

   private static int safeIndex(int index, int length) {
      return index >= 0 && index < length ? index : 0;
   }
}
