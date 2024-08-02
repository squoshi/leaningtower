package com.squoshi.leaningtower.client;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.LeaningTower;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LeaningTowerClientEvents {
    @SubscribeEvent
    public static void onClientComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LeanDirection leanDirection = ClientLeaningData.leanDirection;
        int leanTickDelta = ClientLeaningData.leanTickDelta;
        if (leanDirection != LeanDirection.NONE) {
            int duration = 20;
            int leanAngle = leanDirection == LeanDirection.LEFT ? -45 : 45;
            int angleIfPositive = Math.min(leanAngle, easeToFrom((int) event.getRoll(), leanAngle, duration, leanTickDelta));
            int angleIfNegative = Math.max(leanAngle, easeToFrom((int) event.getRoll(), leanAngle, duration, leanTickDelta));
            int angle = leanAngle > 0 ? angleIfPositive : angleIfNegative;
            event.setRoll(angle);
        } else {
            int duration = 20;
            int rollAsInt = (int) event.getRoll();
            int angle = easeToFrom(rollAsInt, 0, duration, leanTickDelta);
            event.setRoll(angle);
            if (angle == 0) {
                ClientLeaningData.leanTickDelta = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onClientRenderTick(TickEvent.RenderTickEvent event) {
        ClientLeaningData.tick();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }
        if (LeaningTowerKeyMappings.leanLeft.isDown() && LeaningTowerKeyMappings.leanRight.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.NONE);
            return;
        }
        if (LeaningTowerKeyMappings.leanLeft.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.LEFT);
        } else if (LeaningTowerKeyMappings.leanRight.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.RIGHT);
        } else {
            ClientLeaningData.setLeanDirection(LeanDirection.NONE);
        }
    }

    private static int easeToFrom(int from, int to, int duration, int tickDelta) {
        if (tickDelta >= duration) {
            return to;
        }
        return from + (to - from) * tickDelta / duration;
    }
}
