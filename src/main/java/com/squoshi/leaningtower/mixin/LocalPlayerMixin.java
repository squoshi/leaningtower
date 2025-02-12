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

    // =====================================================
    // NORMAL LEAN (e.g. Q/E) – completely separate mode
    // =====================================================
    private boolean isNormalLeaning = false;
    private static final double NORMAL_TOTAL_OFFSET = 0.05;  // Increment per tick for normal lean
    private static final int NORMAL_TICKS_TO_MOVE = 5;         // Total ticks over which to apply lean
    private int normalMovementTicks = 0;
    private LeanDirection normalLeanDirection = LeanDirection.NONE;

    // =====================================================
    // FREE LEAN (leftAlt + A/D) – completely separate mode
    // =====================================================
    private boolean isFreeLeaning = false;
    private Vec3 freeLeanInitialPosition = null; // Recorded when leftAlt is first held
    private double freeLeanCurrentOffset = 0.0;    // Current lateral offset from the base
    private double freeLeanTargetOffset = 0.0;     // Desired offset that we interpolate toward
    private LeanDirection freeLeanDirection = LeanDirection.NONE;
    private static final double FREE_LEAN_TOTAL_OFFSET = 0.05;  // Amount to change the target offset per tick
    private static final double FREE_LEAN_MAX_RIGHT = 0.4;       // Maximum allowed offset to the right (positive)
    private static final double FREE_LEAN_MAX_LEFT = 0.4;        // Maximum allowed offset to the left (as a positive magnitude)
    private static final double FREE_INTERPOLATION_FACTOR = 0.2;  // Smoothing factor for free lean while active

    // Use a separate (typically lower) interpolation factor for the return.
    private static final double FREE_LEAN_RETURN_INTERPOLATION_FACTOR = 0.3;

    // =====================================================
    // MOVE INJECTION – decide which lean mode to use
    // =====================================================
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningtower$move(MoverType pType, Vec3 pPos, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        // If the player is sprinting, cancel any lean.
        if (player.isSprinting()) {
            resetNormalLean();
            resetFreeLean();
            ClientLeaningData.leanDirection = LeanDirection.NONE;
            return;
        }

        // Sync body rotation with head when using normal lean.
        if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            player.yBodyRot = player.yHeadRot;
        }

        // --- FREE LEAN MODE ---
        // Free lean is active while leftAlt is held down.
        if (LeaningTowerKeyMappings.leftAlt.isDown()) {
            // Only allow free lean if there’s ground directly below.
            if (!isGroundDirectlyBelow(player)) {
                resetFreeLean();
                return;
            }
            ci.cancel();
            handleFreeLean(player);
        }
        // --- NORMAL LEAN MODE ---
        else if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            handleNormalLean(player);
        }
        // --- NO LEAN KEYS PRESSED ---
        else {
            // If free lean was active and leftAlt is now released, gradually return the player to the base.
            if (isFreeLeaning) {
                ci.cancel();
                handleFreeLeanReturn(player);
            }
            // For normal lean, gradually remove the lean motion.
            else if (isNormalLeaning) {
                handleNormalReturn(player);
            }
        }
    }

    // =====================================================
    // NORMAL LEAN METHODS (for Q/E or similar keys)
    // =====================================================
    private void handleNormalLean(LocalPlayer player) {
        if (isNormalLeaning && normalLeanDirection != ClientLeaningData.leanDirection) {
            // Step 1: Smoothly return to center before switching lean direction
            if (normalMovementTicks > 0) {
                handleNormalReturn(player);
                return; // Wait for return to complete before switching
            }

            // Step 2: Now that we've returned, switch to the new lean direction
            normalMovementTicks = 0;
            normalLeanDirection = ClientLeaningData.leanDirection;
        }

        if (!isNormalLeaning || normalLeanDirection != ClientLeaningData.leanDirection) {
            // Start the new lean
            isNormalLeaning = true;
            normalMovementTicks = 0;
            normalLeanDirection = ClientLeaningData.leanDirection;
        }

        if (normalMovementTicks < NORMAL_TICKS_TO_MOVE) {
            // Step 3: Smoothly apply the new lean movement
            double incrementalOffset = NORMAL_TOTAL_OFFSET;
            Vec3 movementVector = getNormalMovementVector(player, incrementalOffset);
            player.lerpMotion(movementVector.x, 0, movementVector.z);
            normalMovementTicks++;
        }
    }

    private void handleNormalReturn(LocalPlayer player) {
        if (normalMovementTicks > 0) {
            double incrementalOffset = -NORMAL_TOTAL_OFFSET;
            Vec3 movementVector = getNormalMovementVector(player, incrementalOffset);
            player.lerpMotion(movementVector.x, 0, movementVector.z);
            normalMovementTicks--;
        } else {
            resetNormalLean();
        }
    }

    /**
     * Computes a lateral movement vector for normal lean.
     * Uses the player's right vector such that:
     * - If lean direction is RIGHT (E pressed), returns rightVector * offset (moving right),
     * - If lean direction is LEFT (Q pressed), returns (-rightVector) * offset (moving left).
     */
    private Vec3 getNormalMovementVector(LocalPlayer player, double offset) {
        Vec3 rightVector = getRightVector(player);
        if (normalLeanDirection == LeanDirection.RIGHT) {
            return rightVector.scale(offset);
        } else if (normalLeanDirection == LeanDirection.LEFT) {
            return rightVector.scale(-offset);
        }
        return Vec3.ZERO;
    }

    // =====================================================
    // FREE LEAN METHODS (leftAlt + A/D)
    // =====================================================
    private void handleFreeLean(LocalPlayer player) {
        isFreeLeaning = true;
        // On the first tick of free lean, record the player's base (initial) position.
        if (freeLeanInitialPosition == null) {
            freeLeanInitialPosition = player.position();
            freeLeanTargetOffset = 0.0;
            freeLeanCurrentOffset = 0.0;
            freeLeanDirection = LeanDirection.NONE;
        }

        // Update the target offset only when an increment key is pressed.
        // • Pressing A (increment left) subtracts to get a negative target offset.
        // • Pressing D (increment right) adds to get a positive target offset.
        if (LeaningTowerKeyMappings.incrementLeft.isDown()) {
            freeLeanTargetOffset = Math.max(freeLeanTargetOffset - FREE_LEAN_TOTAL_OFFSET, -FREE_LEAN_MAX_LEFT);
            freeLeanDirection = LeanDirection.LEFT;
        } else if (LeaningTowerKeyMappings.incrementRight.isDown()) {
            freeLeanTargetOffset = Math.min(freeLeanTargetOffset + FREE_LEAN_TOTAL_OFFSET, FREE_LEAN_MAX_RIGHT);
            freeLeanDirection = LeanDirection.RIGHT;
        }
        // Smoothly interpolate the current offset toward the target offset.
        freeLeanCurrentOffset += (freeLeanTargetOffset - freeLeanCurrentOffset) * FREE_INTERPOLATION_FACTOR;

        // Compute the new lateral position relative to the recorded base.
        Vec3 movementVector = getFreeLeanMovementVector(player, freeLeanCurrentOffset);
        Vec3 futurePos = freeLeanInitialPosition.add(movementVector);
        player.setPos(futurePos.x, futurePos.y, futurePos.z);
    }

    /**
     * When free lean is released, this method gradually interpolates the player's position
     * from its current position back to the recorded base (freeLeanInitialPosition).
     */
    private void handleFreeLeanReturn(LocalPlayer player) {
        if (freeLeanInitialPosition == null) {
            resetFreeLean();
            return;
        }
        Vec3 currentPos = player.position();
        // Lerp from current position back to the base using a lower interpolation factor for a slower, smoother return.
        Vec3 newPos = currentPos.lerp(freeLeanInitialPosition, FREE_LEAN_RETURN_INTERPOLATION_FACTOR);
        player.setPos(newPos.x, newPos.y, newPos.z);

        // Also smoothly reduce the lean offset toward zero.
        freeLeanCurrentOffset += (0 - freeLeanCurrentOffset) * FREE_LEAN_RETURN_INTERPOLATION_FACTOR;
        freeLeanTargetOffset += (0 - freeLeanTargetOffset) * FREE_LEAN_RETURN_INTERPOLATION_FACTOR;

        // Once the player is nearly back at the base and the offset is minimal, reset free lean state.
        if (newPos.distanceTo(freeLeanInitialPosition) < 0.01 && Math.abs(freeLeanCurrentOffset) < 0.01) {
            resetFreeLean();
        }
    }

    /**
     * Computes a lateral movement vector for free lean.
     * Uses the same right vector so that a positive offset moves right and a negative offset moves left.
     */
    private Vec3 getFreeLeanMovementVector(LocalPlayer player, double offset) {
        Vec3 rightVector = getRightVector(player);
        return rightVector.scale(offset);
    }

    /**
     * Computes the player's right vector.
     * For a given normalized look vector, the right vector is defined as (-look.z, 0, look.x).
     * For example, if the player is looking north (0, *, -1), this returns (1, 0, 0).
     */
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

    /**
     * Checks if there is any solid ground directly below the player's feet.
     */
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