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
    private static final double EDGE_MARGIN = 0.8; // Margin to add to edge detection

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningtower$move(MoverType pType, Vec3 pPos, CallbackInfo ci) {
        if (pType == MoverType.SELF) {
            LocalPlayer player = (LocalPlayer) (Object) this;

            // Cancel movement when left alt is held
            if (LeaningTowerKeyMappings.leftAlt.isDown()) {
                ci.cancel();
                return;
            }

            // Handle leaning with Q and E keys
            if (ClientLeaningData.leanDirection != LeanDirection.NONE && player.onGround()) {
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

                    if (!isBlockBelow(player, targetPos)) {
                        ci.cancel();
                        return;
                    } else {
                        player.setPos(targetPos.x, targetPos.y, targetPos.z);
                        movementTicks++;
                    }
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

                    if (!isBlockBelow(player, targetPos)) {
                        ci.cancel();
                        return;
                    } else {
                        player.setPos(targetPos.x, targetPos.y, targetPos.z);
                        movementTicks--;
                    }
                } else {
                    isLeaning = false;
                    movementTicks = 0;
                    currentLeanDirection = LeanDirection.NONE;
                }
            }

            // Apply sneak-like behavior only when leaning
            if (isLeaning) {
                pPos = maybeBackOffFromEdge(player, pPos);
                Vec3 futurePos = player.position().add(pPos);
                if (!isBlockBelow(player, futurePos)) {
                    ci.cancel();
                    return;
                }
            }
        }
    }

    private double calculateLeanOffset(LocalPlayer player, double offset) {
        Vec3 direction = player.getLookAngle().yRot((float) Math.PI / 2);
        Vec3 leftPos = player.position().add(direction.x * (offset + EDGE_MARGIN), 0, direction.z * (offset + EDGE_MARGIN));
        Vec3 rightPos = player.position().add(-direction.x * (offset + EDGE_MARGIN), 0, -direction.z * (offset + EDGE_MARGIN));

        if (!isBlockBelow(player, leftPos) || !isBlockBelow(player, rightPos)) {
            return 0; // Prevent leaning if near the edge
        }

        return offset;
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

    private Vec3 maybeBackOffFromEdge(LocalPlayer player, Vec3 vec) {
        Level level = player.level();
        Vec3[] directions = {
                new Vec3(vec.x, 0, 0),
                new Vec3(0, 0, vec.z),
                new Vec3(vec.x, 0, vec.z)
        };

        for (Vec3 dir : directions) {
            Vec3 futurePos = player.position().add(dir);
            if (!isBlockBelow(player, futurePos)) {
                return Vec3.ZERO;
            }
        }

        return vec;
    }
}
