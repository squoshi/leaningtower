package com.squoshi.leaningtower.mixin;

import com.squoshi.leaningtower.capability.Leaning;
import com.squoshi.leaningtower.capability.LeaningCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningtower$move(MoverType type, Vec3 pos, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        player.getCapability(LeaningCapability.LEANING).ifPresent(leaning -> {
            if (leaning.isLeaning()) {
                // Example of handling leaning logic:
                // Modify the player's position based on leaning state
                Vec3 leanOffset = new Vec3(0.2, 0, 0); // Adjust as needed
                if (type == MoverType.SELF) {
                    player.setPos(player.getX() + leanOffset.x, player.getY(), player.getZ() + leanOffset.z);
                }
            }
        });
    }
}
