package dev.proclient.mixin;

import dev.proclient.module.impl.EntityEspModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes ESP targets use vanilla's glow-outline renderer, coloured per category.
 * Entity is shared with the integrated server, so we only act on the client render thread.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void proclient$glow(CallbackInfoReturnable<Boolean> cir) {
        if (EntityEspModule.hasGlowTargets() && MinecraftClient.getInstance().isOnThread()
                && EntityEspModule.glowColor(((Entity) (Object) this).getId()) != 0) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void proclient$color(CallbackInfoReturnable<Integer> cir) {
        if (EntityEspModule.hasGlowTargets() && MinecraftClient.getInstance().isOnThread()) {
            int color = EntityEspModule.glowColor(((Entity) (Object) this).getId());
            if (color != 0) cir.setReturnValue(color);
        }
    }
}
