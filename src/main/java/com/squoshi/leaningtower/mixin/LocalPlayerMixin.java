package com.squoshi.leaningtower.mixin;

import com.squoshi.leaningtower.client.ClientLeaningData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningtower$move(MoverType pType, Vec3 pPos, CallbackInfo ci) {
        if (pType == MoverType.SELF) {
            if (ClientLeaningData.incrementalLeanAngle != 0) {
                ci.cancel();
            }
        }
    }
}
