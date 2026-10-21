package net.orevilleport.runparticlesonly.mixin;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.orevilleport.runparticlesonly.ActionsStuffHurtOverlayFeatureRenderer;
import net.orevilleport.runparticlesonly.RunParticlesConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererHurtMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Shadow
   protected abstract boolean addLayer(RenderLayer<S, M> var1);

   @Shadow
   public abstract Identifier getTextureLocation(S var1);

   @Inject(method = "<init>", at = @At("RETURN"))
   private void runparticlesonly$addActionsStuffHurtOverlay(Context context, M model, float shadowRadius, CallbackInfo ci) {
      this.addLayer(new ActionsStuffHurtOverlayFeatureRenderer<>((LivingEntityRenderer)this, this::getTextureLocation));
   }

   @Redirect(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getOverlayCoords(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)I"
      )
   )
   private static int runparticlesonly$useActionsStuffHurtColour(LivingEntityRenderState state, float whiteOverlayProgress) {
      if (!state.hasRedOverlay) {
         return LivingEntityRenderer.getOverlayCoords(state, whiteOverlayProgress);
      } else {
         int effect = state instanceof AvatarRenderState ? RunParticlesConfig.get().playerHurtEffect : RunParticlesConfig.get().mobHurtEffect;
         if (effect == 1) {
            float cycle = state.ageInTicks / 5.0F - (float)Math.floor(state.ageInTicks / 5.0F);
            return OverlayTexture.pack(0.6F * (1.0F - cycle) / 0.75F, false);
         } else {
            return LivingEntityRenderer.getOverlayCoords(state, whiteOverlayProgress);
         }
      }
   }
}
