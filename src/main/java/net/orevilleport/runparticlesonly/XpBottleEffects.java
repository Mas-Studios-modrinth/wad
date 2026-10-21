package net.orevilleport.runparticlesonly;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

public final class XpBottleEffects {
   private static final double RANGE = 32.0;
   private static final int SAMPLES_PER_TICK = 2;
   private static final float TICKS_PER_SECOND = 20.0F;
   private static final float LIFETIME = 0.3F;
   private static final double VERTICAL_OFFSET = -0.15625;
   private static final double TAIL_OFFSET = 0.08;
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("run_particles_only", "textures/oreville/ans/dgi.png");
   private static final RenderType LAYER = ActionsStuffXpOrbShaders.glow(TEXTURE);
   private static final Map<Integer, XpBottleEffects.BottleState> STATES = new HashMap<>();
   private static final Set<Integer> LIVE_IDS = new HashSet<>();

   private XpBottleEffects() {
   }

   public static void tick(ClientLevel world, LocalPlayer player) {
      if (!RunParticlesConfig.get().xpTrailParticles) {
         clear();
      } else {
         long now = world.getGameTime();
         LIVE_IDS.clear();

         for (ThrownExperienceBottle bottle : world.getEntities(
            EntityTypeTest.forClass(ThrownExperienceBottle.class), player.getBoundingBox().inflate(32.0), candidate -> true
         )) {
            LIVE_IDS.add(bottle.getId());
            Vec3 current = new Vec3(bottle.getX(), bottle.getY(), bottle.getZ());
            XpBottleEffects.BottleState state = STATES.computeIfAbsent(bottle.getId(), ignored -> new XpBottleEffects.BottleState(current));
            emitMotionSamples(state, current, now);
            state.previousPosition = current;
         }

         Iterator<Entry<Integer, XpBottleEffects.BottleState>> iterator = STATES.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<Integer, XpBottleEffects.BottleState> entry = iterator.next();
            entry.getValue().removeExpired(now);
            if (!LIVE_IDS.contains(entry.getKey()) && entry.getValue().segments.isEmpty()) {
               iterator.remove();
            }
         }
      }
   }

   public static void renderTrails(LevelRenderContext context) {
      if (RunParticlesConfig.get().xpTrailParticles) {
         Minecraft client = Minecraft.getInstance();
         ClientLevel world = client.level;
         if (world != null) {
            Vec3 camera = context.gameRenderer().mainCamera().position();
            float now = (float)world.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            BedrockXpOrbParticles orbTrail = BedrockXpOrbParticles.get();
            context.submitNodeCollector().submitCustomGeometry(new PoseStack(), LAYER, (entry, vertices) -> {
               for (XpBottleEffects.BottleState state : STATES.values()) {
                  renderSameAsOrbTrail(vertices, state.segments, camera, now, orbTrail);
               }
            });
         }
      }
   }

   public static void clear() {
      STATES.clear();
      LIVE_IDS.clear();
   }

   private static void emitMotionSamples(XpBottleEffects.BottleState state, Vec3 current, long now) {
      Vec3 travelled = current.subtract(state.previousPosition);
      double travelledLength = travelled.length();
      if (!(travelledLength < 1.0E-8)) {
         Vec3 direction = travelled.normalize();
         Vec3 tailOffset = direction.scale(0.08);

         for (int sample = 0; sample < 2; sample++) {
            double fraction = (sample + 0.5) / 2.0;
            Vec3 position = state.previousPosition.lerp(current, fraction).subtract(tailOffset).add(0.0, -0.15625, 0.0);
            float createdAt = (float)now - 1.0F + (float)fraction;
            state.segments.addLast(new XpBottleEffects.BottleSegment(position, createdAt));
         }
      }
   }

   private static void renderSameAsOrbTrail(
      VertexConsumer vertices, ArrayDeque<XpBottleEffects.BottleSegment> trail, Vec3 camera, float now, BedrockXpOrbParticles source
   ) {
      if (trail.size() >= 2) {
         XpBottleEffects.BottleSegment[] points = trail.toArray(XpBottleEffects.BottleSegment[]::new);
         float sourceWidth = source.trailWidth(0.0F);

         for (int index = 0; index < points.length - 1; index++) {
            XpBottleEffects.BottleSegment first = points[index];
            XpBottleEffects.BottleSegment second = points[index + 1];
            Vec3 direction = second.position.subtract(first.position);
            if (!(direction.lengthSqr() < 1.0E-8)) {
               Vec3 firstSide = ribbonSide(points, index, camera, taperedWidth(sourceWidth, index, points.length));
               Vec3 secondSide = ribbonSide(points, index + 1, camera, taperedWidth(sourceWidth, index + 1, points.length));
               if (firstSide != null && secondSide != null) {
                  float firstAge = (now - first.createdAt) / 20.0F;
                  BedrockXpOrbDefinition.Vec3 colour = source.trailColour(first.createdAt / 20.0F, firstAge);
                  emitRibbonQuad(
                     vertices,
                     first.position.subtract(firstSide).subtract(camera),
                     first.position.add(firstSide).subtract(camera),
                     second.position.add(secondSide).subtract(camera),
                     second.position.subtract(secondSide).subtract(camera),
                     channel(colour.x()),
                     channel(colour.y()),
                     channel(colour.z()),
                     255
                  );
               }
            }
         }

         renderTrailTip(vertices, points, camera, now, source, sourceWidth);
      }
   }

   private static void renderTrailTip(
      VertexConsumer vertices, XpBottleEffects.BottleSegment[] points, Vec3 camera, float now, BedrockXpOrbParticles source, float sourceWidth
   ) {
      XpBottleEffects.BottleSegment tail = points[0];
      XpBottleEffects.BottleSegment next = points[1];
      Vec3 backward = tail.position.subtract(next.position);
      if (!(backward.lengthSqr() < 1.0E-8)) {
         Vec3 side = ribbonSide(points, 0, camera, taperedWidth(sourceWidth, 0, points.length));
         if (side != null) {
            float age = (now - tail.createdAt) / 20.0F;
            BedrockXpOrbDefinition.Vec3 colour = source.trailColour(tail.createdAt / 20.0F, age);
            float length = source.trailLength((float)tail.position.distanceTo(next.position), 0.0F);
            Vec3 tip = tail.position.add(backward.normalize().scale(length));
            Vec3 narrowSide = side.scale(0.06);
            emitQuad(
               vertices,
               tip.subtract(narrowSide).subtract(camera),
               tip.add(narrowSide).subtract(camera),
               tail.position.add(side).subtract(camera),
               tail.position.subtract(side).subtract(camera),
               channel(colour.x()),
               channel(colour.y()),
               channel(colour.z()),
               255
            );
         }
      }
   }

   private static Vec3 ribbonSide(XpBottleEffects.BottleSegment[] points, int index, Vec3 camera, float width) {
      Vec3 tangent;
      if (index == 0) {
         tangent = points[1].position.subtract(points[0].position);
      } else if (index == points.length - 1) {
         tangent = points[index].position.subtract(points[index - 1].position);
      } else {
         tangent = points[index + 1].position.subtract(points[index - 1].position);
      }

      if (tangent.lengthSqr() < 1.0E-8) {
         return null;
      }

      Vec3 view = points[index].position.subtract(camera);
      Vec3 side = tangent.normalize().cross(view);
      return side.lengthSqr() < 1.0E-8 ? null : side.normalize().scale(width * 0.5F);
   }

   private static float taperedWidth(float width, int index, int count) {
      return count <= 1 ? width : width * (0.12F + 0.88F * index / (count - 1));
   }

   private static void emitRibbonQuad(
      VertexConsumer vertices, Vec3 bottomLeft, Vec3 bottomRight, Vec3 topRight, Vec3 topLeft, int red, int green, int blue, int alpha
   ) {
      vertex(vertices, bottomLeft, red, green, blue, alpha, 0.5F, 0.5F);
      vertex(vertices, bottomRight, red, green, blue, alpha, 0.5F, 0.5F);
      vertex(vertices, topRight, red, green, blue, alpha, 0.5F, 0.5F);
      vertex(vertices, topLeft, red, green, blue, alpha, 0.5F, 0.5F);
   }

   private static void emitQuad(
      VertexConsumer vertices, Vec3 bottomLeft, Vec3 bottomRight, Vec3 topRight, Vec3 topLeft, int red, int green, int blue, int alpha
   ) {
      vertex(vertices, bottomLeft, red, green, blue, alpha, 0.0F, 0.0F);
      vertex(vertices, bottomRight, red, green, blue, alpha, 1.0F, 0.0F);
      vertex(vertices, topRight, red, green, blue, alpha, 1.0F, 1.0F);
      vertex(vertices, topLeft, red, green, blue, alpha, 0.0F, 1.0F);
   }

   private static void vertex(VertexConsumer vertices, Vec3 position, int red, int green, int blue, int alpha, float u, float v) {
      vertices.addVertex((float)position.x, (float)position.y, (float)position.z)
         .setColor(red, green, blue, alpha)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(0.0F, 1.0F, 0.0F);
   }

   private static int channel(float value) {
      return Math.round(Math.max(0.0F, Math.min(value, 1.0F)) * 255.0F);
   }

   private record BottleSegment(Vec3 position, float createdAt) {
   }

   private static final class BottleState {
      private Vec3 previousPosition;
      private final ArrayDeque<XpBottleEffects.BottleSegment> segments = new ArrayDeque<>();

      private BottleState(Vec3 previousPosition) {
         this.previousPosition = previousPosition;
      }

      private void removeExpired(long now) {
         while (!this.segments.isEmpty() && ((float)now - this.segments.peekFirst().createdAt) / 20.0F > 0.3F) {
            this.segments.removeFirst();
         }
      }
   }
}
