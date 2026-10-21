package net.orevilleport.runparticlesonly;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

final class ActionsStuffXpOrbOverlayTexture {
   private static final String SOURCE = "assets/run_particles_only/textures/oreville/ans/bfe.png";
   private static final Map<String, Identifier> TINTED_TEXTURES = new HashMap<>();

   private ActionsStuffXpOrbOverlayTexture() {
   }

   static Identifier get(BedrockXpOrbDefinition.RenderValues values, boolean translucentMaterial) {
      return get(values, translucentMaterial, false);
   }

   static Identifier getTrailPlane(BedrockXpOrbDefinition.RenderValues values) {
      return get(values, true, true);
   }

   static Identifier getFarTrail(BedrockXpOrbDefinition.RenderValues values) {
      return get(new BedrockXpOrbDefinition.RenderValues(values.colour(), values.alpha() * 0.35F, values.uvOffset()), true, true);
   }

   private static Identifier get(BedrockXpOrbDefinition.RenderValues values, boolean translucentMaterial, boolean trailPlane) {
      int red = channel(values.colour().x());
      int green = channel(values.colour().y());
      int blue = channel(values.colour().z());
      int alpha = channel(values.alpha());
      String key = translucentMaterial + ":" + trailPlane + ":" + alpha + ":" + red + ":" + green + ":" + blue;
      return TINTED_TEXTURES.computeIfAbsent(key, ignored -> create(red, green, blue, alpha, translucentMaterial, trailPlane, key));
   }

   private static Identifier create(
      int overlayRed, int overlayGreen, int overlayBlue, int overlayAlpha, boolean translucentMaterial, boolean trailPlane, String key
   ) {
      try (InputStream stream = ActionsStuffXpOrbOverlayTexture.class
            .getClassLoader()
            .getResourceAsStream("assets/run_particles_only/textures/oreville/ans/bfe.png")) {
         if (stream == null) {
            throw new IllegalStateException("Missing original Actions & Stuff XP texture");
         }

         NativeImage source = NativeImage.read(stream);

         try {
            NativeImage tinted = new NativeImage(source.getWidth(), source.getHeight(), false);
            float blend = overlayAlpha / 255.0F;

            for (int y = 0; y < source.getHeight(); y++) {
               for (int x = 0; x < source.getWidth(); x++) {
                  int pixel = ARGB.fromABGR(source.getPixel(x, y));
                  int alpha = pixel >>> 24 & 0xFF;
                  if (translucentMaterial) {
                     alpha = trailPlane ? overlayAlpha : Math.round(alpha * blend);
                  }

                  int red = multiply(pixel >>> 16 & 0xFF, overlayRed);
                  int green = multiply(pixel >>> 8 & 0xFF, overlayGreen);
                  int blue = multiply(pixel & 0xFF, overlayBlue);
                  tinted.setPixelABGR(x, y, ARGB.toABGR(alpha << 24 | red << 16 | green << 8 | blue));
               }
            }

            Identifier id = Identifier.fromNamespaceAndPath("run_particles_only", "dynamic/xp_orb_overlay_" + key.replace(':', '_'));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Actions & Stuff XP overlay", tinted));
            return id;
         } finally {
            source.close();
         }
      } catch (IOException exception) {
         throw new IllegalStateException("Unable to tint original Actions & Stuff XP texture", exception);
      }
   }

   private static int multiply(int base, int overlay) {
      return Math.round(base * overlay / 255.0F);
   }

   private static int channel(float value) {
      return Math.round(Math.max(0.0F, Math.min(1.0F, value)) * 255.0F);
   }
}
