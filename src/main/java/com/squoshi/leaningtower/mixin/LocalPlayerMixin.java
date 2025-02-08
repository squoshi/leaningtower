package com.squoshi.leaningtower.mixin;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.client.ClientLeaningData;
import com.squoshi.leaningtower.client.LeaningTowerKeyMappings;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    private boolean isLeaning = false;
    private static final double TOTAL_OFFSET = 0.05;  // Small increments for smooth movement
    private static final double MAX_LEAN_LEFT_DISTANCE = 0.4;
    private static final double MAX_LEAN_RIGHT_DISTANCE = 0.4;
    private static final int TICKS_TO_MOVE = 5;  // Used for normal lean movement (Q/E)
    private int movementTicks = 0;
    private LeanDirection currentLeanDirection = LeanDirection.NONE;
    private Vec3 initialPosition = null;
    private double currentLeanOffset = 0.0; // Tracks current lean offset dynamically

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningtower$move(MoverType pType, Vec3 pPos, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        if (player.isSprinting()) {
            resetLeaningState();
            ClientLeaningData.leanDirection = LeanDirection.NONE;
            return;
        }

        // Sync body rotation with head
        if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            player.yBodyRot = player.yHeadRot;
        }

        // Handle left alt (free lean mode)
        if (LeaningTowerKeyMappings.leftAlt.isDown()) {
            if (!isGroundDirectlyBelow(player)) {
                resetLeaningState();
                return; // Let vanilla movement continue if no ground beneath
            }

            // Cancel vanilla movement and apply custom free lean
            ci.cancel();

            if (initialPosition == null) {
                initialPosition = player.position();
                currentLeanOffset = 0.0;
            }

            handleFreeLeanMovement(player);
        }
        // Normal lean mode when not using left alt
        else if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            handleLeanMovement(player);
        } else if (isLeaning) {
            handleReturnMovement(player);
        }
    }

    private void handleLeanMovement(LocalPlayer player) {
        if (!isLeaning || currentLeanDirection != ClientLeaningData.leanDirection) {
            isLeaning = true;
            movementTicks = 0;
            currentLeanDirection = ClientLeaningData.leanDirection;
        }

        if (movementTicks < TICKS_TO_MOVE) {
            double incrementalOffset = TOTAL_OFFSET;
            Vec3 movementVector = getMovementVector(player, incrementalOffset);
            player.lerpMotion(movementVector.x, 0, movementVector.z);
            movementTicks++;
        }
    }

    private void handleReturnMovement(LocalPlayer player) {
        if (movementTicks > 0) {
            double incrementalOffset = -TOTAL_OFFSET;
            Vec3 movementVector = getMovementVector(player, incrementalOffset);
            player.lerpMotion(movementVector.x, 0, movementVector.z);
            movementTicks--;
        } else {
            resetLeaningState();
        }
    }

    private void handleFreeLeanMovement(LocalPlayer player) {
        double incrementalOffset = 0.05; // Smooth increments for free lean

        // Detect key presses for A (left) or D (right) and update lean offset accordingly
        if (LeaningTowerKeyMappings.incrementLeft.isDown()) {
            currentLeanOffset = Math.max(currentLeanOffset - incrementalOffset, -MAX_LEAN_LEFT_DISTANCE);
            currentLeanDirection = LeanDirection.LEFT;
        } else if (LeaningTowerKeyMappings.incrementRight.isDown()) {
            currentLeanOffset = Math.min(currentLeanOffset + incrementalOffset, MAX_LEAN_RIGHT_DISTANCE);
            currentLeanDirection = LeanDirection.RIGHT;
        } else {
            return; // No key press, do nothing
        }

        // Calculate new position based on the current offset
        Vec3 movementVector = getMovementVector(player, currentLeanOffset);
        Vec3 futurePos = initialPosition.add(movementVector);
        player.setPos(futurePos.x, futurePos.y, futurePos.z);
    }

    private Vec3 getMovementVector(LocalPlayer player, double offset) {
        Vec3 lookDirection = getPerpendicularDirection(player);

        // Invert offset for left lean
        if (currentLeanDirection == LeanDirection.LEFT) {
            offset = -offset;
        }

        return new Vec3(lookDirection.x * offset, 0, lookDirection.z * offset);
    }

    private Vec3 getPerpendicularDirection(LocalPlayer player) {
        Vec3 lookDirection = player.getLookAngle().normalize();
        return new Vec3(-lookDirection.z, 0, lookDirection.x).normalize();
    }

    private void resetLeaningState() {
        isLeaning = false;
        currentLeanDirection = LeanDirection.NONE;
        movementTicks = 0;
        initialPosition = null;
        currentLeanOffset = 0.0;
    }

    private boolean isGroundDirectlyBelow(LocalPlayer player) {
        Level level = player.level();
        BlockPos posBelow = new BlockPos(
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY() - 0.1),
                (int) Math.floor(player.getZ())
        );
        BlockState blockState = level.getBlockState(posBelow);
        return !blockState.isAir();
    }
}