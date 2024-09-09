package com.squoshi.leaningtower.mixin;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.client.ClientLeaningData;
import com.squoshi.leaningtower.client.LeaningTowerKeyMappings;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
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
    private static final double TOTAL_OFFSET = 0.3; // Total offset to lean for Q and E
    private static final int TICKS_TO_MOVE = 5; // Ticks to move for Q and E
    private static final double ALT_TOTAL_OFFSET = 0.2; // Total offset to lean for left alt + A/D
    private static final int ALT_TICKS_TO_MOVE = 5; // Ticks to move for left alt + A/D
    private static final double MAX_LEAN_DISTANCE = 0.7; // Maximum distance the player can lean
    private static final double EDGE_MARGIN = 0.01; // Base edge margin
    private static final double OVERHANG_ALLOWANCE = 0.05; // Allowance for player to overhang
    private static final double WALL_MARGIN = 0.03; // Margin to stop before hitting the wall
    private int movementTicks = 0;
    private LeanDirection currentLeanDirection = LeanDirection.NONE;
    private static final float MAX_LEAN_ANGLE = 35.0f; // Maximum lean angle
    private Vec3 initialPosition = null; // Track initial position when left alt is pressed
    private boolean returningToPosition = false; // Track if the player is returning to the initial position
    private boolean wallDetectedWhileLeaning = false; // Track if wall detected while leaning
    private boolean wallDetectionEnabled = true; // Control to enable/disable wall detection
    private boolean justHitWall = false; // Control to disable further movement canceling after wall hit

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void leaningtower$move(MoverType pType, Vec3 pPos, CallbackInfo ci) {
        if (pType == MoverType.SELF) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            Level level = player.level();
            boolean isOnGround = player.onGround();
            boolean isJumping = player.input.jumping; // Check if the player is attempting to jump

            if (!isOnGround) {
                resetLeaningState(); // Reset leaning if the player is in the air
                return;
            }

            if (isJumping) {
                resetLeaningState(); // Allow jump and reset leaning state
                return;
            }

            Vec3 leanDirection = getPerpendicularDirection(player);

            boolean overrideWallStop = (currentLeanDirection == LeanDirection.LEFT && player.input.left) ||
                    (currentLeanDirection == LeanDirection.RIGHT && player.input.right);

            double maxLeanDistance = MAX_LEAN_DISTANCE;
            if (ClientLeaningData.leanDirection == LeanDirection.LEFT && !overrideWallStop && wallDetectionEnabled) {
                maxLeanDistance = getAdjustedLeanDistance(player, leanDirection.scale(-1));
            } else if (ClientLeaningData.leanDirection == LeanDirection.RIGHT && !overrideWallStop && wallDetectionEnabled) {
                maxLeanDistance = getAdjustedLeanDistance(player, leanDirection);
            }

            if (ClientLeaningData.leanDirection != LeanDirection.NONE && !LeaningTowerKeyMappings.leftAlt.isDown()) {
                handleLeanMovement(player, ci, leanDirection, maxLeanDistance);
            } else if (isLeaning && !returningToPosition) {
                handleReturnMovement(player, ci, leanDirection, maxLeanDistance);
            }

            if (LeaningTowerKeyMappings.leftAlt.isDown()) {
                if (initialPosition == null) {
                    initialPosition = player.position();
                }
                ci.cancel();
                handleFreeLeanMovement(player, ci, leanDirection, maxLeanDistance);
            }

            if (!LeaningTowerKeyMappings.leftAlt.isDown() && initialPosition != null) {
                returningToPosition = true;
                isLeaning = false;
            }

            if (returningToPosition) {
                handleReturnToInitialPosition(player, ci);
            }

            // Adjusted edge detection logic
            if (isLeaning && !returningToPosition && isOnGround) {
                pPos = maybeBackOffFromEdge(player, pPos);
                Vec3 futurePos = player.position().add(pPos);

                if (!isGroundDirectlyBelowOrStep(player, futurePos, EDGE_MARGIN)) {
                    ci.cancel();
                    return;
                }
            }

            if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
                pPos = maybeBackOffFromEdge(player, pPos);
                if (pPos.equals(Vec3.ZERO)) {
                    ci.cancel();
                }
            }
        }
    }

    private void handleLeanMovement(LocalPlayer player, CallbackInfo ci, Vec3 leanDirection, double maxLeanDistance) {
        if (!isLeaning || currentLeanDirection != ClientLeaningData.leanDirection) {
            isLeaning = true;
            movementTicks = 0;
            currentLeanDirection = ClientLeaningData.leanDirection;
            wallDetectedWhileLeaning = false;
            wallDetectionEnabled = true; // Enable wall detection at the start of leaning
            justHitWall = false; // Reset the flag when leaning starts
        }

        if (movementTicks < TICKS_TO_MOVE) {
            double incrementalOffset = Math.min(TOTAL_OFFSET / TICKS_TO_MOVE, maxLeanDistance);
            Vec3 targetPos = player.position();
            if (ClientLeaningData.leanDirection == LeanDirection.LEFT) {
                targetPos = targetPos.add(-leanDirection.x * incrementalOffset, 0, -leanDirection.z * incrementalOffset);
            } else if (ClientLeaningData.leanDirection == LeanDirection.RIGHT) {
                targetPos = targetPos.add(leanDirection.x * incrementalOffset, 0, leanDirection.z * incrementalOffset);
            }

            double adjustedDistance = getAdjustedLeanDistance(player, leanDirection.scale(currentLeanDirection == LeanDirection.LEFT ? -1 : 1));
            if (incrementalOffset > adjustedDistance) {
                wallDetectedWhileLeaning = true;
                wallDetectionEnabled = false; // Disable wall detection after the initial wall hit
                justHitWall = true; // Mark that we just hit the wall
                return;
            }

            if (isAtEdge(player, targetPos)) {
                ci.cancel();
                return;
            }

            if (incrementalOffset > 0) {
                player.setPos(targetPos.x, targetPos.y, targetPos.z);
                movementTicks++;
            }
        }
    }

    private void handleReturnMovement(LocalPlayer player, CallbackInfo ci, Vec3 leanDirection, double maxLeanDistance) {
        if (movementTicks > 0) {
            double incrementalOffset = Math.min(TOTAL_OFFSET / TICKS_TO_MOVE, maxLeanDistance);
            Vec3 targetPos = player.position();
            if (currentLeanDirection == LeanDirection.LEFT) {
                targetPos = targetPos.add(leanDirection.x * incrementalOffset, 0, leanDirection.z * incrementalOffset);
            } else if (currentLeanDirection == LeanDirection.RIGHT) {
                targetPos = targetPos.add(-leanDirection.x * incrementalOffset, 0, -leanDirection.z * incrementalOffset);
            }

            double adjustedDistance = getAdjustedLeanDistance(player, leanDirection.scale(currentLeanDirection == LeanDirection.LEFT ? -1 : 1));
            if (incrementalOffset > adjustedDistance) {
                if (!justHitWall) {
                    wallDetectedWhileLeaning = true;
                    wallDetectionEnabled = false;
                    return;
                } else {
                    justHitWall = false;
                }
            }

            if (isAtEdge(player, targetPos)) {
                ci.cancel();
                return;
            }

            if (incrementalOffset > 0) {
                player.setPos(targetPos.x, targetPos.y, targetPos.z);
                movementTicks--;
            }
        } else {
            resetLeaningState();
        }
    }

    private void handleFreeLeanMovement(LocalPlayer player, CallbackInfo ci, Vec3 leanDirection, double maxLeanDistance) {
        if (LeaningTowerKeyMappings.incrementLeft.isDown() || LeaningTowerKeyMappings.incrementRight.isDown()) {
            double currentLeanOffset = calculateLeanOffset(player, ClientLeaningData.getIncrementalLeanAngle());
            Vec3 targetPos = player.position();

            if (LeaningTowerKeyMappings.incrementLeft.isDown()) {
                if (ClientLeaningData.getIncrementalLeanAngle() > -MAX_LEAN_ANGLE) {
                    currentLeanOffset = calculateLeanOffset(player, ClientLeaningData.targetLeanAngle);
                    targetPos = targetPos.add(-leanDirection.x * currentLeanOffset, 0, -leanDirection.z * currentLeanOffset);
                    ClientLeaningData.targetLeanAngle -= ALT_TOTAL_OFFSET / ALT_TICKS_TO_MOVE;

                    double adjustedDistance = getAdjustedLeanDistance(player, leanDirection.scale(-1));
                    if (currentLeanOffset > adjustedDistance && wallDetectionEnabled) {
                        wallDetectedWhileLeaning = true;
                        wallDetectionEnabled = false;
                        justHitWall = true;
                        return;
                    }
                } else {
                    return;
                }
            } else if (LeaningTowerKeyMappings.incrementRight.isDown()) {
                if (ClientLeaningData.getIncrementalLeanAngle() < MAX_LEAN_ANGLE) {
                    currentLeanOffset = calculateLeanOffset(player, ClientLeaningData.targetLeanAngle);
                    targetPos = targetPos.add(leanDirection.x * currentLeanOffset, 0, leanDirection.z * currentLeanOffset);
                    ClientLeaningData.targetLeanAngle += ALT_TOTAL_OFFSET / ALT_TICKS_TO_MOVE;

                    double adjustedDistance = getAdjustedLeanDistance(player, leanDirection);
                    if (currentLeanOffset > adjustedDistance && wallDetectionEnabled) {
                        wallDetectedWhileLeaning = true;
                        wallDetectionEnabled = false;
                        justHitWall = true;
                        return;
                    }
                } else {
                    return;
                }
            }

            if (isAtEdge(player, targetPos)) {
                ci.cancel();
                return;
            }

            if (initialPosition != null && initialPosition.distanceTo(targetPos) > maxLeanDistance) {
                return;
            }

            if (!isGroundDirectlyBelowOrStep(player, targetPos, EDGE_MARGIN)) {
                return;
            } else {
                player.setPos(targetPos.x, targetPos.y, targetPos.z);
            }
        }
    }

    private void handleReturnToInitialPosition(LocalPlayer player, CallbackInfo ci) {
        if (initialPosition != null && movementTicks < ALT_TICKS_TO_MOVE) {
            double incrementalOffset = initialPosition.distanceTo(player.position()) / (ALT_TICKS_TO_MOVE - movementTicks);
            Vec3 directionToInitial = initialPosition.subtract(player.position()).normalize();
            Vec3 targetPos = player.position().add(directionToInitial.scale(incrementalOffset));
            player.setPos(targetPos.x, targetPos.y, targetPos.z);
            movementTicks++;
        } else {
            resetLeaningState();
        }
    }

    private void resetLeaningState() {
        isLeaning = false;
        returningToPosition = false;
        initialPosition = null;
        ClientLeaningData.leanDirection = LeanDirection.NONE;
        ClientLeaningData.targetLeanAngle = 0;
        wallDetectedWhileLeaning = false;
        wallDetectionEnabled = true; // Re-enable wall detection when leaning is reset
        justHitWall = false; // Reset the wall hit flag
        movementTicks = 0;
    }

    private Vec3 getPerpendicularDirection(LocalPlayer player) {
        Vec3 lookDirection = player.getLookAngle().normalize();
        return new Vec3(-lookDirection.z, 0, lookDirection.x).normalize();
    }

    private double calculateLeanOffset(LocalPlayer player, float angle) {
        double maxOffset = ALT_TOTAL_OFFSET;
        double factor = Math.abs(angle) / MAX_LEAN_ANGLE;
        return maxOffset * factor;
    }

    private boolean isAtEdge(LocalPlayer player, Vec3 position) {
        return !isGroundDirectlyBelowOrStep(player, position, EDGE_MARGIN + OVERHANG_ALLOWANCE);
    }

    private boolean isGroundDirectlyBelowOrStep(LocalPlayer player, Vec3 position, double margin) {
        Level level = player.level();
        BlockPos currentPos = new BlockPos(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y - 0.1), // Slightly below the player's feet
                (int) Math.floor(position.z)
        );

        // Check for ground directly below
        BlockState stateBelow = level.getBlockState(currentPos);
        if (!stateBelow.isAir()) {
            return true;
        }

        // Check for step-down blocks just slightly below
        BlockPos stepPos = currentPos.below();
        BlockState stepState = level.getBlockState(stepPos);
        return !stepState.isAir();
    }

    private double getAdjustedLeanDistance(LocalPlayer player, Vec3 leanDirection) {
        Level level = player.level();
        AABB boundingBox = player.getBoundingBox();
        Vec3 normalizedLeanDirection = leanDirection.normalize();
        double[] heights = {0.0, player.getEyeHeight() / 2.0, player.getEyeHeight()}; // Check at multiple heights to detect collisions.

        for (double distance = WALL_MARGIN; distance <= MAX_LEAN_DISTANCE; distance += WALL_MARGIN) {
            AABB expandedBox = boundingBox.expandTowards(normalizedLeanDirection.scale(distance));
            boolean collides = false;

            for (double height : heights) {
                AABB heightBox = expandedBox.move(0, height, 0);

                if (normalizedLeanDirection.x > 0) {
                    for (double x = heightBox.maxX; x <= heightBox.maxX + WALL_MARGIN; x += WALL_MARGIN) {
                        for (double z = heightBox.minZ; z <= heightBox.maxZ; z += WALL_MARGIN) {
                            BlockPos blockPos = new BlockPos((int) Math.floor(x), (int) Math.floor(heightBox.minY), (int) Math.floor(z));
                            BlockState blockState = level.getBlockState(blockPos);
                            if (!blockState.isAir()) {
                                collides = true;
                                break;
                            }
                        }
                        if (collides) break;
                    }
                } else if (normalizedLeanDirection.x < 0) {
                    for (double x = heightBox.minX; x >= heightBox.minX - WALL_MARGIN; x -= WALL_MARGIN) {
                        for (double z = heightBox.minZ; z <= heightBox.maxZ; z += WALL_MARGIN) {
                            BlockPos blockPos = new BlockPos((int) Math.floor(x), (int) Math.floor(heightBox.minY), (int) Math.floor(z));
                            BlockState blockState = level.getBlockState(blockPos);
                            if (!blockState.isAir()) {
                                collides = true;
                                break;
                            }
                        }
                        if (collides) break;
                    }
                }

                if (normalizedLeanDirection.z > 0) {
                    for (double z = heightBox.maxZ; z <= heightBox.maxZ + WALL_MARGIN; z += WALL_MARGIN) {
                        for (double x = heightBox.minX; x <= heightBox.maxX; x += WALL_MARGIN) {
                            BlockPos blockPos = new BlockPos((int) Math.floor(x), (int) Math.floor(heightBox.minY), (int) Math.floor(z));
                            BlockState blockState = level.getBlockState(blockPos);
                            if (!blockState.isAir()) {
                                collides = true;
                                break;
                            }
                        }
                        if (collides) break;
                    }
                } else if (normalizedLeanDirection.z < 0) {
                    for (double z = heightBox.minZ; z >= heightBox.minZ - WALL_MARGIN; z -= WALL_MARGIN) {
                        for (double x = heightBox.minX; x <= heightBox.maxX; x += WALL_MARGIN) {
                            BlockPos blockPos = new BlockPos((int) Math.floor(x), (int) Math.floor(heightBox.minY), (int) Math.floor(z));
                            BlockState blockState = level.getBlockState(blockPos);
                            if (!blockState.isAir()) {
                                collides = true;
                                break;
                            }
                        }
                        if (collides) break;
                    }
                }

                if (collides) break;
            }

            if (collides) {
                // Allow a further increased overhang beyond the edge
                return Math.max(0, distance - WALL_MARGIN + OVERHANG_ALLOWANCE);
            }
        }

        return MAX_LEAN_DISTANCE;
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

            if (stateBelow.isAir()) {
                BlockPos adjacentBlockPos = futureBlockPos.below();
                BlockState adjacentStateBelow = level.getBlockState(adjacentBlockPos);
                if (adjacentStateBelow.isAir()) {
                    return Vec3.ZERO;
                }
            }
        }

        return vec;
    }
}
