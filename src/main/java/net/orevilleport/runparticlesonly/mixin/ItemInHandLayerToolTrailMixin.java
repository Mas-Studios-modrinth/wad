package net.orevilleport.runparticlesonly.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.orevilleport.runparticlesonly.ToolTrailEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemInHandLayer.class, priority = 700)
public abstract class ItemInHandLayerToolTrailMixin {
   @Inject(
      method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
         shift = Shift.AFTER
      ),
      require = 0
   )
   private void run_particles_only$captureAnimatedToolHand(
      ArmedEntityRenderState state,
      ItemStackRenderState itemState,
      ItemStack stack,
      HumanoidArm arm,
      PoseStack matrices,
      SubmitNodeCollector queue,
      int light,
      CallbackInfo ci
   ) {
      if (state instanceof AvatarRenderState playerState) {
         ToolTrailEffects.captureAnimatedHand(playerState, stack, arm, matrices);
      }
   }
}
