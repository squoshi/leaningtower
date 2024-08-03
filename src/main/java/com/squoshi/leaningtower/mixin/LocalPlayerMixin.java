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

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    private boolean isLeaning = false; // Flag to track if the player is leaning
    private static final double TOTAL_OFFSET = 0.5; // Total movement over multiple ticks
    private static final int TICKS_TO_MOVE = 5; // Number of ticks to complete the movement
    private Vec3 originalPosition; // Store the original position
    private int movementTicks = 0; // Counter for ticks
    private LeanDirection currentLeanDirection = LeanDirection.NONE; // Track the current leaning direction

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningtower$move(MoverType pType, Vec3 pPos, CallbackInfo ci) {
        if (pType == MoverType.SELF) {
            LocalPlayer player = (LocalPlayer) (Object) this;

            // Cancel movement when left alt is held
            if (LeaningTowerKeyMappings.leftAlt.isDown()) {
                ci.cancel();
                return; // Ensure other logic is not executed
            }

            // Handle leaning with Q and E keys
            if (ClientLeaningData.leanDirection!= LeanDirection.NONE) {
                if (!isLeaning || currentLeanDirection!= ClientLeaningData.leanDirection) {
                    originalPosition = player.position(); // Store the original position
                    isLeaning = true; // Set the flag to prevent continuous movement
                    movementTicks = 0; // Reset the movement counter
                    currentLeanDirection = ClientLeaningData.leanDirection; // Update the current leaning direction
                }

                if (movementTicks < TICKS_TO_MOVE) {
                    // Calculate incremental movement per tick
                    double incrementalOffset = TOTAL_OFFSET / TICKS_TO_MOVE;
                    Vec3 direction = player.getLookAngle().yRot((float) Math.PI / 2); // Get the perpendicular direction

                    // Apply incremental movement
                    if (ClientLeaningData.leanDirection == LeanDirection.LEFT) {
                        player.setPos(player.getX() + direction.x * incrementalOffset, player.getY(), player.getZ() + direction.z * incrementalOffset);
                    } else if (ClientLeaningData.leanDirection == LeanDirection.RIGHT) {
                        player.setPos(player.getX() - direction.x * incrementalOffset, player.getY(), player.getZ() - direction.z * incrementalOffset);
                    }

                    // Increment the tick counter
                    movementTicks++;
                }

            } else if (isLeaning) {
                // Move back to the original position when the leaning key is released
                if (movementTicks > 0) {
                    double incrementalOffset = TOTAL_OFFSET / TICKS_TO_MOVE;
                    Vec3 direction = player.getLookAngle().yRot((float) Math.PI / 2); // Get the perpendicular direction

                    // Move back to the original position
                    if (currentLeanDirection == LeanDirection.LEFT) {
                        player.setPos(player.getX() - direction.x * incrementalOffset, player.getY(), player.getZ() - direction.z * incrementalOffset);
                    } else if (currentLeanDirection == LeanDirection.RIGHT) {
                        player.setPos(player.getX() + direction.x * incrementalOffset, player.getY(), player.getZ() + direction.z * incrementalOffset);
                    }

                    // Decrement the tick counter
                    movementTicks--;
                } else {
                    isLeaning = false; // Reset the flag when the keys are released
                    movementTicks = 0; // Reset movement ticks
                    currentLeanDirection = LeanDirection.NONE;
                }
            }
        }
    }
}