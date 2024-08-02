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
    public static int incrementalLeanAngle = 0;
    public static boolean isHoldingAlt = false;

    public static void setLeaning(boolean leaning) {
        ClientLeaningData.isLeaning = leaning;
    }

    public static void setLeanDirection(LeanDirection leanDirection) {
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
        int maxAngle = isHoldingAlt ? 45 : 20; // Max angle is 45 if holding Alt, otherwise 20
        if (direction == LeanDirection.LEFT) {
            incrementalLeanAngle = Math.max(incrementalLeanAngle - 5, -maxAngle);
        } else if (direction == LeanDirection.RIGHT) {
            incrementalLeanAngle = Math.min(incrementalLeanAngle + 5, maxAngle);
        }
        setLeanDirection(direction); // Ensure lean direction is updated
    }

    public static void tick() {
        if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            ClientLeaningData.leanTickDelta++;
        } else if (ClientLeaningData.isLeaning) {
            ClientLeaningData.stopLeanTickDelta++;
        }
    }

    public static int getIncrementalLeanAngle() {
        return incrementalLeanAngle;
    }
}
