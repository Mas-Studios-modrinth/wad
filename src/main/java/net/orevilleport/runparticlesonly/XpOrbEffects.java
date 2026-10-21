package net.orevilleport.runparticlesonly;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

public final class XpOrbEffects {
   private static final double EFFECT_RANGE = 16.0;
   private static final float TICKS_PER_SECOND = 20.0F;
   private static final Identifier AURA_TEXTURE = Identifier.fromNamespaceAndPath("run_particles_only", "textures/oreville/ans/dgr.png");
   private static final Identifier TRAIL_TEXTURE = Identifier.fromNamespaceAndPath("run_particles_only", "textures/oreville/ans/dgi.png");
   private static final Identifier SPARK_TEXTURE = Identifier.fromNamespaceAndPath("run_particles_only", "textures/particle/glowing_smol.png");
   private static final RenderType AURA_LAYER = ActionsStuffXpOrbShaders.glow(AURA_TEXTURE);
   private static final RenderType TRAIL_LAYER = ActionsStuffXpOrbShaders.glow(TRAIL_TEXTURE);
   private static final RenderType SPARK_LAYER = ActionsStuffXpOrbShaders.glow(SPARK_TEXTURE);
   private static final Map<Integer, XpOrbEffects.OrbEmitterState> ORB_STATES = new HashMap<>();
   private static final Set<Integer> LIVE_ORB_IDS = new HashSet<>();

   private XpOrbEffects() {
   }

   public static void tick(ClientLevel world, LocalPlayer player) {
      RunParticlesConfig config = RunParticlesConfig.get();
      if (!config.xpParticles && !config.xpTrailParticles) {
         clear();
      } else {
         BedrockXpOrbParticles source = BedrockXpOrbParticles.get();
         long worldTime = world.getGameTime();

         for (XpOrbEffects.OrbEmitterState state : ORB_STATES.values()) {
            state.tickSparks(source);
         }

         LIVE_ORB_IDS.clear();

         for (ExperienceOrb orb : world.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), player.getBoundingBox().inflate(16.0), candidate -> true)) {
            LIVE_ORB_IDS.add(orb.getId());
            XpOrbEffects.OrbEmitterState state = ORB_STATES.computeIfAbsent(orb.getId(), ignored -> new XpOrbEffects.OrbEmitterState(positionOf(orb)));
            if (config.xpParticles) {
               emitSparks(orb, state, source);
            } else {
               state.sparks.clear();
            }

            if (config.xpTrailParticles) {
               emitTrailPoints(orb, state, worldTime, source);
            }

            state.previousPosition = positionOf(orb);
         }

         for (XpOrbEffects.OrbEmitterState state : ORB_STATES.values()) {
            state.removeExpiredTrail(worldTime);
         }

         ORB_STATES.entrySet().removeIf(entry -> !LIVE_ORB_IDS.contains(entry.getKey()) && entry.getValue().isEmpty());
      }
   }

   public static void renderTrails(LevelRenderContext context) {
      Minecraft client = Minecraft.getInstance();
      ClientLevel world = client.level;
      LocalPlayer player = client.player;
      if (world != null && player != null) {
         BedrockXpOrbParticles source = BedrockXpOrbParticles.get();
         Vec3 cameraPos = context.gameRenderer().mainCamera().position();
         float tickProgress = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
         if (RunParticlesConfig.get().xpParticles) {
            context.submitNodeCollector().submitCustomGeometry(new PoseStack(), SPARK_LAYER, (entry, vertices) -> renderSparks(vertices, cameraPos, source));
         }

         context.submitNodeCollector()
            .submitCustomGeometry(new PoseStack(), AURA_LAYER, (entry, vertices) -> renderAuras(vertices, world, player, cameraPos, tickProgress, source));
         if (RunParticlesConfig.get().xpTrailParticles) {
            float now = (float)world.getGameTime() + tickProgress;
            context.submitNodeCollector().submitCustomGeometry(new PoseStack(), TRAIL_LAYER, (entry, vertices) -> {
               for (XpOrbEffects.OrbEmitterState state : ORB_STATES.values()) {
                  renderTrailRibbon(vertices, state.trail, now, cameraPos, source);
               }
            });
         }
      }
   }

   public static void clear() {
      ORB_STATES.clear();
      LIVE_ORB_IDS.clear();
   }

   private static void emitSparks(ExperienceOrb orb, XpOrbEffects.OrbEmitterState state, BedrockXpOrbParticles source) {
      state.sparkRemainder = state.sparkRemainder + source.sparkRate() / 20.0F;
      int count = Math.min((int)state.sparkRemainder, Math.max(0, source.sparkMaximum() - state.sparks.size()));
      state.sparkRemainder -= count;
      ThreadLocalRandom random = ThreadLocalRandom.current();
      float frame = orb.getIcon();
      BedrockXpOrbDefinition.Vec3 offset = source.sparkOffset(frame);
      float radius = source.sparkRadius(frame);
      float modelScale = (2.4F + frame * 0.6F) / 48.0F;
      float sourceModelCentreY = 0.1F + modelScale * 1.65F;

      for (int i = 0; i < count; i++) {
         double angle = random.nextDouble(0.0, Math.PI * 2);
         double distance = Math.sqrt(random.nextDouble()) * radius;
         float speed = source.sparkInitialSpeed(frame, random.nextFloat());
         float lifetime = Math.min(0.6F, Math.max(0.05F, source.sparkLifetime(frame, random.nextFloat())));
         state.sparks
            .addLast(
               new XpOrbEffects.Spark(
                  new Vec3(
                     orb.getX() + offset.x() + Math.cos(angle) * distance,
                     orb.getY() + sourceModelCentreY + offset.y(),
                     orb.getZ() + offset.z() + Math.sin(angle) * distance
                  ),
                  new Vec3(0.0, speed / 20.0F, 0.0),
                  0.0F,
                  lifetime,
                  frame
               )
            );
      }
   }

   private static void renderSparks(VertexConsumer vertices, Vec3 cameraPos, BedrockXpOrbParticles source) {
      for (XpOrbEffects.OrbEmitterState state : ORB_STATES.values()) {
         for (XpOrbEffects.Spark spark : state.sparks) {
            float size = source.sparkScale(spark.frame, spark.age, spark.lifetime) * 15.3F;
            BedrockXpOrbDefinition.Vec3 colour = source.sparkColour(spark.frame, spark.age, spark.lifetime);
            renderCameraFacingQuad(
               vertices, spark.position, cameraPos, size, size, channel(colour.x()), channel(colour.y()), channel(colour.z()), 255, 0.0F, 0.0F, 1.0F, 1.0F
            );
         }
      }
   }

   private static void emitTrailPoints(ExperienceOrb orb, XpOrbEffects.OrbEmitterState state, long worldTime, BedrockXpOrbParticles source) {
      Vec3 previous = state.previousPosition;
      Vec3 current = positionOf(orb);
      if (current.distanceToSqr(previous) < 1.0E-8) {
         state.trail.clear();
      } else {
         state.trailRemainder = state.trailRemainder + source.trailEmissionRate() / 20.0F;
         int count = (int)state.trailRemainder;
         state.trailRemainder -= count;
         float frame = orb.getIcon();
         float offset = source.trailYOffset(frame);
         float trailLifetimeBase = source.trailInterval();
         float width = source.trailWidth(frame);
         float lifetime = source.trailLifetime(frame);
         double visualRadius = Math.max(0.06, (2.4 + frame * 0.6) / 48.0 * 0.9);
         Vec3 tailOffset = current.subtract(previous).normalize().scale(visualRadius);

         for (int i = 0; i < count; i++) {
            float fraction = (i + 1.0F) / (count + 1.0F);
            float createdAt = (float)worldTime - 1.0F + fraction;
            Vec3 position = previous.lerp(current, fraction).subtract(tailOffset).add(0.0, offset, 0.0);
            float sourceSpawnAge = (orb.tickCount - 1.0F + fraction) / 20.0F;
            float elapsed = Float.isNaN(state.lastTrailSourceAge) ? 0.0F : sourceSpawnAge - state.lastTrailSourceAge;
            float interval = Math.max(0.0F, Math.min(1.0F, elapsed / trailLifetimeBase));
            state.lastTrailSourceAge = sourceSpawnAge;
            state.trail.addLast(new XpOrbEffects.TrailPoint(position, createdAt, sourceSpawnAge, interval, width, lifetime, frame));
         }
      }
   }

   private static void renderAuras(
      VertexConsumer vertices, ClientLevel world, LocalPlayer player, Vec3 cameraPos, float tickProgress, BedrockXpOrbParticles source
   ) {
      for (ExperienceOrb orb : world.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), player.getBoundingBox().inflate(16.0), candidate -> true)) {
         float particleAge = (orb.tickCount + tickProgress) / 20.0F;
         BedrockXpOrbDefinition.Vec3 colour = source.auraColour(particleAge);
         int red = channel(colour.x());
         int green = channel(colour.y());
         int blue = channel(colour.z());
         int alpha = channel(source.auraAlpha(particleAge));
         float frame = orb.getIcon();
         float verticalSpeed = Math.abs((float)orb.getDeltaMovement().y);
         float size = source.auraSize(frame, verticalSpeed) * 3.2F;
         if (!(size <= 0.0F)) {
            BedrockXpOrbDefinition.Vec3 offset = source.auraOffset(frame);
            Vec3 position = orb.getPosition(tickProgress).add(offset.x(), offset.y() + 0.03, offset.z());
            renderHorizontalQuad(vertices, position, cameraPos, size, size, red, green, blue, alpha);
         }
      }
   }

   private static void renderHorizontalQuad(
      VertexConsumer vertices, Vec3 position, Vec3 cameraPos, float width, float height, int red, int green, int blue, int alpha
   ) {
      double halfWidth = width * 0.5;
      double halfHeight = height * 0.5;
      emitQuad(
         vertices,
         position.add(-halfWidth, 0.0, -halfHeight).subtract(cameraPos),
         position.add(halfWidth, 0.0, -halfHeight).subtract(cameraPos),
         position.add(halfWidth, 0.0, halfHeight).subtract(cameraPos),
         position.add(-halfWidth, 0.0, halfHeight).subtract(cameraPos),
         red,
         green,
         blue,
         alpha,
         0.0F,
         0.0F,
         1.0F,
         1.0F
      );
   }

   private static void renderTrailRibbon(
      VertexConsumer vertices, ArrayDeque<XpOrbEffects.TrailPoint> trail, float now, Vec3 cameraPos, BedrockXpOrbParticles source
   ) {
      if (trail.size() >= 2) {
         XpOrbEffects.TrailPoint[] points = trail.toArray(XpOrbEffects.TrailPoint[]::new);

         for (int index = 0; index < points.length - 1; index++) {
            XpOrbEffects.TrailPoint first = points[index];
            XpOrbEffects.TrailPoint second = points[index + 1];
            Vec3 direction = second.position.subtract(first.position);
            if (!(direction.lengthSqr() < 1.0E-8)) {
               Vec3 firstSide = ribbonSide(points, index, cameraPos, taperedWidth(first.width, index, points.length));
               Vec3 secondSide = ribbonSide(points, index + 1, cameraPos, taperedWidth(second.width, index + 1, points.length));
               if (firstSide != null && secondSide != null) {
                  float firstAge = (now - first.createdAt) / 20.0F;
                  float secondAge = (now - second.createdAt) / 20.0F;
                  BedrockXpOrbDefinition.Vec3 colour = source.trailColour(first.sourceSpawnAge, firstAge);
                  float opaqueU = 0.5F;
                  float opaqueV = 0.5F;
                  emitQuad(
                     vertices,
                     first.position.subtract(firstSide).subtract(cameraPos),
                     first.position.add(firstSide).subtract(cameraPos),
                     second.position.add(secondSide).subtract(cameraPos),
                     second.position.subtract(secondSide).subtract(cameraPos),
                     channel(colour.x()),
                     channel(colour.y()),
                     channel(colour.z()),
                     255,
                     opaqueU,
                     opaqueV,
                     opaqueU,
                     opaqueV
                  );
               }
            }
         }

         renderTrailTip(vertices, points, now, cameraPos, source);
      }
   }

   private static void renderTrailTip(VertexConsumer vertices, XpOrbEffects.TrailPoint[] points, float now, Vec3 cameraPos, BedrockXpOrbParticles source) {
      XpOrbEffects.TrailPoint tail = points[0];
      XpOrbEffects.TrailPoint next = points[1];
      Vec3 backward = tail.position.subtract(next.position);
      if (!(backward.lengthSqr() < 1.0E-8)) {
         Vec3 side = ribbonSide(points, 0, cameraPos, taperedWidth(tail.width, 0, points.length));
         if (side != null) {
            float age = (now - tail.createdAt) / 20.0F;
            BedrockXpOrbDefinition.Vec3 colour = source.trailColour(tail.sourceSpawnAge, age);
            float length = source.trailLength((float)tail.position.distanceTo(next.position), tail.frame);
            Vec3 tip = tail.position.add(backward.normalize().scale(length));
            Vec3 narrowSide = side.scale(0.06);
            emitQuad(
               vertices,
               tip.subtract(narrowSide).subtract(cameraPos),
               tip.add(narrowSide).subtract(cameraPos),
               tail.position.add(side).subtract(cameraPos),
               tail.position.subtract(side).subtract(cameraPos),
               channel(colour.x()),
               channel(colour.y()),
               channel(colour.z()),
               255,
               0.0F,
               0.0F,
               1.0F,
               1.0F
            );
         }
      }
   }

   private static Vec3 ribbonSide(XpOrbEffects.TrailPoint[] points, int index, Vec3 cameraPos, float width) {
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

      Vec3 view = points[index].position.subtract(cameraPos);
      Vec3 side = tangent.normalize().cross(view);
      return side.lengthSqr() < 1.0E-8 ? null : side.normalize().scale(width * 0.5F);
   }

   private static float taperedWidth(float sourceWidth, int index, int pointCount) {
      if (pointCount <= 1) {
         return sourceWidth;
      }

      float progress = (float)index / (pointCount - 1);
      return sourceWidth * (0.12F + 0.88F * progress);
   }

   private static void renderCameraFacingQuad(
      VertexConsumer vertices,
      Vec3 position,
      Vec3 cameraPos,
      float width,
      float height,
      int red,
      int green,
      int blue,
      int alpha,
      float minU,
      float minV,
      float maxU,
      float maxV
   ) {
      Vec3 towardCamera = cameraPos.subtract(position).normalize();
      Vec3 right = new Vec3(0.0, 1.0, 0.0).cross(towardCamera);
      if (right.lengthSqr() < 1.0E-8) {
         right = new Vec3(1.0, 0.0, 0.0);
      } else {
         right = right.normalize();
      }

      Vec3 up = towardCamera.cross(right).normalize();
      Vec3 halfRight = right.scale(width * 0.5F);
      Vec3 halfUp = up.scale(height * 0.5F);
      emitQuad(
         vertices,
         position.subtract(halfRight).subtract(halfUp).subtract(cameraPos),
         position.add(halfRight).subtract(halfUp).subtract(cameraPos),
         position.add(halfRight).add(halfUp).subtract(cameraPos),
         position.subtract(halfRight).add(halfUp).subtract(cameraPos),
         red,
         green,
         blue,
         alpha,
         minU,
         minV,
         maxU,
         maxV
      );
   }

   private static void emitQuad(
      VertexConsumer vertices,
      Vec3 bottomLeft,
      Vec3 bottomRight,
      Vec3 topRight,
      Vec3 topLeft,
      int red,
      int green,
      int blue,
      int alpha,
      float minU,
      float minV,
      float maxU,
      float maxV
   ) {
      vertex(vertices, bottomLeft, red, green, blue, alpha, minU, minV);
      vertex(vertices, bottomRight, red, green, blue, alpha, maxU, minV);
      vertex(vertices, topRight, red, green, blue, alpha, maxU, maxV);
      vertex(vertices, topLeft, red, green, blue, alpha, minU, maxV);
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

   private static Vec3 positionOf(ExperienceOrb orb) {
      return new Vec3(orb.getX(), orb.getY(), orb.getZ());
   }

   private static final class OrbEmitterState {
      private Vec3 previousPosition;
      private float lastTrailSourceAge = Float.NaN;
      private double sparkRemainder;
      private double trailRemainder;
      private final ArrayDeque<XpOrbEffects.Spark> sparks = new ArrayDeque<>();
      private final ArrayDeque<XpOrbEffects.TrailPoint> trail = new ArrayDeque<>();

      private OrbEmitterState(Vec3 previousPosition) {
         this.previousPosition = previousPosition;
      }

      private void tickSparks(BedrockXpOrbParticles source) {
         Iterator<XpOrbEffects.Spark> iterator = this.sparks.iterator();

         while (iterator.hasNext()) {
            XpOrbEffects.Spark spark = iterator.next();
            spark.age += 0.05F;
            if (spark.age >= spark.lifetime) {
               iterator.remove();
            } else {
               BedrockXpOrbDefinition.Vec3 acceleration = source.sparkAcceleration(spark.frame);
               spark.velocity = spark.velocity.add(acceleration.x() / 400.0F, acceleration.y() / 400.0F, acceleration.z() / 400.0F);
               spark.position = spark.position.add(spark.velocity);
            }
         }
      }

      private void removeExpiredTrail(long now) {
         Iterator<XpOrbEffects.TrailPoint> iterator = this.trail.iterator();

         while (iterator.hasNext()) {
            XpOrbEffects.TrailPoint point = iterator.next();
            if (((float)now - point.createdAt) / 20.0F > point.lifetime) {
               iterator.remove();
            }
         }
      }

      private boolean isEmpty() {
         return this.sparks.isEmpty() && this.trail.isEmpty();
      }
   }

   private static final class Spark {
      private Vec3 position;
      private Vec3 velocity;
      private float age;
      private final float lifetime;
      private final float frame;

      private Spark(Vec3 position, Vec3 velocity, float age, float lifetime, float frame) {
         this.position = position;
         this.velocity = velocity;
         this.age = age;
         this.lifetime = lifetime;
         this.frame = frame;
      }
   }

   private record TrailPoint(Vec3 position, float createdAt, float sourceSpawnAge, float interval, float width, float lifetime, float frame) {
   }
}
