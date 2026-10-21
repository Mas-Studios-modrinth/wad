package net.orevilleport.runparticlesonly;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.Iterator;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ToolTrailEffects {
   private static final float TICKS_PER_SECOND = 20.0F;
   private static final float LIFETIME_SECONDS = 0.24F;
   private static final float WIDTH = 0.115F;
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("run_particles_only", "textures/particle/glowing_smol.png");
   private static final RenderType LAYER = ActionsStuffXpOrbShaders.glow(TEXTURE);
   private static final ArrayDeque<ToolTrailEffects.TrailPoint> POINTS = new ArrayDeque<>();
   private static Vec3 previousTip;
   private static boolean wasSwinging;
   private static Vec3 animatedToolTip;
   private static long animatedToolTipCapturedAt = Long.MIN_VALUE;

   private ToolTrailEffects() {
   }

   public static void tick(LocalPlayer player) {
      long now = player.level().getGameTime();
      prune(now);
      if (RunParticlesConfig.get().toolTrailParticles && player.swinging) {
         InteractionHand hand = player.swingingArm;
         ItemStack stack = hand == InteractionHand.OFF_HAND ? player.getOffhandItem() : player.getMainHandItem();
         if (!isTool(stack)) {
            previousTip = null;
            wasSwinging = false;
         } else {
            Vec3 tip = swingTip(player, hand, player.getAttackAnim(1.0F), now);
            if (tip == null) {
               previousTip = null;
               wasSwinging = false;
            } else {
               if (wasSwinging && previousTip != null && !(previousTip.distanceToSqr(tip) > 3.0)) {
                  POINTS.addLast(new ToolTrailEffects.TrailPoint(previousTip.lerp(tip, 0.5), now));
                  POINTS.addLast(new ToolTrailEffects.TrailPoint(tip, now));
               } else {
                  POINTS.clear();
                  POINTS.addLast(new ToolTrailEffects.TrailPoint(tip, now));
               }

               while (POINTS.size() > 12) {
                  POINTS.removeFirst();
               }

               previousTip = tip;
               wasSwinging = true;
            }
         }
      } else {
         previousTip = null;
         wasSwinging = false;
      }
   }

   public static void render(LevelRenderContext context) {
      if (RunParticlesConfig.get().toolTrailParticles && POINTS.size() >= 2) {
         Minecraft client = Minecraft.getInstance();
         if (client.level != null) {
            ToolTrailEffects.TrailPoint[] points = POINTS.toArray(ToolTrailEffects.TrailPoint[]::new);
            Vec3 camera = context.gameRenderer().mainCamera().position();
            float now = (float)client.level.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            context.submitNodeCollector()
               .submitCustomGeometry(
                  new PoseStack(),
                  LAYER,
                  (entry, vertices) -> {
                     for (int index = 0; index < points.length - 1; index++) {
                        ToolTrailEffects.TrailPoint first = points[index];
                        ToolTrailEffects.TrailPoint second = points[index + 1];
                        Vec3 direction = second.position.subtract(first.position);
                        if (!(direction.lengthSqr() < 1.0E-8)) {
                           float firstAlpha = alpha(now, first.createdAt);
                           float secondAlpha = alpha(now, second.createdAt);
                           if (!(firstAlpha <= 0.0F) || !(secondAlpha <= 0.0F)) {
                              Vec3 firstSide = cameraFacingSide(first.position, direction, camera, 0.115F * (0.35F + 0.65F * firstAlpha));
                              Vec3 secondSide = cameraFacingSide(second.position, direction, camera, 0.115F * (0.35F + 0.65F * secondAlpha));
                              if (firstSide != null && secondSide != null) {
                                 emitQuad(
                                    vertices,
                                    first.position.subtract(firstSide).subtract(camera),
                                    first.position.add(firstSide).subtract(camera),
                                    second.position.add(secondSide).subtract(camera),
                                    second.position.subtract(secondSide).subtract(camera),
                                    112,
                                    220,
                                    255,
                                    Math.round(firstAlpha * 165.0F),
                                    Math.round(secondAlpha * 235.0F)
                                 );
                              }
                           }
                        }
                     }
                  }
               );
         }
      }
   }

   public static void clear() {
      POINTS.clear();
      previousTip = null;
      wasSwinging = false;
      animatedToolTip = null;
      animatedToolTipCapturedAt = Long.MIN_VALUE;
   }

   public static void captureAnimatedHand(AvatarRenderState state, ItemStack stack, HumanoidArm arm, PoseStack matrices) {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null && state.id == player.getId() && isTool(stack)) {
         HumanoidArm swingingArm = player.swingingArm == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         if (arm == swingingArm) {
            Matrix4f itemMatrix = new Matrix4f(matrices.last().pose());
            itemMatrix.translate(arm == HumanoidArm.RIGHT ? -0.0625F : 0.0625F, 0.4375F, 0.0625F);
            itemMatrix.scale(-0.0625F, -0.0625F, 0.0625F);
            Vector3f toolTip = new Vector3f(0.0F, toolLength(stack), 0.0F);
            itemMatrix.transformPosition(toolTip);
            Vec3 camera = client.gameRenderer.mainCamera().position();
            animatedToolTip = camera.add(toolTip.x, toolTip.y, toolTip.z);
            animatedToolTipCapturedAt = player.level().getGameTime();
         }
      }
   }

   public static void captureFirstPersonTool(LocalPlayer player, ItemStack stack, String slotBone, PoseStack matrices) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == player && player.swinging && isTool(stack)) {
         boolean mainSwing = player.swingingArm == InteractionHand.MAIN_HAND;
         if ((!mainSwing || "rightitem".equals(slotBone)) && (mainSwing || "leftitem".equals(slotBone))) {
            Matrix4f itemMatrix = new Matrix4f(matrices.last().pose());
            Vector3f toolTip = new Vector3f(0.0F, toolLength(stack), 0.0F);
            itemMatrix.transformPosition(toolTip);
            Vec3 camera = client.gameRenderer.mainCamera().position();
            animatedToolTip = camera.add(toolTip.x, toolTip.y, toolTip.z);
            animatedToolTipCapturedAt = player.level().getGameTime();
         }
      }
   }

   private static boolean isTool(ItemStack stack) {
      return !stack.isEmpty()
         && (
            stack.is(ItemTags.SWORDS)
               || stack.is(ItemTags.AXES)
               || stack.is(ItemTags.PICKAXES)
               || stack.is(ItemTags.SHOVELS)
               || stack.is(ItemTags.HOES)
               || stack.is(Items.SHEARS)
               || stack.is(Items.TRIDENT)
               || stack.is(Items.MACE)
         );
   }

   private static Vec3 swingTip(LocalPlayer player, InteractionHand hand, float swing, long now) {
      Minecraft client = Minecraft.getInstance();
      if (!client.options.getCameraType().isFirstPerson()) {
         double yaw = Math.toRadians(player.getYRot());
         Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
         Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
         double side = hand == InteractionHand.OFF_HAND ? -1.0 : 1.0;
         if (animatedToolTip != null && now - animatedToolTipCapturedAt <= 1L) {
            return animatedToolTip;
         }

         double arc = Mth.lerp(swing, -1.12F, 1.64F);
         Vec3 shoulder = player.position().add(right.scale(side * 0.34)).add(0.0, player.getBbHeight() * 0.67, 0.0).add(forward.scale(0.18));
         return shoulder.add(right.scale(side * (0.28 + Math.cos(arc) * 0.38)))
            .add(forward.scale(0.15 + Math.sin(arc) * 0.64))
            .add(0.0, Math.sin(arc * 0.82) * 0.28, 0.0);
      } else {
         return animatedToolTip != null && now - animatedToolTipCapturedAt <= 1L ? animatedToolTip : null;
      }
   }

   private static void prune(long now) {
      Iterator<ToolTrailEffects.TrailPoint> iterator = POINTS.iterator();

      while (iterator.hasNext()) {
         if ((float)(now - iterator.next().createdAt) / 20.0F > 0.24F) {
            iterator.remove();
         }
      }
   }

   private static float toolLength(ItemStack stack) {
      if (stack.is(ItemTags.SWORDS) || stack.is(Items.TRIDENT)) {
         return 15.5F;
      } else if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES)) {
         return 13.5F;
      } else {
         return stack.is(Items.MACE) ? 14.0F : 12.5F;
      }
   }

   private static float alpha(float now, long createdAt) {
      float progress = Math.max(0.0F, Math.min(1.0F, (now - (float)createdAt) / 4.7999997F));
      return (1.0F - progress) * (1.0F - progress);
   }

   private static Vec3 cameraFacingSide(Vec3 point, Vec3 direction, Vec3 camera, float width) {
      Vec3 side = direction.normalize().cross(point.subtract(camera));
      return side.lengthSqr() < 1.0E-8 ? null : side.normalize().scale(width * 0.5);
   }

   private static void emitQuad(
      VertexConsumer vertices, Vec3 bottomLeft, Vec3 bottomRight, Vec3 topRight, Vec3 topLeft, int red, int green, int blue, int firstAlpha, int secondAlpha
   ) {
      vertex(vertices, bottomLeft, red, green, blue, firstAlpha, 0.5F, 0.5F);
      vertex(vertices, bottomRight, red, green, blue, firstAlpha, 0.5F, 0.5F);
      vertex(vertices, topRight, red, green, blue, secondAlpha, 0.5F, 0.5F);
      vertex(vertices, topLeft, red, green, blue, secondAlpha, 0.5F, 0.5F);
   }

   private static void vertex(VertexConsumer vertices, Vec3 position, int red, int green, int blue, int alpha, float u, float v) {
      vertices.addVertex((float)position.x, (float)position.y, (float)position.z)
         .setColor(red, green, blue, alpha)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(0.0F, 1.0F, 0.0F);
   }

   private record TrailPoint(Vec3 position, long createdAt) {
   }
}
