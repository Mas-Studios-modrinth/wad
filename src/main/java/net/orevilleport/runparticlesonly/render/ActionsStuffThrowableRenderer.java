package net.orevilleport.runparticlesonly.render;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.aleca.actionsstufffp.ActionsStuffFirstPersonClient;
import dev.aleca.actionsstufffp.animation.BedrockPose;
import dev.aleca.actionsstufffp.config.ActionsStuffConfig;
import dev.aleca.actionsstufffp.render.FirstPersonRenderer;
import dev.aleca.actionsstufffp.runtime.BedrockAssets;
import dev.aleca.actionsstufffp.runtime.FirstPersonRuntime;
import dev.aleca.actionsstufffp.runtime.FirstPersonRuntime.Frame;
import dev.aleca.actionsstuffmobs.BedrockMobAssets;
import dev.aleca.actionsstuffmobs.animation.BoneTransform;
import dev.aleca.actionsstuffmobs.animation.MobAnimationLibrary;
import dev.aleca.actionsstuffmobs.animation.MobAnimationProgram;
import dev.aleca.actionsstuffmobs.math.Vec3Value;
import dev.aleca.actionsstuffmobs.render.BedrockGeometry;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.orevilleport.runparticlesonly.RunParticlesConfig;

public final class ActionsStuffThrowableRenderer {
   private static final Map<ThrownItemRenderState, ActionsStuffThrowableRenderer.RuntimeEntry> RUNTIMES = new WeakHashMap<>();
   private static BedrockAssets assets;
   private static BedrockMobAssets mobAssets;
   private static MobAnimationLibrary mobAnimations;

   private ActionsStuffThrowableRenderer() {
   }

   public static void capture(Entity entity, ThrownItemRenderState state, ItemStack stack, float tickProgress) {
      if (RunParticlesConfig.get().throwSpin && !ActionsStuffConfig.isCustomExperience() && !stack.isEmpty()) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player == null) {
            RUNTIMES.remove(state);
         } else {
            try {
               ActionsStuffThrowableRenderer.FlightAnimation kind = ActionsStuffThrowableRenderer.FlightAnimation.forProjectile(entity, stack);
               if (kind == ActionsStuffThrowableRenderer.FlightAnimation.NONE) {
                  RUNTIMES.remove(state);
                  return;
               }

               ActionsStuffThrowableRenderer.RuntimeEntry entry = RUNTIMES.computeIfAbsent(
                  state, ignored -> new ActionsStuffThrowableRenderer.RuntimeEntry(new FirstPersonRuntime(assets()))
               );
               entry.stack = stack.copy();
               entry.frame = entry.runtime.updateFlyingItem(player, entry.stack, tickProgress, (entity.tickCount + tickProgress) / 20.0F);
               if (entry.sourceAnimation == null || entry.sourceAnimation.kind != kind) {
                  entry.sourceAnimation = ActionsStuffThrowableRenderer.SourceAnimation.create(kind);
               }

               entry.sourceAnimation.sample(entity, tickProgress);
               if (entry.frame.mainRenderPasses().isEmpty()) {
                  RUNTIMES.remove(state);
               }
            } catch (RuntimeException exception) {
               RUNTIMES.remove(state);
               ActionsStuffFirstPersonClient.LOGGER.warn("Falling back to vanilla flying-item render for {}", stack.getItem(), exception);
            }
         }
      } else {
         RUNTIMES.remove(state);
      }
   }

   public static boolean render(ThrownItemRenderState state, PoseStack matrices, SubmitNodeCollector queue, int light, CameraRenderState camera) {
      ActionsStuffThrowableRenderer.RuntimeEntry entry = RUNTIMES.get(state);
      if (entry != null && entry.frame != null && entry.sourceAnimation != null && !entry.frame.mainRenderPasses().isEmpty()) {
         matrices.pushPose();

         try {
            matrices.rotate(Axis.YP.rotationDegrees(-180.0F));
            matrices.rotate(Axis.XP.rotationDegrees(90.0F));
            matrices.scale(-0.0625F, -0.0625F, 0.0625F);
            BedrockPose pose = entry.sourceAnimation.itemPose();
            FirstPersonRenderer.renderProjectileItemPasses(
               entry.frame.mainAttachable() == null ? ItemStack.EMPTY : entry.stack, entry.frame.mainRenderPasses(), matrices, queue, light, pose, false
            );
            return true;
         } catch (RuntimeException exception) {
            ActionsStuffFirstPersonClient.LOGGER.warn("Falling back to vanilla flying-item render", exception);
            return false;
         } finally {
            matrices.popPose();
         }
      } else {
         return false;
      }
   }

   private static BedrockAssets assets() {
      if (assets == null) {
         assets = FirstPersonRenderer.sharedAssets();
      }

      return assets;
   }

   private static BedrockMobAssets mobAssets() {
      if (mobAssets == null) {
         mobAssets = (BedrockMobAssets)BedrockMobAssets.create().orElseThrow(() -> new IllegalStateException("Actions & Stuff mob assets are unavailable"));
      }

      return mobAssets;
   }

   private static MobAnimationLibrary mobAnimations() {
      if (mobAnimations == null) {
         mobAnimations = MobAnimationLibrary.load();
      }

      return mobAnimations;
   }

   private enum FlightAnimation {
      NONE(null, null),
      SPLASH_POTION("minecraft:splash_potion", "hglagf"),
      LINGERING_POTION("minecraft:lingering_potion", "hglagf"),
      EXPERIENCE_BOTTLE("minecraft:xp_bottle", "hglagf"),
      EGG("minecraft:egg", "688"),
      SNOWBALL("minecraft:snowball", "kfgo32dd"),
      ENDER_PEARL("minecraft:ender_pearl", "6f56jh62jd"),
      ENDER_EYE("minecraft:eye_of_ender_signal", "6f56j_6q6");

      private final String sourceIdentifier;
      private final String modelBone;

      FlightAnimation(String sourceIdentifier, String modelBone) {
         this.sourceIdentifier = sourceIdentifier;
         this.modelBone = modelBone;
      }

      private static ActionsStuffThrowableRenderer.FlightAnimation forProjectile(Entity entity, ItemStack stack) {
         if (stack.is(Items.SPLASH_POTION)) {
            return SPLASH_POTION;
         } else if (stack.is(Items.LINGERING_POTION)) {
            return LINGERING_POTION;
         } else if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return EXPERIENCE_BOTTLE;
         } else if (stack.is(Items.EGG)) {
            return EGG;
         } else if (stack.is(Items.SNOWBALL)) {
            return SNOWBALL;
         } else if (stack.is(Items.ENDER_PEARL)) {
            return ENDER_PEARL;
         } else {
            return stack.is(Items.ENDER_EYE) ? ENDER_EYE : NONE;
         }
      }
   }

   private static final class RuntimeEntry {
      private final FirstPersonRuntime runtime;
      private Frame frame;
      private ItemStack stack = ItemStack.EMPTY;
      private ActionsStuffThrowableRenderer.SourceAnimation sourceAnimation;

      private RuntimeEntry(FirstPersonRuntime runtime) {
         this.runtime = runtime;
      }
   }

   private static final class SourceAnimation {
      private final ActionsStuffThrowableRenderer.FlightAnimation kind;
      private final MobAnimationProgram program;
      private final BedrockGeometry geometry;
      private final dev.aleca.actionsstuffmobs.animation.BedrockPose pose = new dev.aleca.actionsstuffmobs.animation.BedrockPose();

      private SourceAnimation(ActionsStuffThrowableRenderer.FlightAnimation kind, MobAnimationProgram program, BedrockGeometry geometry) {
         this.kind = kind;
         this.program = program;
         this.geometry = geometry;
      }

      private static ActionsStuffThrowableRenderer.SourceAnimation create(ActionsStuffThrowableRenderer.FlightAnimation kind) {
         JsonObject definition = (JsonObject)ActionsStuffThrowableRenderer.mobAssets()
            .entityDefinition(kind.sourceIdentifier)
            .orElseThrow(() -> new IllegalStateException("Missing Actions & Stuff projectile definition: " + kind.sourceIdentifier));
         String geometryId = definition.getAsJsonObject("minecraft:client_entity")
            .getAsJsonObject("description")
            .getAsJsonObject("geometry")
            .get("default")
            .getAsString();
         BedrockGeometry geometry = ActionsStuffThrowableRenderer.mobAssets()
            .geometry(geometryId)
            .map(model -> BedrockGeometry.parse(geometryId, model))
            .orElseThrow(() -> new IllegalStateException("Missing Actions & Stuff projectile geometry: " + geometryId));
         return new ActionsStuffThrowableRenderer.SourceAnimation(kind, ActionsStuffThrowableRenderer.mobAnimations().program(definition), geometry);
      }

      private void sample(Entity entity, float tickProgress) {
         this.pose.clear();
         double seconds = (entity.tickCount + tickProgress) / 40.0;
         float bodyYaw = entity.getYRot(tickProgress);
         this.program
            .sample(
               this.pose,
               seconds,
               0.0,
               0.0,
               entity.onGround(),
               entity.isInWater(),
               false,
               false,
               false,
               entity.isAlive(),
               entity.getDeltaMovement().y * 20.0,
               entity.getXRot(tickProgress),
               0.0F,
               bodyYaw,
               false,
               false,
               0.0,
               entity.getX(),
               entity.getY(),
               entity.getZ()
            );
      }

      private void apply(PoseStack matrices) {
         this.geometry.applyBoneTransform(this.kind.modelBone, matrices, this.pose);
      }

      private BedrockPose itemPose() {
         BedrockPose result = new BedrockPose();

         for (Entry<String, BoneTransform> entry : this.pose.bones().entrySet()) {
            BoneTransform transform = entry.getValue();
            Vec3Value position = transform.position();
            Vec3Value rotation = transform.rotation();
            Vec3Value scale = transform.scale();
            result.add(
               entry.getKey(),
               new dev.aleca.actionsstufffp.math.Vec3Value(position.x(), position.y(), position.z()),
               new dev.aleca.actionsstufffp.math.Vec3Value(rotation.x(), rotation.y(), rotation.z()),
               new dev.aleca.actionsstufffp.math.Vec3Value(scale.x(), scale.y(), scale.z()),
               1.0,
               transform.entityRelativeRotation()
            );
         }

         return result;
      }
   }
}
