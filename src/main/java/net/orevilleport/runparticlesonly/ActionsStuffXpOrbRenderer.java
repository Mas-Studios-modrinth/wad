package net.orevilleport.runparticlesonly;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class ActionsStuffXpOrbRenderer {
   private static final float PIXEL = 0.020833334F;
   private static final float U0 = 0.16666667F;
   private static final float U1 = 0.5F;
   private static final float U2 = 0.8333333F;
   private static final float V0 = 0.25F;
   private static final float V1 = 0.75F;
   private static final float BOTTOM_GRADIENT_BRIGHTNESS = 0.55F;
   private static final double FAR_TRAIL_START_SQUARED = 12.25;
   private static final float FAR_TRAIL_HEIGHT_SCALE = 0.25F;

   private ActionsStuffXpOrbRenderer() {
   }

   public static void render(ExperienceOrbRenderState state, PoseStack matrices, OrderedSubmitNodeCollector queue, CameraRenderState camera) {
      BedrockXpOrbDefinition definition = BedrockXpOrbDefinition.get();
      ActionsStuffXpOrbRenderState runtimeState = (ActionsStuffXpOrbRenderState)state;
      float sourceLifeTime = (float)Math.floor(state.ageInTicks) / 20.0F;
      BedrockXpOrbDefinition.EvalContext context = BedrockXpOrbDefinition.EvalContext.entity(
            sourceLifeTime, state.icon, runtimeState.runParticlesOnly$getOrbPhase()
         )
         .withVariable("v.mvycwl", runtimeState.runParticlesOnly$getGroundSpeed());

      for (BedrockXpOrbDefinition.RenderPass pass : definition.renderPasses()) {
         BedrockXpOrbDefinition.RenderValues materialValues = definition.renderValues(pass, context);
         if (pass.geometryId().equals("geometry.oreville_ans.jujxrc")) {
            renderCore(queue, matrices, state, context, ActionsStuffXpOrbOverlayTexture.get(materialValues, false));
         } else if (pass.geometryId().equals("geometry.oreville_ans.lovaey")) {
            renderPulse(queue, matrices, state, context, materialValues, ActionsStuffXpOrbOverlayTexture.getTrailPlane(materialValues));
         } else if (pass.geometryId().equals("geometry.oreville_ans.osgkbi") && shouldRenderFarTrail(runtimeState)) {
            renderFarTrail(queue, matrices, state, context, materialValues, ActionsStuffXpOrbOverlayTexture.getFarTrail(materialValues));
         }
      }
   }

   private static boolean shouldRenderFarTrail(ActionsStuffXpOrbRenderState runtimeState) {
      Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.mainCamera().position();
      return runtimeState.runParticlesOnly$getRenderPosition().distanceToSqr(cameraPosition) >= 12.25;
   }

   private static void renderCore(
      OrderedSubmitNodeCollector queue, PoseStack matrices, ExperienceOrbRenderState state, BedrockXpOrbDefinition.EvalContext context, Identifier texture
   ) {
      matrices.pushPose();
      applyBodyTransform(matrices, state, context);
      RenderType outerLayer = coreMaterialLayer(texture);
      queue.submitCustomGeometry(
         matrices, outerLayer, (entry, vertices) -> cube(vertices, entry, -0.85F, 0.3F, -0.85F, 0.85F, 2.0F, 0.85F, 0.8333333F, 0.8333333F, 0.5F, 0.25F, true)
      );
      queue.submitCustomGeometry(
         matrices,
         innerCoreMaterialLayer(texture),
         (entry, vertices) -> cube(vertices, entry, -0.68F, 0.47F, -0.68F, 0.68F, 1.83F, 0.68F, 0.5F, 0.5F, 0.5F, 0.25F, false)
      );
      matrices.popPose();
   }

   private static void renderPulse(
      OrderedSubmitNodeCollector queue,
      PoseStack matrices,
      ExperienceOrbRenderState state,
      BedrockXpOrbDefinition.EvalContext context,
      BedrockXpOrbDefinition.RenderValues values,
      Identifier texture
   ) {
      float pulse = 1.1F + (float)Math.sin(Math.toRadians(context.lifeTime() * 180.0F + context.phase())) * 0.1F;
      matrices.pushPose();
      applyBodyTransform(matrices, state, context);
      matrices.translate(0.0F, 1.15F, 0.0F);
      matrices.scale(pulse, pulse, pulse);
      matrices.translate(0.0F, -1.15F, 0.0F);
      queue.submitCustomGeometry(matrices, glowMaterialLayer(texture), (entry, vertices) -> {
         float animatedV = 0.25F + values.uvOffset().y() * 0.5F;
         cube(vertices, entry, -1.02F, 0.13F, -1.02F, 1.02F, 2.17F, 1.02F, 0.5F, 0.5F, 0.8333333F, animatedV, false);
      });
      matrices.popPose();
   }

   private static void renderFarTrail(
      OrderedSubmitNodeCollector queue,
      PoseStack matrices,
      ExperienceOrbRenderState state,
      BedrockXpOrbDefinition.EvalContext context,
      BedrockXpOrbDefinition.RenderValues values,
      Identifier texture
   ) {
      matrices.pushPose();
      applyFarTrailTransform(matrices, state);
      queue.submitCustomGeometry(matrices, glowMaterialLayer(texture), (entry, vertices) -> {
         plane(vertices, entry, -0.85F, farTrailY(1.80875F), 0.85F, farTrailY(18.80875F), 0.16666667F, 0.75F);
         plane(vertices, entry, -0.85F, farTrailY(1.15F), 0.85F, farTrailY(1.80875F), 0.16666667F, 0.25F);
      });
      matrices.popPose();
   }

   private static float farTrailY(float sourceY) {
      return 1.15F + (sourceY - 1.15F) * 0.25F;
   }

   private static void applyBodyTransform(PoseStack matrices, ExperienceOrbRenderState state, BedrockXpOrbDefinition.EvalContext context) {
      float quadSize = (2.4F + state.icon * 0.6F) * 0.020833334F;
      float angle = context.lifeTime() * 90.0F + context.phase();
      float roll = (float)Math.sin(Math.toRadians(angle)) * 22.5F;
      matrices.translate(0.0F, 0.1F, 0.0F);
      matrices.scale(quadSize, quadSize, quadSize);
      matrices.translate(0.0F, 1.15F, 0.0F);
      matrices.rotate(new Quaternionf().rotateY((float)Math.toRadians(angle)).rotateZ((float)Math.toRadians(roll)));
      matrices.translate(0.0F, -1.15F, 0.0F);
      matrices.translate(0.0F, 0.5F, 0.0F);
   }

   private static void applyFarTrailTransform(PoseStack matrices, ExperienceOrbRenderState state) {
      float quadSize = (2.4F + state.icon * 0.6F) * 0.020833334F;
      float cameraYaw = Minecraft.getInstance().gameRenderer.mainCamera().yRot();
      matrices.translate(0.0F, 0.1F, 0.0F);
      matrices.scale(quadSize, quadSize, quadSize);
      matrices.translate(0.0F, 1.65F, 0.0F);
      matrices.rotate(new Quaternionf().rotateY((float)Math.toRadians(-cameraYaw)));
      matrices.translate(0.0F, -1.15F, 0.0F);
   }

   private static RenderType coreMaterialLayer(Identifier texture) {
      return ActionsStuffXpOrbShaders.core(texture);
   }

   private static RenderType glowMaterialLayer(Identifier texture) {
      return ActionsStuffXpOrbShaders.glow(texture);
   }

   private static RenderType innerCoreMaterialLayer(Identifier texture) {
      return ActionsStuffXpOrbShaders.innerCore(texture);
   }

   private static void cube(
      VertexConsumer vertices,
      Pose entry,
      float minX,
      float minY,
      float minZ,
      float maxX,
      float maxY,
      float maxZ,
      float sideU,
      float topU,
      float bottomU,
      float v,
      boolean inverted
   ) {
      quad(vertices, entry, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, sideU, v, sideU, v, inverted, minY, maxY);
      quad(vertices, entry, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, sideU, v, sideU, v, inverted, minY, maxY);
      quad(vertices, entry, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, sideU, v, sideU, v, inverted, minY, maxY);
      quad(vertices, entry, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, sideU, v, sideU, v, inverted, minY, maxY);
      quad(vertices, entry, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, topU, v, topU, v, inverted, minY, maxY);
      quad(vertices, entry, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, bottomU, v, bottomU, v, inverted, minY, maxY);
   }

   private static void plane(VertexConsumer vertices, Pose entry, float minX, float minY, float maxX, float maxY, float u, float v) {
      vertex(vertices, entry, minX, minY, 0.0F, u, v, minY, maxY);
      vertex(vertices, entry, maxX, minY, 0.0F, u, v, minY, maxY);
      vertex(vertices, entry, maxX, maxY, 0.0F, u, v, minY, maxY);
      vertex(vertices, entry, minX, maxY, 0.0F, u, v, minY, maxY);
   }

   private static void quad(
      VertexConsumer vertices,
      Pose entry,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float minU,
      float minV,
      float maxU,
      float maxV,
      boolean inverted,
      float cubeMinY,
      float cubeMaxY
   ) {
      if (inverted) {
         vertex(vertices, entry, x0, y0, z0, minU, minV, cubeMinY, cubeMaxY);
         vertex(vertices, entry, x3, y3, z3, minU, maxV, cubeMinY, cubeMaxY);
         vertex(vertices, entry, x2, y2, z2, maxU, maxV, cubeMinY, cubeMaxY);
         vertex(vertices, entry, x1, y1, z1, maxU, minV, cubeMinY, cubeMaxY);
      } else {
         vertex(vertices, entry, x0, y0, z0, minU, minV, cubeMinY, cubeMaxY);
         vertex(vertices, entry, x1, y1, z1, maxU, minV, cubeMinY, cubeMaxY);
         vertex(vertices, entry, x2, y2, z2, maxU, maxV, cubeMinY, cubeMaxY);
         vertex(vertices, entry, x3, y3, z3, minU, maxV, cubeMinY, cubeMaxY);
      }
   }

   private static void vertex(VertexConsumer vertices, Pose entry, float x, float y, float z, float u, float v, float cubeMinY, float cubeMaxY) {
      float height = Math.max(1.0E-4F, cubeMaxY - cubeMinY);
      float verticalProgress = Math.max(0.0F, Math.min(1.0F, (y - cubeMinY) / height));
      int shade = Math.round(255.0F * (0.55F + 0.45F * verticalProgress));
      vertices.addVertex(entry, x, y, z)
         .setColor(shade, shade, shade, 255)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(entry, 0.0F, 1.0F, 0.0F);
   }
}
