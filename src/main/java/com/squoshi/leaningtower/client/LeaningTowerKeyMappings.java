package com.squoshi.leaningtower.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class LeaningTowerKeyMappings {
    public static KeyMapping leanLeft = new ToggleKeyMapping("key.leaningtower.lean_left", GLFW.GLFW_KEY_Q, "key.category.leaningtower", () -> false);
    public static KeyMapping leanRight = new ToggleKeyMapping("key.leaningtower.lean_right", GLFW.GLFW_KEY_E, "key.category.leaningtower", () -> false);

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(leanLeft);
        event.register(leanRight);
    }
}
