package com.squoshi.leaningtower.mixin.compat.scguns;

import net.minecraftforge.client.event.ViewportEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.client.handler.GunRenderingHandler;
import com.squoshi.leaningtower.client.LeaningTowerClientEvents;

@Mixin(GunRenderingHandler.class)
public class GunRenderingHandlerMixin {

    @Inject(method = "onCameraSetup", at = @At("TAIL"), remap = false)
    private void addLeaningRoll(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event, CallbackInfo ci) {
        float leanRoll = LeaningTowerClientEvents.getFinalRoll();
        event.setRoll(event.getRoll() + leanRoll); // Add to SCGuns roll, do not replace
    }
}
