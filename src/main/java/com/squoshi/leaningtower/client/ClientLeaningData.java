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

    public static void tick() {
        if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            ClientLeaningData.leanTickDelta++;
        } else if (ClientLeaningData.isLeaning) {
            ClientLeaningData.stopLeanTickDelta++;
        }
    }
}
