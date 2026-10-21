package net.orevilleport.runparticlesonly;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RunParticlesConfigScreen extends Screen {
   private static final int BUTTON_HEIGHT = 20;
   private static final int BUTTON_SPACING = 24;
   private static final int BUTTON_WIDTH = 220;
   private final Screen parent;

   public RunParticlesConfigScreen(Screen parent) {
      super(Component.translatable("screen.run_particles_only.title"));
      this.parent = parent;
   }

   protected void init() {
      RunParticlesConfig config = RunParticlesConfig.get();
      RunParticlesConfigScreen.ButtonEntry[] settings = new RunParticlesConfigScreen.ButtonEntry[]{
         toggle("option.run_particles_only.sprint_dust", () -> config.sprintDustParticles, value -> config.sprintDustParticles = value),
         cycle("option.run_particles_only.run_dust_color", () -> Component.translatable(config.runDustColorTranslationKey()), config::cycleRunDustColorMode),
         cycle(
            "option.run_particles_only.run_dust_intensity",
            () -> Component.translatable(config.runDustIntensityTranslationKey()),
            config::cycleRunDustIntensity
         ),
         toggle("option.run_particles_only.landing_dust", () -> config.landingDustParticles, value -> config.landingDustParticles = value),
         toggle("option.run_particles_only.bow_charging", () -> config.bowChargingParticles, value -> config.bowChargingParticles = value),
         toggle("option.run_particles_only.arrow_charge", () -> config.arrowChargeParticles, value -> config.arrowChargeParticles = value),
         toggle("option.run_particles_only.critical_hit", () -> config.criticalHitParticles, value -> config.criticalHitParticles = value),
         toggle("option.run_particles_only.normal_hit", () -> config.normalHitParticles, value -> config.normalHitParticles = value),
         toggle("option.run_particles_only.player_hurt_particles", () -> config.playerHurtParticles, value -> config.playerHurtParticles = value),
         cycle(
            "option.run_particles_only.player_hurt_effect",
            () -> Component.translatable(config.playerHurtEffectTranslationKey()),
            config::cyclePlayerHurtEffect
         ),
         cycle("option.run_particles_only.mob_hurt_effect", () -> Component.translatable(config.mobHurtEffectTranslationKey()), config::cycleMobHurtEffect),
         cycle("option.run_particles_only.fire", () -> Component.translatable(config.fireModeTranslationKey()), config::cycleFireMode),
         toggle("option.run_particles_only.healing_rings", () -> config.healingPotionRings, value -> config.healingPotionRings = value),
         toggle("option.run_particles_only.splash_potions", () -> config.splashPotionLandingParticles, value -> config.splashPotionLandingParticles = value),
         toggle("option.run_particles_only.arrow_trail", () -> config.arrowTrailParticles, value -> config.arrowTrailParticles = value),
         toggle("option.run_particles_only.elytra_trail", () -> config.elytraTrailParticles, value -> config.elytraTrailParticles = value),
         toggle("option.run_particles_only.xp_particles", () -> config.xpParticles, value -> config.xpParticles = value),
         toggle("option.run_particles_only.xp_trail", () -> config.xpTrailParticles, value -> config.xpTrailParticles = value),
         toggle("option.run_particles_only.throw_spin", () -> config.throwSpin, value -> config.throwSpin = value),
         toggle("option.run_particles_only.tool_trail", () -> config.toolTrailParticles, value -> config.toolTrailParticles = value)
      };
      boolean twoColumns = this.width >= 520;
      int buttonWidth = twoColumns ? 220 : Math.min(220, this.width - 40);
      int leftX = twoColumns ? this.width / 2 - buttonWidth - 8 : (this.width - buttonWidth) / 2;
      int rightX = this.width / 2 + 8;
      int startY = 42;

      for (int i = 0; i < settings.length; i++) {
         RunParticlesConfigScreen.ButtonEntry setting = settings[i];
         int column = twoColumns ? i % 2 : 0;
         int row = twoColumns ? i / 2 : i;
         int x = column == 0 ? leftX : rightX;
         int y = startY + row * 24;
         this.addRenderableWidget(Button.builder(setting.message(), button -> {
            setting.action().run();
            RunParticlesConfig.save();
            button.setMessage(setting.message());
         }).bounds(x, y, buttonWidth, 20).build());
      }

      int rows = twoColumns ? (settings.length + 1) / 2 : settings.length;
      int doneY = Math.min(this.height - 28, startY + rows * 24 + 8);
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds((this.width - 120) / 2, doneY, 120, 20).build()
      );
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -1610612736);
      context.centeredText(this.font, this.title, this.width / 2, 16, 16777215);
      super.extractRenderState(context, mouseX, mouseY, delta);
   }

   public void onClose() {
      RunParticlesConfig.save();
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(this.parent);
      }
   }

   private static RunParticlesConfigScreen.ButtonEntry toggle(String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) {
      return new RunParticlesConfigScreen.ButtonEntry(
         () -> Component.translatable(
            "screen.run_particles_only.option_value",
            new Object[]{
               Component.translatable(labelKey),
               Component.translatable(getter.getAsBoolean() ? "value.run_particles_only.enabled" : "value.run_particles_only.disabled")
            }
         ),
         () -> setter.accept(!getter.getAsBoolean())
      );
   }

   private static RunParticlesConfigScreen.ButtonEntry cycle(String labelKey, Supplier<Component> value, Runnable action) {
      return new RunParticlesConfigScreen.ButtonEntry(
         () -> Component.translatable("screen.run_particles_only.option_value", new Object[]{Component.translatable(labelKey), value.get()}), action
      );
   }

   private record ButtonEntry(Supplier<Component> messageSupplier, Runnable action) {
      Component message() {
         return this.messageSupplier.get();
      }
   }
}
