package com.squoshi.leaningtower.mixin;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.server.ServerLeaningData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-safe player mixin to apply lean movement based on networked direction.
 */
@Mixin(Player.class)
public class PlayerMixin {

    private static final double SERVER_LEAN_OFFSET = 0.05;

    @Inject(method = "tick", at = @At("HEAD"))
    private void leaningTower$serverLeanTick(CallbackInfo ci) {
        Player player = (Player)(Object)this;

        // Only run on server players
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Only lean when grounded
        if (!player.onGround()) return;

        LeanDirection direction = ServerLeaningData.getDirection(serverPlayer);
        if (direction == LeanDirection.NONE) return;

        // Compute right vector
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

        Vec3 offset = switch (direction) {
            case LEFT -> right.scale(-SERVER_LEAN_OFFSET);
            case RIGHT -> right.scale(SERVER_LEAN_OFFSET);
            default -> Vec3.ZERO;
        };

        // Apply lateral lean motion
        player.lerpMotion(offset.x, 0, offset.z);
    }
}
