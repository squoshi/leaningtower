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
    private boolean isLeaning = false;
    private static final double TOTAL_OFFSET = 0.5; // Total offset to lean
    private static final int TICKS_TO_MOVE = 4;
    private int movementTicks = 0;
    private LeanDirection currentLeanDirection = LeanDirection.NONE;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)

    private void leaningtower$move(MoverType pType, Vec3 pPos, CallbackInfo ci) {

        if (pType == MoverType.SELF) {

            LocalPlayer player = (LocalPlayer) (Object) this;


            // Cancel movement

            if (LeaningTowerKeyMappings.leftAlt.isDown()) {

                ci.cancel();

                return;

            }


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


                    // Check for collision

                    if (!player.level().noCollision(player, player.getBoundingBox().move(targetPos.subtract(player.position())))) {

                        // If collision is detected, move the player away from the colliding block

                        Vec3 collisionNormal = getCollisionNormal(player, targetPos);

                        targetPos = targetPos.add(collisionNormal.x * 0.05, collisionNormal.y * 0.05, collisionNormal.z * 0.05);

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


                    // Check for collision

                    if (!player.level().noCollision(player, player.getBoundingBox().move(targetPos.subtract(player.position())))) {

                        // If collision is detected, move the player away from the colliding block

                        Vec3 collisionNormal = getCollisionNormal(player, targetPos);

                        targetPos = targetPos.add(collisionNormal.x * 0.05, collisionNormal.y * 0.05, collisionNormal.z * 0.05);

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


    private Vec3 getCollisionNormal(LocalPlayer player, Vec3 targetPos) {

        // Calculate the collision normal (the direction away from the colliding block)

        Vec3 collisionNormal = player.position().subtract(targetPos).normalize();

        return collisionNormal;

    }
}