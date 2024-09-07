package com.squoshi.leaningtower.client;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.config.LeaningTowerConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "leaningtower", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LeaningTowerClientEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean leanLeftToggled = false;
    private static boolean leanRightToggled = false;
    private static final int ALT_RESET_DURATION = 60; // Duration for Alt lean reset (slower)
    private static final int NORMAL_RESET_DURATION = 42; // Duration for normal lean reset (faster)

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

        if (ClientLeaningData.isHoldingAlt) {
            int duration = ALT_RESET_DURATION;
            float currentRoll = (float) event.getRoll();
            float angle = (float) easeToFrom(currentRoll, leanAngle, duration, leanTickDelta, event.getPartialTick());
            event.setRoll(angle);
        } else if (ClientLeaningData.isLeaning && ClientLeaningData.leanDirection == LeanDirection.NONE) {
            // Only apply the slow reset when Alt is released
            float targetAngle = 0.0f;
            int duration = ALT_RESET_DURATION;
            float angle = (float) easeToFrom(ClientLeaningData.currentLeanAngle, targetAngle, duration, stopLeanTickDelta, event.getPartialTick());
            event.setRoll(angle);
            if (Math.abs(angle) < 0.01) { // If the angle is very close to zero, stop leaning
                resetLeaningState();
            }
        } else if (leanDirection != LeanDirection.NONE) {
            // Regular lean reset
            int duration = NORMAL_RESET_DURATION;
            float currentRoll = (float) event.getRoll();
            float angle = (float) easeToFrom(currentRoll, leanAngle, duration, leanTickDelta, event.getPartialTick());
            event.setRoll(angle);
        }
    }

    @SubscribeEvent
    public static void onClientRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }
        ClientLeaningData.tick(event.renderTickTime);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }

        boolean holdLean = LeaningTowerConfig.CLIENT.holdLean.get();
        boolean wasHoldingAlt = ClientLeaningData.isHoldingAlt;
        ClientLeaningData.isHoldingAlt = LeaningTowerKeyMappings.leftAlt.isDown();

        if (ClientLeaningData.isHoldingAlt) {
            handleAltLean();
        } else if (wasHoldingAlt) {
            // Reset state after Alt is released
            resetAltLeanState();
        } else if (holdLean) {
            handleHoldLean();
        } else {
            handleToggleLean();
        }

        // Stop leaning if sprinting or jumping
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && (player.isSprinting() || player.input.jumping)) {
            ClientLeaningData.stopLeaning();
        }
    }

    private static void handleHoldLean() {
        if (LeaningTowerKeyMappings.leanLeft.isDown() && LeaningTowerKeyMappings.leanRight.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.NONE);
            return;
        }

        if (LeaningTowerKeyMappings.leanLeft.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.LEFT);
            ClientLeaningData.targetLeanAngle = -20;
        } else if (LeaningTowerKeyMappings.leanRight.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.RIGHT);
            ClientLeaningData.targetLeanAngle = 20;
        } else {
            ClientLeaningData.setLeanDirection(LeanDirection.NONE);
        }
    }

    private static void handleToggleLean() {
        if (LeaningTowerKeyMappings.leanLeft.isDown()) {
            if (!leanLeftToggled) {
                if (ClientLeaningData.leanDirection == LeanDirection.LEFT) {
                    ClientLeaningData.setLeanDirection(LeanDirection.NONE);
                } else {
                    ClientLeaningData.setLeanDirection(LeanDirection.LEFT);
                    ClientLeaningData.targetLeanAngle = -20;
                }
                leanLeftToggled = true;
            }
        } else {
            leanLeftToggled = false;
        }

        if (LeaningTowerKeyMappings.leanRight.isDown()) {
            if (!leanRightToggled) {
                if (ClientLeaningData.leanDirection == LeanDirection.RIGHT) {
                    ClientLeaningData.setLeanDirection(LeanDirection.NONE);
                } else {
                    ClientLeaningData.setLeanDirection(LeanDirection.RIGHT);
                    ClientLeaningData.targetLeanAngle = 20;
                }
                leanRightToggled = true;
            }
        } else {
            leanRightToggled = false;
        }
    }

    private static void handleAltLean() {
        if (LeaningTowerKeyMappings.incrementLeft.isDown()) {
            ClientLeaningData.incrementLean(LeanDirection.LEFT);
        } else if (LeaningTowerKeyMappings.incrementRight.isDown()) {
            ClientLeaningData.incrementLean(LeanDirection.RIGHT);
        }
    }

    private static void resetAltLeanState() {
        ClientLeaningData.setLeanDirection(LeanDirection.NONE);
        ClientLeaningData.targetLeanAngle = 0;
        ClientLeaningData.stopLeanTickDelta = 0;
        ClientLeaningData.leanTickDelta = 0;
        ClientLeaningData.setLeaning(true); // Ensures the easing happens when Alt is released
    }

    private static void resetLeaningState() {
        ClientLeaningData.leanTickDelta = 0;
        ClientLeaningData.stopLeanTickDelta = 0;
        ClientLeaningData.setLeaning(false);
        ClientLeaningData.targetLeanAngle = 0;
        ClientLeaningData.currentLeanAngle = 0;
    }

    private static double easeToFrom(double from, double to, int duration, int tickDelta, double partialTicks) {
        double progress = (tickDelta + partialTicks) / duration;
        if (progress >= 1.0) {
            return to;
        }
        return from + (to - from) * (Math.pow(progress, 3) * (progress * (6.0 * progress - 15.0) + 10.0));
    }
}
