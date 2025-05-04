package com.squoshi.leaningtower.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import com.squoshi.leaningtower.LeaningTower;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = LeaningTower.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LeaningTowerKeyMappings {
    public static KeyMapping leanLeft = new ToggleKeyMapping("key.leaningtower.lean_left", GLFW.GLFW_KEY_Q, "key.categories.leaningtower", () -> false);
    public static KeyMapping leanRight = new ToggleKeyMapping("key.leaningtower.lean_right", GLFW.GLFW_KEY_E, "key.categories.leaningtower", () -> false);
    public static KeyMapping leftAlt = new KeyMapping("key.leaningtower.left_alt", GLFW.GLFW_KEY_LEFT_ALT, "key.categories.leaningtower");
    public static KeyMapping incrementLeft = new KeyMapping("key.leaningtower.increment_left", GLFW.GLFW_KEY_A, "key.categories.leaningtower");
    public static KeyMapping incrementRight = new KeyMapping("key.leaningtower.increment_right", GLFW.GLFW_KEY_D, "key.categories.leaningtower");
}