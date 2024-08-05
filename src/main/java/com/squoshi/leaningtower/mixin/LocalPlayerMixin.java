package com.squoshi.leaningtower.mixin;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.client.ClientLeaningData;
import com.squoshi.leaningtower.client.LeaningTowerKeyMappings;
import com.squoshi.leaningtower.network.LeaningPacket;
import com.squoshi.leaningtower.network.NetworkHandler;
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
    private static final double TOTAL_OFFSET = 0.5; // Total offset to lean for Q and E
    private static final int TICKS_TO_MOVE = 4; // Ticks to move for Q and E
    private static final double ALT_TOTAL_OFFSET = 0.2; // Total offset to lean for left alt + A/D
    private static final int ALT_TICKS_TO_MOVE = 5; // Ticks to move for left alt + A/D (increased speed), less is faster return to previous position
    private static final double MAX_LEAN_DISTANCE = 0.7; // Maximum distance the player can lean
    private static final double EDGE_MARGIN = 0.5; // Edge detection margin
    private int movementTicks = 0;
    private LeanDirection currentLeanDirection = LeanDirection.NONE;
    private static final float MAX_LEAN_ANGLE = 35.0f; // Maximum lean angle
    private Vec3 initialPosition; // Track initial position when left alt is pressed
    private boolean returningToPosition = false; // Track if the player is returning to the initial position

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningtower$move(MoverType pType, Vec3 pPos, CallbackInfo ci) {
        if (pType == MoverType.SELF) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            Level level = player.level();
            boolean isOnGround = player.onGround();

            // Log the state for debugging
            System.out.println("Leaning direction: " + ClientLeaningData.leanDirection);
            System.out.println("Is on ground: " + isOnGround);
            System.out.println("Is returning to position: " + returningToPosition);
            System.out.println("Initial position: " + initialPosition);
            System.out.println("Movement ticks: " + movementTicks);

            // Prevent leaning if the player is not on the ground
            if (!isOnGround) {
                // Reset leaning state when in the air
                if (isLeaning || returningToPosition) {
                    isLeaning = false;
                    returningToPosition = false;
                    ClientLeaningData.leanDirection = LeanDirection.NONE;
                    ClientLeaningData.targetLeanAngle = 0;
                }
                return;
            }

            // Handle leaning with Q and E keys (unaffected by incremental lean)
            if (ClientLeaningData.leanDirection != LeanDirection.NONE && !LeaningTowerKeyMappings.leftAlt.isDown()) {
                if (!isLeaning || currentLeanDirection != ClientLeaningData.leanDirection) {
                    isLeaning = true;
                    movementTicks = 0;
                    currentLeanDirection = ClientLeaningData.leanDirection;
                    NetworkHandler.INSTANCE.sendToServer(new LeaningPacket(currentLeanDirection == LeanDirection.LEFT || currentLeanDirection == LeanDirection.RIGHT));
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
            } else if (isLeaning && !returningToPosition) {
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
                    NetworkHandler.INSTANCE.sendToServer(new LeaningPacket(false));
                }
            }

            // Save the initial position when left alt is pressed
            if (LeaningTowerKeyMappings.leftAlt.isDown() && !returningToPosition) {
                if (initialPosition == null) {
                    initialPosition = player.position();
                }

                // Cancel horizontal movement when left alt is held without A or D
                if (!LeaningTowerKeyMappings.incrementLeft.isDown() && !LeaningTowerKeyMappings.incrementRight.isDown()) {
                    ci.cancel();
                    return;
                }

                // Apply incremental leaning with left alt + A or D keys
                if (LeaningTowerKeyMappings.incrementLeft.isDown() || LeaningTowerKeyMappings.incrementRight.isDown()) {
                    ci.cancel();

                    double currentLeanOffset = calculateLeanOffset(player, ClientLeaningData.getIncrementalLeanAngle());
                    Vec3 direction = player.getLookAngle().yRot((float) Math.PI / 2); // Get the perpendicular direction
                    Vec3 targetPos = player.position();

                    // Handle incremental leaning
                    if (LeaningTowerKeyMappings.incrementLeft.isDown()) {
                        if (ClientLeaningData.getIncrementalLeanAngle() > -MAX_LEAN_ANGLE) {
                            currentLeanOffset = calculateLeanOffset(player, ClientLeaningData.targetLeanAngle);
                            targetPos = targetPos.add(direction.x * currentLeanOffset, 0, direction.z * currentLeanOffset);
                            ClientLeaningData.targetLeanAngle -= ALT_TOTAL_OFFSET / ALT_TICKS_TO_MOVE;
                        } else {
                            return;
                        }
                    } else if (LeaningTowerKeyMappings.incrementRight.isDown()) {
                        if (ClientLeaningData.getIncrementalLeanAngle() < MAX_LEAN_ANGLE) {
                            currentLeanOffset = calculateLeanOffset(player, ClientLeaningData.targetLeanAngle);
                            targetPos = targetPos.add(-direction.x * currentLeanOffset, 0, -direction.z * currentLeanOffset);
                            ClientLeaningData.targetLeanAngle += ALT_TOTAL_OFFSET / ALT_TICKS_TO_MOVE;
                        } else {
                            return;
                        }
                    }

                    // Ensure the player doesn't move beyond the maximum lean distance
                    if (initialPosition.distanceTo(targetPos) > MAX_LEAN_DISTANCE) {
                        return;
                    }

                    if (!isBlockBelow(player, targetPos)) {
                        return;
                    } else {
                        player.setPos(targetPos.x, targetPos.y, targetPos.z);
                    }
                }
            }

            // Start returning to the initial position when left alt is released
            if (!LeaningTowerKeyMappings.leftAlt.isDown() && initialPosition != null) {
                returningToPosition = true;
                isLeaning = false;
            }

            // Handle returning to the initial position smoothly
            if (returningToPosition) {
                ci.cancel();

                if (initialPosition != null && movementTicks < ALT_TICKS_TO_MOVE) {
                    double incrementalOffset = initialPosition.distanceTo(player.position()) / (ALT_TICKS_TO_MOVE - movementTicks);
                    Vec3 direction = initialPosition.subtract(player.position()).normalize();
                    Vec3 targetPos = player.position().add(direction.scale(incrementalOffset));
                    player.setPos(targetPos.x, targetPos.y, targetPos.z);
                    movementTicks++;
                } else {
                    returningToPosition = false;
                    movementTicks = 0;
                    initialPosition = null;
                    ClientLeaningData.targetLeanAngle = 0; // Reset lean angle
                }
            }

            // Apply sneak-like behavior only when leaning and on the ground
            if (isLeaning && !returningToPosition && isOnGround) {
                pPos = maybeBackOffFromEdge(player, pPos);
                Vec3 futurePos = player.position().add(pPos);
                if (!isBlockBelow(player, futurePos) && !isFloatingBlock(level, futurePos)) {
                    ci.cancel();
                    return;
                }
            }

            // Additional edge detection for forward and backward movement while leaning
            if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
                pPos = maybeBackOffFromEdge(player, pPos);
                if (pPos.equals(Vec3.ZERO)) {
                    ci.cancel();
                }
            }
        }
    }

    private double calculateLeanOffset(LocalPlayer player, float angle) {
        double maxOffset = ALT_TOTAL_OFFSET;
        double factor = Math.abs(angle) / MAX_LEAN_ANGLE; // Use MAX_LEAN_ANGLE for the factor calculation
        return maxOffset * factor;
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
            BlockPos futureBlockPos = new BlockPos((int) Math.floor(futurePos.x), (int) Math.floor(futurePos.y - 1), (int) Math.floor(futurePos.z));
            BlockState stateBelow = level.getBlockState(futureBlockPos);

            // Adjust edge detection logic to allow walking onto floating blocks
            if (stateBelow.isAir()) {
                BlockPos adjacentBlockPos = futureBlockPos.below();
                BlockState adjacentStateBelow = level.getBlockState(adjacentBlockPos);
                if (adjacentStateBelow.isAir()) {
                    return Vec3.ZERO; // Stop movement if there's no support below the floating block
                }
            }
        }

        return vec;
    }

    private boolean isFloatingBlock(Level level, Vec3 position) {
        BlockPos blockPos = new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y - 1), (int) Math.floor(position.z));
        BlockState stateBelow = level.getBlockState(blockPos);
        return stateBelow.isAir();
    }
}
