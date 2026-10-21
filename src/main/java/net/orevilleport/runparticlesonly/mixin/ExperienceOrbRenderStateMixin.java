package net.orevilleport.runparticlesonly.mixin;

import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.world.phys.Vec3;
import net.orevilleport.runparticlesonly.ActionsStuffXpOrbRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ExperienceOrbRenderState.class)
public class ExperienceOrbRenderStateMixin implements ActionsStuffXpOrbRenderState {
   @Unique
   private int runParticlesOnly$orbId = Integer.MIN_VALUE;
   @Unique
   private float runParticlesOnly$orbPhase;
   @Unique
   private float runParticlesOnly$groundSpeed;
   @Unique
   private Vec3 runParticlesOnly$renderPosition = Vec3.ZERO;

   @Override
   public void runParticlesOnly$setOrbId(int entityId) {
      if (this.runParticlesOnly$orbId != entityId) {
         this.runParticlesOnly$orbId = entityId;
         int hash = entityId * 73244475;
         hash ^= hash >>> 16;
         this.runParticlesOnly$orbPhase = Math.floorMod(hash, 36000) / 100.0F;
      }
   }

   @Override
   public float runParticlesOnly$getOrbPhase() {
      return this.runParticlesOnly$orbPhase;
   }

   @Override
   public void runParticlesOnly$setGroundSpeed(float speed) {
      this.runParticlesOnly$groundSpeed = speed;
   }

   @Override
   public float runParticlesOnly$getGroundSpeed() {
      return this.runParticlesOnly$groundSpeed;
   }

   @Override
   public void runParticlesOnly$setRenderPosition(Vec3 position) {
      this.runParticlesOnly$renderPosition = position;
   }

   @Override
   public Vec3 runParticlesOnly$getRenderPosition() {
      return this.runParticlesOnly$renderPosition;
   }
}
