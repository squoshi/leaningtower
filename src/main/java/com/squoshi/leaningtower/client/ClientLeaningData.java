package com.squoshi.leaningtower.client;

import com.mojang.logging.LogUtils;
import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.LeaningTower;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@SuppressWarnings("unchecked")
@OnlyIn(Dist.CLIENT)
public class ClientLeaningData {
    public static LeanDirection leanDirection = LeanDirection.NONE;
    public static LeanDirection prevLeanDirection = LeanDirection.NONE;
    public static boolean isLeaning;
    public static int leanTickDelta = 0;
    public static int stopLeanTickDelta = 0;
    public static float currentLeanAngle = 0; // Current angle for smooth interpolation
    public static float targetLeanAngle = 0;  // Target angle for smooth interpolation
    public static float transitionAngle = 0;  // Transition angle for smooth swapping between Q and E
    public static boolean isHoldingAlt = false;

    public static void setLeaning(boolean leaning) {
        ClientLeaningData.isLeaning = leaning;
    }

    public static void setLeanDirection(LeanDirection leanDirection) {
        if (ClientLeaningData.leanDirection != leanDirection) {
            ClientLeaningData.transitionAngle = ClientLeaningData.currentLeanAngle;
        }
        ClientLeaningData.leanDirection = leanDirection;
        if (leanDirection != LeanDirection.NONE) {
            setPrevLeanDirection(leanDirection);
            setLeaning(true);
        }
    }

    public static void setPrevLeanDirection(LeanDirection leanDirection) {
        ClientLeaningData.prevLeanDirection = leanDirection;
    }

    public static void incrementLean(LeanDirection direction) {
        int maxAngle = isHoldingAlt ? 35 : 25;
        if (direction == LeanDirection.LEFT) {
            targetLeanAngle = Math.max(targetLeanAngle - 5, -maxAngle);
        } else if (direction == LeanDirection.RIGHT) {
            targetLeanAngle = Math.min(targetLeanAngle + 5, maxAngle);
        }
        setLeanDirection(direction);
    }

    public static void tick(float deltaTime) {
        if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            ClientLeaningData.leanTickDelta++;
            boolean isLeaningLeft = ClientLeaningData.leanDirection == LeanDirection.LEFT;

            if (leanTickDelta == 1) {
                playAnim("lean_right", isLeaningLeft); // Mirror right lean for left lean
            }
        } else if (ClientLeaningData.isLeaning) {
            ClientLeaningData.stopLeanTickDelta++;
            stopAnim();
        }
        smoothUpdate(deltaTime);
    }

    public static float getIncrementalLeanAngle() {
        return currentLeanAngle;
    }

    private static void smoothUpdate(float deltaTime) {
        float smoothingFactor = 0.15f;
        float adjustedSmoothing = smoothingFactor * deltaTime;

        if (currentLeanAngle < targetLeanAngle) {
            currentLeanAngle = Math.min(currentLeanAngle + adjustedSmoothing * Math.abs(targetLeanAngle - currentLeanAngle), targetLeanAngle);
        } else if (currentLeanAngle > targetLeanAngle) {
            currentLeanAngle = Math.max(currentLeanAngle - adjustedSmoothing * Math.abs(targetLeanAngle - currentLeanAngle), targetLeanAngle);
        }
    }

    public static void stopLeaning() {
        isLeaning = false;
        leanTickDelta = 0;
        stopLeanTickDelta = 0;
        targetLeanAngle = 0;
    }

    private static void playAnim(String name, boolean mirror) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData((AbstractClientPlayer) player)
                .get(new ResourceLocation(LeaningTower.MODID, "animation"));

        if (animation == null) {
            LogUtils.getLogger().error("Animation layer not found for player!");
            return;
        }

        // Retrieve the KeyframeAnimation from the registry
        KeyframeAnimation keyframeAnim = PlayerAnimationRegistry.getAnimation(new ResourceLocation(LeaningTower.MODID, name));

        if (keyframeAnim == null) {
            LogUtils.getLogger().error("Animation '{}' not found in registry!", name);
            return;
        }

        // Convert KeyframeAnimation to KeyframeAnimationPlayer
        KeyframeAnimationPlayer animPlayer = new KeyframeAnimationPlayer(keyframeAnim);

        // Create a new ModifierLayer to hold the animation
        ModifierLayer<IAnimation> animationLayer = new ModifierLayer<>();

        if (mirror) {
            animationLayer.addModifierBefore(new MirrorModifier(true)); // Apply mirror effect
        }

        animationLayer.setAnimation(animPlayer);
        animation.setAnimation(animationLayer);
    }

    private static void stopAnim() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        var animation = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData((AbstractClientPlayer) player)
                .get(new ResourceLocation(LeaningTower.MODID, "animation"));

        if (animation == null) {
            LogUtils.getLogger().error("Animation layer not found for player!");
            return;
        }

        // Check if an animation is currently active before stopping
        IAnimation currentAnimation = animation.getAnimation();
        if (currentAnimation instanceof KeyframeAnimationPlayer) {
            ((KeyframeAnimationPlayer) currentAnimation).stop();
        } else {
            animation.setAnimation(null); // Clear the animation safely
        }
    }
}
