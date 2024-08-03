package com.squoshi.leaningtower.mixin;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.client.ClientLeaningData;
import com.squoshi.leaningtower.client.LeaningTowerKeyMappings;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    private boolean isLeaning = false;
    private static final double TOTAL_OFFSET = 0.5; // Total offset to lean
    private static final int TICKS_TO_MOVE = 4;
    private int movementTicks = 0;
    private LeanDirection currentLeanDirection = LeanDirection.NONE;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningtower$move(MoverType pType, Vec3 pPos, CallbackInfo ci) {
        if (pType == MoverType.SELF) {
            LocalPlayer player = (LocalPlayer) (Object) this;

            // Cancel movement when left alt is held
            if (LeaningTowerKeyMappings.leftAlt.isDown()) {
                ci.cancel();
                return;
            }

            // Continuous check while walking and leaning
            if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
                Vec3 direction = player.getLookAngle().yRot((float) Math.PI / 2); // Get the perpendicular direction
                Vec3 targetPos = player.position();
                if (ClientLeaningData.leanDirection == LeanDirection.LEFT) {
                    targetPos = targetPos.add(direction.x * 0.1, 0, direction.z * 0.1);
                } else if (ClientLeaningData.leanDirection == LeanDirection.RIGHT) {
                    targetPos = targetPos.add(-direction.x * 0.1, 0, -direction.z * 0.1);
                }

                // Check if there's a block below the player or the target position
                if (!isBlockBelow(player, player.position()) || !isBlockBelow(player, targetPos)) {
                    ci.cancel();
                    return;
                }
            }

            // Handle leaning with Q and E keys
            if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
                if (!isLeaning || currentLeanDirection != ClientLeaningData.leanDirection) {
                    isLeaning = true;
                    movementTicks = 0;
                    currentLeanDirection = ClientLeaningData.leanDirection;
                }
                if (movementTicks < TICKS_TO_MOVE) {
                    double incrementalOffset = TOTAL_OFFSET / TICKS_TO_MOVE;
                    Vec3 direction = player.getLookAngle().yRot((float) Math.PI / 2); // Get the perpendicular direction
                    Vec3 targetPos = player.position();
                    if (ClientLeaningData.leanDirection == LeanDirection.LEFT) {
                        targetPos = targetPos.add(direction.x * incrementalOffset, 0, direction.z * incrementalOffset);
                    } else if (ClientLeaningData.leanDirection == LeanDirection.RIGHT) {
                        targetPos = targetPos.add(-direction.x * incrementalOffset, 0, -direction.z * incrementalOffset);
                    }

                    // Check if there's a block below the target position
                    if (!isBlockBelow(player, targetPos)) {
                        ci.cancel();
                        return;
                    }

                    player.setPos(targetPos.x, targetPos.y, targetPos.z);
                    movementTicks++;
                }
            } else if (isLeaning) {
                // Move back on release
                if (movementTicks > 0) {
                    double incrementalOffset = TOTAL_OFFSET / TICKS_TO_MOVE;
                    Vec3 direction = player.getLookAngle().yRot((float) Math.PI / 2);
                    Vec3 targetPos = player.position();
                    if (currentLeanDirection == LeanDirection.LEFT) {
                        targetPos = targetPos.add(-direction.x * incrementalOffset, 0, -direction.z * incrementalOffset);
                    } else if (currentLeanDirection == LeanDirection.RIGHT) {
                        targetPos = targetPos.add(direction.x * incrementalOffset, 0, direction.z * incrementalOffset);
                    }

                    // Check if there's a block below the target position
                    if (!isBlockBelow(player, targetPos)) {
                        ci.cancel();
                        return;
                    }

                    player.setPos(targetPos.x, targetPos.y, targetPos.z);
                    movementTicks--;
                } else {
                    isLeaning = false;
                    movementTicks = 0;
                    currentLeanDirection = LeanDirection.NONE;
                }
            }
        }
    }

    private boolean isBlockBelow(LocalPlayer player, Vec3 position) {
        Level level = player.level();
        double yOffset = -0.1; // Check slightly below the player's feet

        // Define multiple check points under the player's feet
        Vec3[] checkPoints = {
                new Vec3(position.x, position.y + yOffset, position.z),
                new Vec3(position.x + 0.3, position.y + yOffset, position.z),
                new Vec3(position.x - 0.3, position.y + yOffset, position.z),
                new Vec3(position.x, position.y + yOffset, position.z + 0.3),
                new Vec3(position.x, position.y + yOffset, position.z - 0.3)
        };

        for (Vec3 checkPoint : checkPoints) {
            BlockPos blockPos = new BlockPos(
                    (int) Math.floor(checkPoint.x),
                    (int) Math.floor(position.y - 1),
                    (int) Math.floor(checkPoint.z)
            );
            BlockState stateBelow = level.getBlockState(blockPos);
            if (!stateBelow.isAir()) {
                return true; // If any point is supported, it's safe to move
            }
        }

        return false; // No solid ground found at the required positions
    }
}
