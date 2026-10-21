package net.orevilleport.runparticlesonly;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class ActionsStuffHurtOverlayFeatureRenderer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
   private static final int MOB_HURT_OVERLAY = 872349696;
   private final Function<S, Identifier> textureProvider;

   public ActionsStuffHurtOverlayFeatureRenderer(RenderLayerParent<S, M> parent, Function<S, Identifier> textureProvider) {
      super(parent);
      this.textureProvider = textureProvider;
   }

   public void submit(PoseStack matrices, SubmitNodeCollector queue, int light, S state, float limbAngle, float limbDistance) {
      if (state.hasRedOverlay && !state.isInvisible) {
         int effect = state instanceof AvatarRenderState ? RunParticlesConfig.get().playerHurtEffect : RunParticlesConfig.get().mobHurtEffect;
         int color = effect == 0 ? 872349696 : 0;
         if (color != 0) {
            queue.submitModel(
               this.getParentModel(),
               state,
               matrices,
               RenderTypes.entityTranslucent(this.textureProvider.apply(state), false),
               light,
               OverlayTexture.NO_OVERLAY,
               color
            );
         }
      }
   }
}
