package net.orevilleport.runparticlesonly.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aleca.actionsstufffp.animation.BedrockPose;
import dev.aleca.actionsstufffp.render.BedrockGeometry;
import dev.aleca.actionsstufffp.runtime.RenderControllerResolver.RenderPass;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.orevilleport.runparticlesonly.ToolTrailEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.aleca.actionsstufffp.render.FirstPersonRenderer", remap = false, priority = 700)
public abstract class FirstPersonToolTrailMixin {
   @Inject(
      method = "renderAttachedItem",
      at = @At(
         value = "INVOKE",
         target = "Ldev/aleca/actionsstufffp/render/BedrockGeometry;applyFirstPersonBoneTransform(Ljava/lang/String;Lcom/mojang/blaze3d/vertex/PoseStack;Ldev/aleca/actionsstufffp/animation/BedrockPose;)Ljava/util/List;",
         shift = Shift.AFTER,
         remap = false
      ),
      require = 0,
      remap = false
   )
   private static void run_particles_only$captureFirstPersonTool(
      FirstPersonHandsAndItemsRenderer vanillaRenderer,
      PoseStack matrices,
      SubmitNodeCollector queue,
      LocalPlayer player,
      int light,
      BedrockGeometry arms,
      String slotBone,
      ItemStack stack,
      List<RenderPass> renderPasses,
      BedrockPose playerPose,
      BedrockPose itemPose,
      ItemDisplayContext fallbackContext,
      boolean reverseWinding,
      boolean flintAndSteelLeftAssist,
      CallbackInfo ci
   ) {
      ToolTrailEffects.captureFirstPersonTool(player, stack, slotBone, matrices);
   }
}
