package net.orevilleport.runparticlesonly.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelPotionEventMixin {
   @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
   private void runParticlesOnly$suppressVanillaPotionBurst(Entity source, int eventId, BlockPos pos, int data, CallbackInfo ci) {
      if (eventId == 2002 || eventId == 2007) {
         ClientLevel world = (ClientLevel)this;
         boolean potionImpact = !world.getEntities(
               EntityTypeTest.forClass(AbstractThrownPotion.class),
               new AABB(pos).inflate(2.0),
               potion -> potion instanceof ThrownSplashPotion || potion instanceof ThrownLingeringPotion
            )
            .isEmpty();
         if (potionImpact) {
            world.playLocalSound(pos, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
            ci.cancel();
         }
      }
   }
}
