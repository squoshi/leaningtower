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
        LeanDirection prevLeanDirection = ClientLeaningData.prevLeanDirection;
        int leanTickDelta = ClientLeaningData.leanTickDelta;
        int stopLeanTickDelta = ClientLeaningData.stopLeanTickDelta;
        int leanAngle = ClientLeaningData.getIncrementalLeanAngle();

        LeaningTower.LOGGER.info("Current Lean Angle: " + leanAngle);
        LeaningTower.LOGGER.info("Current Lean Direction: " + leanDirection);

        if (leanDirection != LeanDirection.NONE || ClientLeaningData.isHoldingAlt) { // Maintain angle if holding Alt
            int duration = 40;
            int angleIfPositive = Math.min(leanAngle, easeToFrom((int) event.getRoll(), leanAngle, duration, leanTickDelta));
            int angleIfNegative = Math.max(leanAngle, easeToFrom((int) event.getRoll(), leanAngle, duration, leanTickDelta));
            int angle = leanAngle > 0 ? angleIfPositive : angleIfNegative;
            event.setRoll(angle);
        } else if (ClientLeaningData.isLeaning) {
            int duration = 40;
            int rollAsInt = prevLeanDirection == LeanDirection.LEFT ? -20 : 20;
            int angle = easeToFrom(rollAsInt, 0, duration, stopLeanTickDelta);
            LeaningTower.LOGGER.info("Returning to center, angle: " + angle);
            event.setRoll(angle);
            if (angle == 0) {
                ClientLeaningData.leanTickDelta = 0;
                ClientLeaningData.stopLeanTickDelta = 0;
                ClientLeaningData.setLeaning(false);
                ClientLeaningData.incrementalLeanAngle = 0; // Reset incremental angle when not leaning
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
        ClientLeaningData.isHoldingAlt = LeaningTowerKeyMappings.leftAlt.isDown(); // Track if Alt is held
        if (LeaningTowerKeyMappings.leanLeft.isDown() && LeaningTowerKeyMappings.leanRight.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.NONE);
            return;
        }
        if (ClientLeaningData.isHoldingAlt) {
//            LeaningTower.LOGGER.info("Left Alt is down");
            if (LeaningTowerKeyMappings.incrementLeft.isDown()) {
//                LeaningTower.LOGGER.info("Increment left is down");
                ClientLeaningData.incrementLean(LeanDirection.LEFT);
            } else if (LeaningTowerKeyMappings.incrementRight.isDown()) {
//                LeaningTower.LOGGER.info("Increment right is down");
                ClientLeaningData.incrementLean(LeanDirection.RIGHT);
            }
        } else {
            if (LeaningTowerKeyMappings.leanLeft.isDown()) {
//                LeaningTower.LOGGER.info("Lean left is down");
                ClientLeaningData.setLeanDirection(LeanDirection.LEFT);
                ClientLeaningData.incrementalLeanAngle = -20; // Ensure lean is set to -20 for Q
            } else if (LeaningTowerKeyMappings.leanRight.isDown()) {
//                LeaningTower.LOGGER.info("Lean right is down");
                ClientLeaningData.setLeanDirection(LeanDirection.RIGHT);
                ClientLeaningData.incrementalLeanAngle = 20; // Ensure lean is set to 20 for E
            } else {
                ClientLeaningData.setLeanDirection(LeanDirection.NONE);
            }
        }
    }

    private static int easeToFrom(int from, int to, int duration, int tickDelta) {
        if (tickDelta >= duration) {
            return to;
        }
        return from + (to - from) * tickDelta / duration;
    }
}
