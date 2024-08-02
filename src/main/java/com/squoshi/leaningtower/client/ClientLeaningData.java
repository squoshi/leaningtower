package com.squoshi.leaningtower.client;

import com.squoshi.leaningtower.LeanDirection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientLeaningData {
    public static LeanDirection leanDirection = LeanDirection.NONE;
    public static int leanTickDelta = 0;

    public static void setLeanDirection(LeanDirection leanDirection) {
        ClientLeaningData.leanDirection = leanDirection;
    }

    public static void tick() {
        if (ClientLeaningData.leanDirection != LeanDirection.NONE) {
            ClientLeaningData.leanTickDelta++;
        }
    }
}
