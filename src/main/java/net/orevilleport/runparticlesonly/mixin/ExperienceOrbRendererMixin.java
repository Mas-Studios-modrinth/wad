package net.orevilleport.runparticlesonly.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.ExperienceOrb;
import net.orevilleport.runparticlesonly.ActionsStuffXpOrbRenderState;
import net.orevilleport.runparticlesonly.ActionsStuffXpOrbRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbRenderer.class)
public class ExperienceOrbRendererMixin {
   protected float getShadowRadius(ExperienceOrbRenderState state) {
      return 0.0F;
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void runParticlesOnly$captureOrbPhase(ExperienceOrb entity, ExperienceOrbRenderState state, float tickProgress, CallbackInfo ci) {
      ActionsStuffXpOrbRenderState runtimeState = (ActionsStuffXpOrbRenderState)state;
      runtimeState.runParticlesOnly$setOrbId(entity.getId());
      runtimeState.runParticlesOnly$setGroundSpeed((float)entity.getDeltaMovement().horizontalDistance());
      runtimeState.runParticlesOnly$setRenderPosition(entity.getPosition(tickProgress));
      state.shadowRadius = 0.0F;
      state.shadowPieces.clear();
   }

   @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
   private void runParticlesOnly$renderActionsStuffOrb(
      ExperienceOrbRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera, CallbackInfo ci
   ) {
      state.shadowRadius = 0.0F;
      state.shadowPieces.clear();
      ActionsStuffXpOrbRenderer.render(state, matrices, queue, camera);
      ci.cancel();
   }
}
