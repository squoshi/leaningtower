package com.squoshi.leaningtower.client;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.LeaningTower;
import com.squoshi.leaningtower.config.LeaningTowerConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unchecked")
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "leaningtower", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LeaningTowerClientEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean leanLeftToggled = false;
    private static boolean leanRightToggled = false;
    private static final int ALT_RESET_DURATION = 60;
    private static final int NORMAL_RESET_DURATION = 42;

    private static float currentRoll = 0.0f;
    private static float targetRoll = 0.0f;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }

        LeanDirection leanDirection = ClientLeaningData.leanDirection;

        if (ClientLeaningData.isHoldingAlt) {
            targetRoll = ClientLeaningData.getIncrementalLeanAngle();
            smoothCameraRoll(event);
        } else if (leanDirection == LeanDirection.NONE) {
            targetRoll = 0.0f;
            smoothCameraRoll(event);
        } else {
            targetRoll = ClientLeaningData.getIncrementalLeanAngle();
            smoothCameraRoll(event);
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

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive()) {
            resetLeaningState(); // Fix for death crash
            return;
        }

        boolean holdLean = LeaningTowerConfig.CLIENT.holdLean.get();
        boolean wasHoldingAlt = ClientLeaningData.isHoldingAlt;
        ClientLeaningData.isHoldingAlt = LeaningTowerKeyMappings.leftAlt.isDown();

        if (ClientLeaningData.isHoldingAlt) {
            handleAltLean();
        } else if (wasHoldingAlt) {
            resetAltLeanState();
        } else if (holdLean) {
            handleHoldLean();
        } else {
            handleToggleLean();
        }

        if (player.isSprinting() || player.input.jumping || ClientLeaningData.stopLeanTickDelta > 20) {
            ClientLeaningData.stopLeaning();
        }
    }

    private static void handleHoldLean() {
        if (LeaningTowerKeyMappings.leanLeft.isDown() && LeaningTowerKeyMappings.leanRight.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.NONE);
            targetRoll = 0.0f;
            return;
        }

        if (LeaningTowerKeyMappings.leanLeft.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.LEFT);
            ClientLeaningData.targetLeanAngle = -25;
            targetRoll = -25.0f;
        } else if (LeaningTowerKeyMappings.leanRight.isDown()) {
            ClientLeaningData.setLeanDirection(LeanDirection.RIGHT);
            ClientLeaningData.targetLeanAngle = 25;
            targetRoll = 25.0f;
        } else {
            ClientLeaningData.setLeanDirection(LeanDirection.NONE);
            targetRoll = 0.0f;
        }
    }

    private static void handleToggleLean() {
        if (LeaningTowerKeyMappings.leanLeft.isDown()) {
            if (!leanLeftToggled) {
                if (ClientLeaningData.leanDirection == LeanDirection.LEFT) {
                    ClientLeaningData.setLeanDirection(LeanDirection.NONE);
                    targetRoll = 0.0f;
                } else {
                    ClientLeaningData.setLeanDirection(LeanDirection.LEFT);
                    ClientLeaningData.targetLeanAngle = -25;
                    targetRoll = -25.0f;
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
                    targetRoll = 0.0f;
                } else {
                    ClientLeaningData.setLeanDirection(LeanDirection.RIGHT);
                    ClientLeaningData.targetLeanAngle = 25;
                    targetRoll = 25.0f;
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
        ClientLeaningData.setLeaning(true);
        targetRoll = 0.0f;
    }

    private static void resetLeaningState() {
        ClientLeaningData.leanTickDelta = 0;
        ClientLeaningData.stopLeanTickDelta = 0;
        ClientLeaningData.setLeaning(false);
        ClientLeaningData.leanDirection = LeanDirection.NONE;
        ClientLeaningData.prevLeanDirection = LeanDirection.NONE;
        ClientLeaningData.targetLeanAngle = 0;
        ClientLeaningData.currentLeanAngle = 0;
        targetRoll = 0.0f;
        currentRoll = 0.0f;
    }

    private static void smoothCameraRoll(ViewportEvent.ComputeCameraAngles event) {
        float deltaTime = Minecraft.getInstance().getDeltaFrameTime();
        float smoothingFactor = 0.35f;

        currentRoll += (targetRoll - currentRoll) * smoothingFactor * deltaTime;
        event.setRoll(currentRoll);
    }
    public static float getFinalRoll() {
        return currentRoll;
    }
}