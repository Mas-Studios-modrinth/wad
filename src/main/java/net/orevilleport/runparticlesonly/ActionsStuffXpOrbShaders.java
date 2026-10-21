package net.orevilleport.runparticlesonly;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

final class ActionsStuffXpOrbShaders {
   private ActionsStuffXpOrbShaders() {
   }

   static RenderType core(Identifier texture) {
      return RenderTypes.entitySolid(texture);
   }

   static RenderType glow(Identifier texture) {
      return RenderTypes.entityTranslucentEmissive(texture);
   }

   static RenderType innerCore(Identifier texture) {
      return RenderTypes.entityCutout(texture, false);
   }
}
