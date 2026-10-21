package net.orevilleport.runparticlesonly;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.resources.Identifier;

public final class BedrockXpOrbDefinition {
   private static final String ROOT = "assets/run_particles_only/bedrock/xp_orb/";
   private static final BedrockXpOrbDefinition INSTANCE = load();
   private final JsonObject entity;
   private final JsonObject renderControllers;
   private final JsonObject animations;
   private final JsonObject particleController;
   private final JsonObject particles;
   private final JsonObject trailEvents;
   private final Map<String, JsonObject> geometries;
   private final List<BedrockXpOrbDefinition.RenderPass> renderPasses;

   private BedrockXpOrbDefinition(
      JsonObject entity, JsonObject renderControllers, JsonObject animations, JsonObject particleController, JsonObject particles, JsonObject trailEvents
   ) {
      this.entity = entity.getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
      this.renderControllers = renderControllers.getAsJsonObject("render_controllers");
      this.animations = animations.getAsJsonObject("animations");
      this.particleController = particleController.getAsJsonObject("animation_controllers");
      this.particles = particles.getAsJsonObject("particle_effects");
      this.trailEvents = trailEvents;
      this.geometries = this.readGeometries();
      this.renderPasses = this.readRenderPasses();
   }

   public static BedrockXpOrbDefinition get() {
      return INSTANCE;
   }

   public List<BedrockXpOrbDefinition.RenderPass> renderPasses() {
      return this.renderPasses;
   }

   public JsonObject geometry(String identifier) {
      JsonObject geometry = this.geometries.get(identifier);
      if (geometry == null) {
         throw new IllegalArgumentException("Missing Bedrock XP geometry: " + identifier);
      } else {
         return geometry;
      }
   }

   public JsonObject particle(String identifier) {
      JsonObject effect = this.particles.getAsJsonObject(identifier);
      if (effect == null) {
         throw new IllegalArgumentException("Missing Bedrock XP particle: " + identifier);
      } else {
         return effect;
      }
   }

   public JsonObject particleEvent(String effectIdentifier, String eventName) {
      JsonObject events = this.particle(effectIdentifier).getAsJsonObject("events");
      if (events != null && events.has(eventName)) {
         return events.getAsJsonObject(eventName);
      } else {
         JsonObject storedEvents = this.trailEvents.getAsJsonObject(effectIdentifier);
         if (storedEvents != null && storedEvents.has(eventName)) {
            return storedEvents.getAsJsonObject(eventName);
         } else {
            throw new IllegalArgumentException("Missing Bedrock XP particle event: " + effectIdentifier + "/" + eventName);
         }
      }
   }

   public List<String> controllerParticleEffects() {
      List<String> effects = new ArrayList<>();

      for (Entry<String, JsonElement> entry : this.particleController.entrySet()) {
         JsonObject states = entry.getValue().getAsJsonObject().getAsJsonObject("states");

         for (Entry<String, JsonElement> state : states.entrySet()) {
            JsonArray particleEffects = object(state.getValue()).getAsJsonArray("particle_effects");
            if (particleEffects != null) {
               for (JsonElement effect : particleEffects) {
                  String alias = object(effect).get("effect").getAsString();
                  JsonElement effectId = this.entity.getAsJsonObject("particle_effects").get(alias);
                  if (effectId != null) {
                     effects.add(effectId.getAsString());
                  }
               }
            }
         }
      }

      return List.copyOf(effects);
   }

   public String controllerPreEffectScript(String effectIdentifier) {
      JsonObject aliases = this.entity.getAsJsonObject("particle_effects");

      for (Entry<String, JsonElement> controller : this.particleController.entrySet()) {
         JsonObject states = controller.getValue().getAsJsonObject().getAsJsonObject("states");

         for (Entry<String, JsonElement> state : states.entrySet()) {
            JsonArray effects = object(state.getValue()).getAsJsonArray("particle_effects");
            if (effects != null) {
               for (JsonElement effect : effects) {
                  JsonObject configured = object(effect);
                  JsonElement resolved = aliases.get(configured.get("effect").getAsString());
                  if (resolved != null && effectIdentifier.equals(resolved.getAsString())) {
                     JsonElement script = configured.get("pre_effect_script");
                     return script == null ? "" : script.getAsString();
                  }
               }
            }
         }
      }

      return "";
   }

   public BedrockXpOrbDefinition.Pose poseForBone(String boneName, BedrockXpOrbDefinition.EvalContext context) {
      BedrockXpOrbDefinition.Vec3 position = BedrockXpOrbDefinition.Vec3.ZERO;
      BedrockXpOrbDefinition.Vec3 rotation = BedrockXpOrbDefinition.Vec3.ZERO;
      BedrockXpOrbDefinition.Vec3 scale = BedrockXpOrbDefinition.Vec3.ONE;
      JsonArray animate = this.entity.getAsJsonObject("scripts").getAsJsonArray("animate");
      JsonObject aliases = this.entity.getAsJsonObject("animations");

      for (JsonElement animationAlias : animate) {
         String alias = animationAlias.getAsString();
         JsonElement animationName = aliases.get(alias);
         if (animationName != null && animationName.isJsonPrimitive()) {
            JsonObject animation = this.animations.getAsJsonObject(animationName.getAsString());
            if (animation != null && animation.has("bones")) {
               JsonObject bone = animation.getAsJsonObject("bones").getAsJsonObject(boneName);
               if (bone != null) {
                  if (bone.has("position")) {
                     position = vector(bone.get("position"), context);
                  }

                  if (bone.has("rotation")) {
                     rotation = vector(bone.get("rotation"), context);
                  }

                  if (bone.has("scale")) {
                     scale = vector(bone.get("scale"), context);
                  }
               }
            }
         }
      }

      return new BedrockXpOrbDefinition.Pose(position, rotation, scale);
   }

   public BedrockXpOrbDefinition.RenderValues renderValues(BedrockXpOrbDefinition.RenderPass pass, BedrockXpOrbDefinition.EvalContext context) {
      JsonObject controller = this.renderControllers.getAsJsonObject(pass.controllerId());
      JsonObject overlay = controller.getAsJsonObject("overlay_color");
      BedrockXpOrbDefinition.Vec3 colour = new BedrockXpOrbDefinition.Vec3(
         evaluate(overlay.get("r"), context), evaluate(overlay.get("g"), context), evaluate(overlay.get("b"), context)
      );
      float alpha = evaluate(overlay.get("a"), context);
      BedrockXpOrbDefinition.Vec3 uvOffset = BedrockXpOrbDefinition.Vec3.ZERO;
      if (controller.has("uv_anim")) {
         uvOffset = vector(controller.getAsJsonObject("uv_anim").get("offset"), context);
      }

      return new BedrockXpOrbDefinition.RenderValues(colour, alpha, uvOffset);
   }

   public boolean hasCameraRelativeBone(String geometryIdentifier) {
      JsonArray geometryBones = this.geometry(geometryIdentifier).getAsJsonArray("bones");
      JsonObject aliases = this.entity.getAsJsonObject("animations");

      for (JsonElement animationAlias : this.entity.getAsJsonObject("scripts").getAsJsonArray("animate")) {
         JsonElement name = aliases.get(animationAlias.getAsString());
         JsonObject animation = name == null ? null : this.animations.getAsJsonObject(name.getAsString());
         if (animation != null && animation.has("bones")) {
            JsonObject animatedBones = animation.getAsJsonObject("bones");

            for (JsonElement geometryBone : geometryBones) {
               String boneName = object(geometryBone).get("name").getAsString();
               JsonObject animatedBone = animatedBones.getAsJsonObject(boneName);
               if (animatedBone != null && animatedBone.toString().contains("q.rotation_to_camera")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean hasEntityRelativeRotation(String boneName) {
      JsonObject aliases = this.entity.getAsJsonObject("animations");

      for (JsonElement animationAlias : this.entity.getAsJsonObject("scripts").getAsJsonArray("animate")) {
         JsonElement name = aliases.get(animationAlias.getAsString());
         JsonObject animation = name == null ? null : this.animations.getAsJsonObject(name.getAsString());
         if (animation != null && animation.has("bones")) {
            JsonObject bone = animation.getAsJsonObject("bones").getAsJsonObject(boneName);
            if (bone != null && bone.has("relative_to")) {
               JsonObject relativeTo = bone.getAsJsonObject("relative_to");
               JsonElement rotation = relativeTo.get("rotation");
               if (rotation != null && "entity".equals(rotation.getAsString())) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static BedrockXpOrbDefinition.Vec3 vector(JsonElement element, BedrockXpOrbDefinition.EvalContext context) {
      JsonArray array = element.getAsJsonArray();
      return new BedrockXpOrbDefinition.Vec3(
         evaluate(array.get(0), context), evaluate(array.get(1), context), array.size() > 2 ? evaluate(array.get(2), context) : 0.0F
      );
   }

   public static float evaluate(JsonElement element, BedrockXpOrbDefinition.EvalContext context) {
      if (element == null || element.isJsonNull()) {
         return 0.0F;
      } else {
         return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsFloat() : Molang.evaluate(element.getAsString(), context);
      }
   }

   private Map<String, JsonObject> readGeometries() {
      JsonObject modelRoot = read("models.json");
      Map<String, JsonObject> values = new LinkedHashMap<>();

      for (JsonElement geometry : modelRoot.getAsJsonArray("minecraft:geometry")) {
         JsonObject value = object(geometry);
         values.put(value.getAsJsonObject("description").get("identifier").getAsString(), value);
      }

      return Map.copyOf(values);
   }

   private List<BedrockXpOrbDefinition.RenderPass> readRenderPasses() {
      JsonObject geometryAliases = this.entity.getAsJsonObject("geometry");
      JsonObject textureAliases = this.entity.getAsJsonObject("textures");
      List<BedrockXpOrbDefinition.RenderPass> values = new ArrayList<>();

      for (JsonElement configuredController : this.entity.getAsJsonArray("render_controllers")) {
         String controllerId = configuredController.getAsString();
         JsonObject controller = this.renderControllers.getAsJsonObject(controllerId);
         String geometryAlias = controller.get("geometry").getAsString().replace("Geometry.", "");
         JsonArray textures = controller.getAsJsonArray("textures");
         String textureAlias = textures.get(0).getAsString().replace("Texture.", "");
         JsonObject materialRule = controller.getAsJsonArray("materials").get(0).getAsJsonObject();
         String materialAlias = materialRule.get("*").getAsString().replace("Material.", "");
         values.add(
            new BedrockXpOrbDefinition.RenderPass(
               controllerId,
               geometryAliases.get(geometryAlias).getAsString(),
               toTextureIdentifier(textureAliases.get(textureAlias).getAsString()),
               this.entity.getAsJsonObject("materials").get(materialAlias).getAsString()
            )
         );
      }

      return List.copyOf(values);
   }

   private static Identifier toTextureIdentifier(String bedrockTexturePath) {
      String path = bedrockTexturePath.startsWith("textures/") ? bedrockTexturePath : "textures/" + bedrockTexturePath;
      return Identifier.fromNamespaceAndPath("run_particles_only", path + (path.endsWith(".png") ? "" : ".png"));
   }

   private static BedrockXpOrbDefinition load() {
      return new BedrockXpOrbDefinition(
         read("entity.json"),
         read("render_controllers.json"),
         read("animations.json"),
         read("particle_controller.json"),
         read("particles.json"),
         read("trail_events.json")
      );
   }

   private static JsonObject read(String file) {
      try (InputStream stream = BedrockXpOrbDefinition.class.getClassLoader().getResourceAsStream("assets/run_particles_only/bedrock/xp_orb/" + file)) {
         if (stream == null) {
            throw new IllegalStateException("Missing embedded Bedrock XP definition: " + file);
         } else {
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         }
      } catch (IOException exception) {
         throw new IllegalStateException("Unable to read embedded Bedrock XP definition: " + file, exception);
      }
   }

   private static JsonObject object(JsonElement element) {
      return element.getAsJsonObject();
   }

   public record EvalContext(
      float lifeTime,
      float textureFrameIndex,
      float phase,
      float particleAge,
      float particleLifetime,
      float verticalSpeed,
      float random,
      Map<String, Float> variables
   ) {
      public EvalContext(float lifeTime, float textureFrameIndex, float phase, float particleAge, float particleLifetime, float verticalSpeed, float random) {
         this(lifeTime, textureFrameIndex, phase, particleAge, particleLifetime, verticalSpeed, random, Map.of());
      }

      public static BedrockXpOrbDefinition.EvalContext entity(float lifeTime, float textureFrameIndex, float phase) {
         return new BedrockXpOrbDefinition.EvalContext(lifeTime, textureFrameIndex, phase, 0.0F, 0.0F, 0.0F, 0.5F);
      }

      public static BedrockXpOrbDefinition.EvalContext zero() {
         return entity(0.0F, 0.0F, 0.0F);
      }

      float variable(String name) {
         Float override = this.variables.get(name);
         if (override != null) {
            return override;
         }

         return switch (name) {
            case "q.life_time" -> this.lifeTime;
            case "q.texture_frame_index" -> this.textureFrameIndex;
            case "v.ngjrwv" -> this.phase;
            case "v.particle_age" -> this.particleAge;
            case "v.particle_lifetime" -> this.particleLifetime;
            case "v.emitter_age" -> this.lifeTime;
            case "q.vertical_speed" -> this.verticalSpeed;
            default -> 0.0F;
         };
      }

      public BedrockXpOrbDefinition.EvalContext withVariable(String name, float value) {
         Map<String, Float> copy = new LinkedHashMap<>(this.variables);
         copy.put(name, value);
         return new BedrockXpOrbDefinition.EvalContext(
            this.lifeTime, this.textureFrameIndex, this.phase, this.particleAge, this.particleLifetime, this.verticalSpeed, this.random, Map.copyOf(copy)
         );
      }
   }

   public record Pose(BedrockXpOrbDefinition.Vec3 position, BedrockXpOrbDefinition.Vec3 rotation, BedrockXpOrbDefinition.Vec3 scale) {
   }

   public record RenderPass(String controllerId, String geometryId, Identifier texture, String material) {
   }

   public record RenderValues(BedrockXpOrbDefinition.Vec3 colour, float alpha, BedrockXpOrbDefinition.Vec3 uvOffset) {
   }

   public record Vec3(float x, float y, float z) {
      public static final BedrockXpOrbDefinition.Vec3 ZERO = new BedrockXpOrbDefinition.Vec3(0.0F, 0.0F, 0.0F);
      public static final BedrockXpOrbDefinition.Vec3 ONE = new BedrockXpOrbDefinition.Vec3(1.0F, 1.0F, 1.0F);
   }
}
