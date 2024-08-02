package com.squoshi.leaningtower.client;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.LeaningTower;
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

    public static void setLeaning(boolean leaning) {
        ClientLeaningData.isLeaning = leaning;
    }

    public static void setLeanDirection(LeanDirection leanDirection) {
        ClientLeaningData.leanDirection = leanDirection;
//        LeaningTower.LOGGER.info("Set Lean Direction: " + leanDirection);
        if (leanDirection != LeanDirection.NONE) {
            setPrevLeanDirection(leanDirection);
            setLeaning(true);
        }
    }

    public static void setPrevLeanDirection(LeanDirection leanDirection) {
        ClientLeaningData.prevLeanDirection = leanDirection;
    }

    public static void incrementLean(LeanDirection direction) {
        if (direction == LeanDirection.LEFT) {
            incrementalLeanAngle -= 5;
//            LeaningTower.LOGGER.info("Increment lean left: " + incrementalLeanAngle);
        } else if (direction == LeanDirection.RIGHT) {
            incrementalLeanAngle += 5;
//            LeaningTower.LOGGER.info("Increment lean right: " + incrementalLeanAngle);
        }
        incrementalLeanAngle = Math.max(-20, Math.min(20, incrementalLeanAngle));
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
//        LeaningTower.LOGGER.info("Get Incremental Lean Angle: " + incrementalLeanAngle);
        return incrementalLeanAngle;
    }
}
