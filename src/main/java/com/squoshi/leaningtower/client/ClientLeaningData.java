package com.squoshi.leaningtower.client;

import com.squoshi.leaningtower.LeanDirection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
        int maxAngle = isHoldingAlt ? 35 : 20; // Max angle is 35 if holding Alt, otherwise 20
        if (direction == LeanDirection.LEFT) {
            targetLeanAngle = Math.max(targetLeanAngle - 5, -maxAngle);
        } else if (direction == LeanDirection.RIGHT) {
            targetLeanAngle = Math.min(targetLeanAngle + 5, maxAngle);
        }
        setLeanDirection(direction); // Ensure lean direction is updated
    }

    public static void tick() {
        if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            ClientLeaningData.leanTickDelta++;
        } else if (ClientLeaningData.isLeaning) {
            ClientLeaningData.stopLeanTickDelta++;
        }
        smoothUpdate(); // Update current angle smoothly towards the target angle
    }

    public static float getIncrementalLeanAngle() {
        return currentLeanAngle;
    }

    private static void smoothUpdate() {
        float smoothingFactor = 0.05f; // Adjust this value for smoother transitions (lower value means smoother transition)
        if (currentLeanAngle < targetLeanAngle) {
            currentLeanAngle = Math.min(currentLeanAngle + smoothingFactor * Math.abs(targetLeanAngle - currentLeanAngle), targetLeanAngle);
        } else if (currentLeanAngle > targetLeanAngle) {
            currentLeanAngle = Math.max(currentLeanAngle - smoothingFactor * Math.abs(targetLeanAngle - currentLeanAngle), targetLeanAngle);
        }
    }
}
