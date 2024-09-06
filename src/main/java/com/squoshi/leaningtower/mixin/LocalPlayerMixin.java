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
    private static final double EDGE_MARGIN = 0.5; // Edge detection margin
    private static final double WALL_MARGIN = 0.1; // Margin to stop before hitting the wall
    private int movementTicks = 0;
    private LeanDirection currentLeanDirection = LeanDirection.NONE;
    private static final float MAX_LEAN_ANGLE = 35.0f; // Maximum lean angle
    private Vec3 initialPosition = null; // Track initial position when left alt is pressed
    private boolean returningToPosition = false; // Track if the player is returning to the initial position
    private int wallDetectionCooldown = 0; // Cooldown timer for wall detection

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

            // Handle wall detection cooldown
            if (wallDetectionCooldown > 0) {
                wallDetectionCooldown--;
            }

            // Prevent leaning if the player is not on the ground
            if (!isOnGround) {
                resetLeaningState();
                return;
            }

            // Calculate the perpendicular direction to the player's facing direction
            Vec3 leanDirection = getPerpendicularDirection(player);

            // Check for directional override: disable wall detection if moving away
            boolean overrideWallStop = (currentLeanDirection == LeanDirection.LEFT && player.input.left) ||
                    (currentLeanDirection == LeanDirection.RIGHT && player.input.right);

            // Adjust the max lean distance based on wall detection
            double maxLeanDistance = MAX_LEAN_DISTANCE;
            if (ClientLeaningData.leanDirection == LeanDirection.LEFT && !overrideWallStop && wallDetectionCooldown == 0) {
                maxLeanDistance = getAdjustedLeanDistance(player, leanDirection.scale(-1));
            } else if (ClientLeaningData.leanDirection == LeanDirection.RIGHT && !overrideWallStop && wallDetectionCooldown == 0) {
                maxLeanDistance = getAdjustedLeanDistance(player, leanDirection);
            }

            // Handle leaning with Q and E keys
            if (ClientLeaningData.leanDirection != LeanDirection.NONE && !LeaningTowerKeyMappings.leftAlt.isDown()) {
                handleLeanMovement(player, ci, leanDirection, maxLeanDistance);
            } else if (isLeaning && !returningToPosition) {
                handleReturnMovement(player, ci, leanDirection, maxLeanDistance);
            }

            // Handle free-lean with Alt + A/D keys
            if (LeaningTowerKeyMappings.leftAlt.isDown()) {
                if (initialPosition == null) {
                    initialPosition = player.position(); // Store initial position when Alt is first pressed
                }
                ci.cancel(); // Cancel normal movement when in free-lean mode
                handleFreeLeanMovement(player, ci, leanDirection, maxLeanDistance);
            }

            if (!LeaningTowerKeyMappings.leftAlt.isDown() && initialPosition != null) {
                returningToPosition = true;
                isLeaning = false;
            }

            if (returningToPosition) {
                handleReturnToInitialPosition(player, ci);
            }

            if (isLeaning && !returningToPosition && isOnGround) {
                pPos = maybeBackOffFromEdge(player, pPos);
                Vec3 futurePos = player.position().add(pPos);
                if (!isBlockBelow(player, futurePos) && !isFloatingBlock(level, futurePos)) {
                    return;
                }
            }

            if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
                pPos = maybeBackOffFromEdge(player, pPos);
                if (pPos.equals(Vec3.ZERO)) {
                    return;
                }
            }
        }
    }

    private void handleLeanMovement(LocalPlayer player, CallbackInfo ci, Vec3 leanDirection, double maxLeanDistance) {
        if (!isLeaning || currentLeanDirection != ClientLeaningData.leanDirection) {
            isLeaning = true;
            movementTicks = 0;
            currentLeanDirection = ClientLeaningData.leanDirection;
            wallDetectionCooldown = 20; // Set cooldown (e.g., 1 second at 20 ticks per second)
        }
        if (movementTicks < TICKS_TO_MOVE) {
            double incrementalOffset = Math.min(TOTAL_OFFSET / TICKS_TO_MOVE, maxLeanDistance);
            Vec3 targetPos = player.position();
            if (ClientLeaningData.leanDirection == LeanDirection.LEFT) {
                targetPos = targetPos.add(-leanDirection.x * incrementalOffset, 0, -leanDirection.z * incrementalOffset);
            } else if (ClientLeaningData.leanDirection == LeanDirection.RIGHT) {
                targetPos = targetPos.add(leanDirection.x * incrementalOffset, 0, leanDirection.z * incrementalOffset);
            }

            // Adjust the player's position to avoid leaning into walls
            double adjustedDistance = getAdjustedLeanDistance(player, leanDirection.scale(currentLeanDirection == LeanDirection.LEFT ? -1 : 1));
            if (incrementalOffset > adjustedDistance) {
                incrementalOffset = adjustedDistance;
            }

            // Apply the adjusted position if we have any distance left
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

            // Adjust the player's position to avoid leaning into walls
            double adjustedDistance = getAdjustedLeanDistance(player, leanDirection.scale(currentLeanDirection == LeanDirection.LEFT ? -1 : 1));
            if (incrementalOffset > adjustedDistance) {
                incrementalOffset = adjustedDistance;
            }

            // Apply the adjusted position if we have any distance left
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

                    // Adjust for wall detection during Alt Lean
                    double adjustedDistance = getAdjustedLeanDistance(player, leanDirection.scale(-1));
                    if (currentLeanOffset > adjustedDistance) {
                        return; // Stop movement if a wall is detected
                    }
                } else {
                    return;
                }
            } else if (LeaningTowerKeyMappings.incrementRight.isDown()) {
                if (ClientLeaningData.getIncrementalLeanAngle() < MAX_LEAN_ANGLE) {
                    currentLeanOffset = calculateLeanOffset(player, ClientLeaningData.targetLeanAngle);
                    targetPos = targetPos.add(leanDirection.x * currentLeanOffset, 0, leanDirection.z * currentLeanOffset);
                    ClientLeaningData.targetLeanAngle += ALT_TOTAL_OFFSET / ALT_TICKS_TO_MOVE;

                    // Adjust for wall detection during Alt Lean
                    double adjustedDistance = getAdjustedLeanDistance(player, leanDirection);
                    if (currentLeanOffset > adjustedDistance) {
                        return; // Stop movement if a wall is detected
                    }
                } else {
                    return;
                }
            }

            if (initialPosition != null && initialPosition.distanceTo(targetPos) > maxLeanDistance) {
                return;
            }

            if (!isBlockBelow(player, targetPos)) {
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

    private boolean isBlockBelow(LocalPlayer player, Vec3 position) {
        Level level = player.level();
        double yOffset = -0.1;

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
                return true;
            }
        }

        return false;
    }

    private double getAdjustedLeanDistance(LocalPlayer player, Vec3 leanDirection) {
        Level level = player.level();
        AABB boundingBox = player.getBoundingBox();

        Vec3 normalizedLeanDirection = leanDirection.normalize();
        double[] heights = {0.0, player.getEyeHeight() / 2.0, player.getEyeHeight()}; // Feet, chest, head heights

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
                return Math.max(0, distance - WALL_MARGIN);
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

    private boolean isFloatingBlock(Level level, Vec3 position) {
        BlockPos blockPos = new BlockPos((int) Math.floor(position.x), (int) Math.floor(position.y - 1), (int) Math.floor(position.z));
        BlockState stateBelow = level.getBlockState(blockPos);
        return stateBelow.isAir();
    }
}