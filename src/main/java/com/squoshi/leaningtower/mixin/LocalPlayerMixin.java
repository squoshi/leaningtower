package com.squoshi.leaningtower.mixin;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.client.ClientLeaningData;
import com.squoshi.leaningtower.client.LeaningTowerKeyMappings;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side mixin for handling lean input and applying it visually to the LocalPlayer.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    private boolean isNormalLeaning = false;
    private static final double NORMAL_TOTAL_OFFSET = 0.05;
    private static final int NORMAL_TICKS_TO_MOVE = 5;
    private int normalMovementTicks = 0;
    private LeanDirection normalLeanDirection = LeanDirection.NONE;

    private boolean isFreeLeaning = false;
    private Vec3 freeLeanInitialPosition = null;
    private double freeLeanCurrentOffset = 0.0;
    private double freeLeanTargetOffset = 0.0;
    private LeanDirection freeLeanDirection = LeanDirection.NONE;

    private static final double FREE_LEAN_TOTAL_OFFSET = 0.05;
    private static final double FREE_LEAN_MAX_RIGHT = 0.4;
    private static final double FREE_LEAN_MAX_LEFT = 0.4;
    private static final double FREE_INTERPOLATION_FACTOR = 0.2;
    private static final double FREE_LEAN_RETURN_INTERPOLATION_FACTOR = 0.3;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningTower$injectMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        if (player.isSprinting() || !player.onGround()) {
            resetNormalLean();
            resetFreeLean();
            ClientLeaningData.leanDirection = LeanDirection.NONE;
            return;
        }

        if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            player.yBodyRot = player.yHeadRot;
        }

        if (LeaningTowerKeyMappings.leftAlt.isDown()) {
            ci.cancel();
            handleFreeLean(player);
        } else if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            handleNormalLean(player);
        } else {
            if (isFreeLeaning) {
                ci.cancel();
                handleFreeLeanReturn(player);
            } else if (isNormalLeaning) {
                handleNormalReturn(player);
            }
        }
    }

    private void handleNormalLean(LocalPlayer player) {
        if (isNormalLeaning && normalLeanDirection != ClientLeaningData.leanDirection) {
            if (normalMovementTicks > 0) {
                handleNormalReturn(player);
                return;
            }
            normalMovementTicks = 0;
            normalLeanDirection = ClientLeaningData.leanDirection;
        }

        if (!isNormalLeaning || normalLeanDirection != ClientLeaningData.leanDirection) {
            isNormalLeaning = true;
            normalMovementTicks = 0;
            normalLeanDirection = ClientLeaningData.leanDirection;
        }

        if (normalMovementTicks < NORMAL_TICKS_TO_MOVE) {
            Vec3 offset = getNormalMovementVector(player, NORMAL_TOTAL_OFFSET);
            player.lerpMotion(offset.x, 0, offset.z);
            normalMovementTicks++;
        }
    }

    private void handleNormalReturn(LocalPlayer player) {
        if (normalMovementTicks > 0) {
            Vec3 offset = getNormalMovementVector(player, -NORMAL_TOTAL_OFFSET);
            player.lerpMotion(offset.x, 0, offset.z);
            normalMovementTicks--;
        } else {
            resetNormalLean();
        }
    }

    private Vec3 getNormalMovementVector(LocalPlayer player, double offset) {
        Vec3 right = getRightVector(player);
        return switch (normalLeanDirection) {
            case RIGHT -> right.scale(offset);
            case LEFT -> right.scale(-offset);
            default -> Vec3.ZERO;
        };
    }

    private void handleFreeLean(LocalPlayer player) {
        isFreeLeaning = true;
        if (freeLeanInitialPosition == null) {
            freeLeanInitialPosition = player.position();
            freeLeanTargetOffset = 0.0;
            freeLeanCurrentOffset = 0.0;
            freeLeanDirection = LeanDirection.NONE;
        }

        if (LeaningTowerKeyMappings.incrementLeft.isDown()) {
            freeLeanTargetOffset = Math.max(freeLeanTargetOffset - FREE_LEAN_TOTAL_OFFSET, -FREE_LEAN_MAX_LEFT);
            freeLeanDirection = LeanDirection.LEFT;
        } else if (LeaningTowerKeyMappings.incrementRight.isDown()) {
            freeLeanTargetOffset = Math.min(freeLeanTargetOffset + FREE_LEAN_TOTAL_OFFSET, FREE_LEAN_MAX_RIGHT);
            freeLeanDirection = LeanDirection.RIGHT;
        }

        freeLeanCurrentOffset += (freeLeanTargetOffset - freeLeanCurrentOffset) * FREE_INTERPOLATION_FACTOR;
        Vec3 newPos = freeLeanInitialPosition.add(getFreeLeanMovementVector(player, freeLeanCurrentOffset));
        player.setPos(newPos.x, newPos.y, newPos.z);
    }

    private void handleFreeLeanReturn(LocalPlayer player) {
        if (freeLeanInitialPosition == null || !player.onGround()) {
            resetFreeLean();
            return;
        }

        Vec3 current = player.position();
        Vec3 newPos = current.lerp(freeLeanInitialPosition, FREE_LEAN_RETURN_INTERPOLATION_FACTOR);
        player.setPos(newPos.x, newPos.y, newPos.z);

        freeLeanCurrentOffset += -freeLeanCurrentOffset * FREE_LEAN_RETURN_INTERPOLATION_FACTOR;
        freeLeanTargetOffset += -freeLeanTargetOffset * FREE_LEAN_RETURN_INTERPOLATION_FACTOR;

        if (newPos.distanceTo(freeLeanInitialPosition) < 0.01 && Math.abs(freeLeanCurrentOffset) < 0.01) {
            resetFreeLean();
        }
    }

    private Vec3 getFreeLeanMovementVector(LocalPlayer player, double offset) {
        return getRightVector(player).scale(offset);
    }

    private Vec3 getRightVector(LocalPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        return new Vec3(-look.z, 0, look.x).normalize();
    }

    private void resetNormalLean() {
        isNormalLeaning = false;
        normalLeanDirection = LeanDirection.NONE;
        normalMovementTicks = 0;
    }

    private void resetFreeLean() {
        isFreeLeaning = false;
        freeLeanInitialPosition = null;
        freeLeanCurrentOffset = 0.0;
        freeLeanTargetOffset = 0.0;
        freeLeanDirection = LeanDirection.NONE;
    }
}
