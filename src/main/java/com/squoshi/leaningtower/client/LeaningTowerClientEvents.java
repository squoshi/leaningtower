package com.squoshi.leaningtower.client;

import com.squoshi.leaningtower.LeanDirection;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        if (Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }

        LeanDirection leanDirection = ClientLeaningData.leanDirection;
        LeanDirection prevLeanDirection = ClientLeaningData.prevLeanDirection;
        int leanTickDelta = ClientLeaningData.leanTickDelta;
        int stopLeanTickDelta = ClientLeaningData.stopLeanTickDelta;
        float leanAngle = ClientLeaningData.getIncrementalLeanAngle();

        if (leanDirection != LeanDirection.NONE || ClientLeaningData.isHoldingAlt) {
            int duration = 42;
            float currentRoll = (float) event.getRoll();
            float angle = (float) easeToFrom((double) currentRoll, (double) leanAngle, duration, leanTickDelta, (double) event.getPartialTick());
            event.setRoll(angle);
        } else if (ClientLeaningData.isLeaning) {
            int duration = 42;
            float targetAngle = prevLeanDirection == LeanDirection.LEFT ? -20 : 20;
            float angle = (float) easeToFrom((double) targetAngle, 0.0, duration, stopLeanTickDelta, (double) event.getPartialTick());
            event.setRoll(angle);
            if (Math.abs(angle) < 0.01) { // If the angle is very close to zero, stop leaning
                ClientLeaningData.leanTickDelta = 0;
                ClientLeaningData.stopLeanTickDelta = 0;
                ClientLeaningData.setLeaning(false);
                ClientLeaningData.targetLeanAngle = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onClientRenderTick(TickEvent.RenderTickEvent event) {
        ClientLeaningData.tick(event.renderTickTime);
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

        // Stop leaning if sprinting or jumping
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            if (player.isSprinting() || player.input.jumping) {
                ClientLeaningData.stopLeaning();
            }
        }
    }

    private static double easeToFrom(double from, double to, int duration, int tickDelta, double partialTicks) {
        double progress = (tickDelta + partialTicks) / duration;
        if (progress >= 1.0) {
            return to;
        }
        // Smooth easing using a cubic function for smoother transition
        return from + (to - from) * (Math.pow(progress, 3) * (progress * (6.0 * progress - 15.0) + 10.0));
    }
}
