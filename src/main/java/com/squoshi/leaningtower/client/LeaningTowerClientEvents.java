package com.squoshi.leaningtower.client;

import com.squoshi.leaningtower.LeanDirection;
import net.minecraft.client.CameraType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LeaningTowerClientEvents {
    @SubscribeEvent
    public static void onClientComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }

        LeanDirection leanDirection = ClientLeaningData.leanDirection;
        LeanDirection prevLeanDirection = ClientLeaningData.prevLeanDirection;
        int leanTickDelta = ClientLeaningData.leanTickDelta;
        int stopLeanTickDelta = ClientLeaningData.stopLeanTickDelta;
        float leanAngle = ClientLeaningData.getIncrementalLeanAngle();

        if (leanDirection != LeanDirection.NONE || ClientLeaningData.isHoldingAlt) { // Maintain angle if holding Alt
            int duration = 42;
            float angleIfPositive = Math.min(leanAngle, easeToFrom((float) event.getRoll(), leanAngle, duration, leanTickDelta));
            float angleIfNegative = Math.max(leanAngle, easeToFrom((float) event.getRoll(), leanAngle, duration, leanTickDelta));
            float angle = leanAngle > 0 ? angleIfPositive : angleIfNegative;
            event.setRoll(angle);
        } else if (ClientLeaningData.isLeaning) {
            int duration = 42;
            float rollAsFloat = prevLeanDirection == LeanDirection.LEFT ? -20 : 20;
            float angle = easeToFrom(rollAsFloat, 0, duration, stopLeanTickDelta);
            event.setRoll(angle);
            if (angle == 0) {
                ClientLeaningData.leanTickDelta = 0;
                ClientLeaningData.stopLeanTickDelta = 0;
                ClientLeaningData.setLeaning(false);
                ClientLeaningData.targetLeanAngle = 0; // Reset target angle when not leaning
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

        ClientLeaningData.isHoldingAlt = LeaningTowerKeyMappings.leftAlt.isDown(); // Tracks if Alt is held
        if (LeaningTowerKeyMappings.leanLeft.isDown() && LeaningTowerKeyMappings.leanRight.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.NONE);
            return;
        }
        if (ClientLeaningData.isHoldingAlt) {
            if (LeaningTowerKeyMappings.incrementLeft.isDown()) {
                ClientLeaningData.incrementLean(LeanDirection.LEFT);
            } else if (LeaningTowerKeyMappings.incrementRight.isDown()) {
                ClientLeaningData.incrementLean(LeanDirection.RIGHT);
            }
        } else {
            if (LeaningTowerKeyMappings.leanLeft.isDown()) {
                ClientLeaningData.setLeanDirection(LeanDirection.LEFT);
                ClientLeaningData.targetLeanAngle = -20; // Ensure lean is set to -20 for Q unless we agreed upon changing it
            } else if (LeaningTowerKeyMappings.leanRight.isDown()) {
                ClientLeaningData.setLeanDirection(LeanDirection.RIGHT);
                ClientLeaningData.targetLeanAngle = 20; // Ensure lean is set to 20 for E unless we agreed upon changing it
            } else {
                ClientLeaningData.setLeanDirection(LeanDirection.NONE);
            }
        }
    }

    private static float easeToFrom(float from, float to, int duration, int tickDelta) {
        if (tickDelta >= duration) {
            return to;
        }
        return from + (to - from) * tickDelta / duration;
    }
}
