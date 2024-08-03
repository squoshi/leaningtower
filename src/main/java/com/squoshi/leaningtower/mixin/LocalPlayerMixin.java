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
    private boolean hasLeant = false; // Flag to track if the player has already leant
    private static final double OFFSET = 0.5; // Half a block
    private Vec3 originalPosition; // Store the original position

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
            if (ClientLeaningData.leanDirection != LeanDirection.NONE && !hasLeant) {
                originalPosition = player.position(); // Store the original position
                Vec3 direction = player.getLookAngle().yRot((float) Math.PI / 2); // Get the perpendicular direction

                if (ClientLeaningData.leanDirection == LeanDirection.LEFT) {
                    player.setPos(player.getX() + direction.x * OFFSET, player.getY(), player.getZ() + direction.z * OFFSET);
                } else if (ClientLeaningData.leanDirection == LeanDirection.RIGHT) {
                    player.setPos(player.getX() - direction.x * OFFSET, player.getY(), player.getZ() - direction.z * OFFSET);
                }
                hasLeant = true; // Set the flag to prevent continuous movement
            }

            // Handle returning to the original position
            if (!LeaningTowerKeyMappings.leanLeft.isDown() && !LeaningTowerKeyMappings.leanRight.isDown()) {
                if (hasLeant) {
                    player.setPos(originalPosition.x, originalPosition.y, originalPosition.z); // Reset to the original position
                    hasLeant = false; // Reset the flag when the keys are released
                }
                ClientLeaningData.setLeanDirection(LeanDirection.NONE);
            }
        }
    }
}
