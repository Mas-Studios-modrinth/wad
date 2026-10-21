package net.orevilleport.runparticlesonly.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.Items;
import net.orevilleport.runparticlesonly.render.ActionsStuffThrowableRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownItemRenderer.class)
public abstract class ThrownItemRendererMixin {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void runParticlesOnly$captureThrowable(Entity entity, ThrownItemRenderState state, float tickProgress, CallbackInfo ci) {
      if (entity instanceof ItemSupplier itemSupplier) {
         ActionsStuffThrowableRenderer.capture(entity, state, itemSupplier.getItem(), tickProgress);
      } else if (entity instanceof EyeOfEnder) {
         ActionsStuffThrowableRenderer.capture(entity, state, Items.ENDER_EYE.getDefaultInstance(), tickProgress);
      }
   }

   @Redirect(
      method = "submit",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
      )
   )
   private void runParticlesOnly$renderThrowable(
      ItemStackRenderState itemState,
      PoseStack matrices,
      SubmitNodeCollector queue,
      int light,
      int overlay,
      int outlineColor,
      ThrownItemRenderState state,
      PoseStack ignoredMatrices,
      SubmitNodeCollector ignoredQueue,
      CameraRenderState ignoredCamera
   ) {
      if (!ActionsStuffThrowableRenderer.render(state, matrices, queue, light, ignoredCamera)) {
         itemState.submit(matrices, queue, light, overlay, outlineColor);
      }
   }
}
