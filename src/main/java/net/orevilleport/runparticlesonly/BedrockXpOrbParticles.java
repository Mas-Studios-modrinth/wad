package net.orevilleport.runparticlesonly;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BedrockXpOrbParticles {
   private static final BedrockXpOrbParticles INSTANCE = new BedrockXpOrbParticles(BedrockXpOrbDefinition.get());
   private final BedrockXpOrbDefinition definition;
   private final String sparkId;
   private final String auraId;
   private final String trailEmitterId;
   private final String trailId;

   private BedrockXpOrbParticles(BedrockXpOrbDefinition definition) {
      this.definition = definition;
      String spark = null;
      String aura = null;
      String trailEmitter = null;

      for (String effect : definition.controllerParticleEffects()) {
         JsonObject components = this.components(effect);
         if (components.has("minecraft:emitter_shape_disc")) {
            spark = effect;
         } else if (this.material(effect).equals("particles_add")) {
            aura = effect;
         } else if (components.has("minecraft:particle_lifetime_events")) {
            trailEmitter = effect;
         }
      }

      this.sparkId = required(spark, "spark");
      this.auraId = required(aura, "aura");
      this.trailEmitterId = required(trailEmitter, "trail emitter");
      this.trailId = this.childTrailEffect(this.trailEmitterId);
   }

   public static BedrockXpOrbParticles get() {
      return INSTANCE;
   }

   public float sparkRate() {
      return this.value(this.sparkId, "minecraft:emitter_rate_steady", "spawn_rate", BedrockXpOrbDefinition.EvalContext.zero());
   }

   public int sparkMaximum() {
      return Math.round(this.value(this.sparkId, "minecraft:emitter_rate_steady", "max_particles", BedrockXpOrbDefinition.EvalContext.zero()));
   }

   public BedrockXpOrbDefinition.Vec3 sparkOffset(float frame) {
      return this.vector(this.component(this.sparkId, "minecraft:emitter_shape_disc").get("offset"), context(frame));
   }

   public float sparkRadius(float frame) {
      return this.evaluate(this.component(this.sparkId, "minecraft:emitter_shape_disc").get("radius"), context(frame));
   }

   public float sparkLifetime(float frame, float random) {
      return this.evaluate(this.component(this.sparkId, "minecraft:particle_lifetime_expression").get("max_lifetime"), context(frame, random));
   }

   public float sparkInitialSpeed(float frame, float random) {
      return this.evaluate(this.components(this.sparkId).get("minecraft:particle_initial_speed"), context(frame, random));
   }

   public BedrockXpOrbDefinition.Vec3 sparkAcceleration(float frame) {
      return this.vector(this.component(this.sparkId, "minecraft:particle_motion_dynamic").get("linear_acceleration"), context(frame));
   }

   public float sparkScale(float frame, float particleAge, float particleLifetime) {
      JsonElement size = this.component(this.sparkId, "minecraft:particle_appearance_billboard").getAsJsonArray("size").get(0);
      float progress = particleLifetime <= 0.0F ? 0.0F : particleAge / particleLifetime;
      JsonArray nodes = this.effect(this.sparkId).getAsJsonObject("curves").getAsJsonObject("variable.scale").getAsJsonArray("nodes");
      float curve = linearCurve(nodes, progress);
      return this.evaluate(
         size, new BedrockXpOrbDefinition.EvalContext(0.0F, frame, 0.0F, particleAge, particleLifetime, 0.0F, 0.5F).withVariable("v.scale", curve)
      );
   }

   public BedrockXpOrbDefinition.Vec3 sparkColour(float frame, float particleAge, float particleLifetime) {
      return this.colour(
         this.component(this.sparkId, "minecraft:particle_appearance_tinting"),
         new BedrockXpOrbDefinition.EvalContext(0.0F, frame, 0.0F, particleAge, particleLifetime, 0.0F, 0.5F)
      );
   }

   public float auraSize(float frame, float verticalSpeed) {
      JsonElement size = this.component(this.auraId, "minecraft:particle_appearance_billboard").getAsJsonArray("size").get(0);
      return this.evaluate(size, new BedrockXpOrbDefinition.EvalContext(0.0F, frame, 0.0F, 0.0F, 0.0F, verticalSpeed, 0.5F));
   }

   public BedrockXpOrbDefinition.Vec3 auraOffset(float frame) {
      return this.vector(this.component(this.auraId, "minecraft:emitter_shape_point").get("offset"), context(frame));
   }

   public BedrockXpOrbDefinition.Vec3 auraColour(float particleAge) {
      return this.colour(
         this.component(this.auraId, "minecraft:particle_appearance_tinting"),
         new BedrockXpOrbDefinition.EvalContext(0.0F, 0.0F, 0.0F, particleAge, 0.0F, 0.0F, 0.5F)
      );
   }

   public float auraAlpha(float lifeTime) {
      JsonArray colour = this.component(this.auraId, "minecraft:particle_appearance_tinting").getAsJsonArray("color");
      return this.evaluate(colour.get(3), BedrockXpOrbDefinition.EvalContext.entity(lifeTime, 0.0F, 0.0F));
   }

   public float trailEmissionRate() {
      return this.value(this.trailEmitterId, "minecraft:emitter_rate_steady", "spawn_rate", BedrockXpOrbDefinition.EvalContext.zero());
   }

   public float trailInterval() {
      return assignment(
         this.component(this.trailEmitterId, "minecraft:emitter_initialization").get("creation_expression").getAsString(), "v.hrsqwn", context(0.0F)
      );
   }

   public float trailYOffset(float frame) {
      return assignment(this.controllerPreEffectScript(this.trailEmitterId), "v.vimpib", context(frame));
   }

   public float trailWidth(float frame) {
      return this.evaluate(this.component(this.trailId, "minecraft:particle_appearance_billboard").getAsJsonArray("size").get(0), context(frame));
   }

   public float trailLength(float distance, float frame) {
      return assignment(this.trailSpawnScript(), "v.cfglpd", contextWith(Map.of("t.kgp", distance), frame));
   }

   public float trailLifetime(float frame) {
      float interval = this.trailInterval();
      return this.evaluate(
         this.component(this.trailId, "minecraft:particle_lifetime_expression").get("max_lifetime"), contextWith(Map.of("v.hrsqwn", interval), frame)
      );
   }

   public BedrockXpOrbDefinition.Vec3 trailColour(float spawnAge, float emitterAge) {
      return this.colour(
         this.component(this.trailId, "minecraft:particle_appearance_tinting"), contextWith(Map.of("v.tlnhrs", spawnAge, "v.emitter_age", emitterAge), 0.0F)
      );
   }

   public float trailUvHeight(float sampleInterval) {
      JsonElement size = this.component(this.trailId, "minecraft:particle_appearance_billboard").getAsJsonObject("uv").get("uv_size").getAsJsonArray().get(1);
      return this.evaluate(size, contextWith(Map.of("v.sbiadh", sampleInterval), 0.0F));
   }

   public float trailUvStart(float sampleInterval, float particleAge) {
      JsonElement start = this.component(this.trailId, "minecraft:particle_appearance_billboard").getAsJsonObject("uv").getAsJsonArray("uv").get(1);
      return this.evaluate(start, contextWith(Map.of("v.sbiadh", sampleInterval, "v.particle_age", particleAge, "v.hrsqwn", sampleInterval), 0.0F));
   }

   private String childTrailEffect(String parent) {
      String eventName = this.component(parent, "minecraft:particle_lifetime_events").get("expiration_event").getAsString();
      JsonObject definition = this.definition.particleEvent(parent, eventName);

      for (JsonElement entry : definition.getAsJsonArray("sequence")) {
         JsonObject child = entry.getAsJsonObject().getAsJsonObject("particle_effect");
         if (child != null) {
            return child.get("effect").getAsString();
         }
      }

      throw new IllegalStateException("Imported Bedrock XP trail emitter has no child particle");
   }

   private String trailSpawnScript() {
      String eventName = this.component(this.trailEmitterId, "minecraft:particle_lifetime_events").get("expiration_event").getAsString();
      JsonObject definition = this.definition.particleEvent(this.trailEmitterId, eventName);

      for (JsonElement entry : definition.getAsJsonArray("sequence")) {
         JsonObject child = entry.getAsJsonObject().getAsJsonObject("particle_effect");
         if (child != null) {
            return child.get("pre_effect_expression").getAsString();
         }
      }

      return "";
   }

   private String controllerPreEffectScript(String effectId) {
      return this.definition.controllerPreEffectScript(effectId);
   }

   private JsonObject effect(String id) {
      return this.definition.particle(id);
   }

   private JsonObject components(String id) {
      return this.effect(id).getAsJsonObject("components");
   }

   private JsonObject component(String id, String name) {
      return this.components(id).getAsJsonObject(name);
   }

   private String material(String id) {
      return this.effect(id).getAsJsonObject("description").getAsJsonObject("basic_render_parameters").get("material").getAsString();
   }

   private float value(String id, String component, String field, BedrockXpOrbDefinition.EvalContext context) {
      return this.evaluate(this.component(id, component).get(field), context);
   }

   private BedrockXpOrbDefinition.Vec3 vector(JsonElement element, BedrockXpOrbDefinition.EvalContext context) {
      return BedrockXpOrbDefinition.vector(element, context);
   }

   private float evaluate(JsonElement expression, BedrockXpOrbDefinition.EvalContext context) {
      return BedrockXpOrbDefinition.evaluate(expression, context);
   }

   private BedrockXpOrbDefinition.Vec3 colour(JsonObject tinting, BedrockXpOrbDefinition.EvalContext context) {
      JsonArray colour = tinting.getAsJsonArray("color");
      return new BedrockXpOrbDefinition.Vec3(
         this.evaluate(colour.get(0), context), this.evaluate(colour.get(1), context), this.evaluate(colour.get(2), context)
      );
   }

   private static float linearCurve(JsonArray nodes, float progress) {
      float clamped = Math.max(0.0F, Math.min(progress, 1.0F));
      float position = clamped * (nodes.size() - 1);
      int lower = Math.min((int)position, nodes.size() - 1);
      int upper = Math.min(lower + 1, nodes.size() - 1);
      float fraction = position - lower;
      return nodes.get(lower).getAsFloat() + (nodes.get(upper).getAsFloat() - nodes.get(lower).getAsFloat()) * fraction;
   }

   private static String required(String value, String description) {
      if (value == null) {
         throw new IllegalStateException("Missing imported Bedrock XP " + description);
      } else {
         return value;
      }
   }

   private static BedrockXpOrbDefinition.EvalContext context(float frame) {
      return BedrockXpOrbDefinition.EvalContext.entity(0.0F, frame, 0.0F);
   }

   private static BedrockXpOrbDefinition.EvalContext context(float frame, float random) {
      return new BedrockXpOrbDefinition.EvalContext(0.0F, frame, 0.0F, 0.0F, 0.0F, 0.0F, random);
   }

   private static BedrockXpOrbDefinition.EvalContext contextWith(Map<String, Float> variables, float frame) {
      return new BedrockXpOrbDefinition.EvalContext(0.0F, frame, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F, variables);
   }

   private static float assignment(String script, String variable, BedrockXpOrbDefinition.EvalContext context) {
      Matcher matcher = Pattern.compile(Pattern.quote(variable) + "\\s*=\\s*([^;]+)").matcher(script);
      if (!matcher.find()) {
         throw new IllegalStateException("Missing " + variable + " assignment in imported Bedrock XP script");
      } else {
         return Molang.evaluate(matcher.group(1), context);
      }
   }
}
