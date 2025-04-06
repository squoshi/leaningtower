package com.squoshi.leaningtower.client;

import com.squoshi.leaningtower.LeaningTower;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = LeaningTower.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientInit {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                new ResourceLocation(LeaningTower.MODID, "animation"),
                42,
                (AbstractClientPlayer player) -> new ModifierLayer<IAnimation>()
        );

        MinecraftForge.EVENT_BUS.register(LeaningTowerClientEvents.class);
    }

    @SubscribeEvent
    public static void registerKeybinds(RegisterKeyMappingsEvent event) {
        event.register(LeaningTowerKeyMappings.leanLeft);
        event.register(LeaningTowerKeyMappings.leanRight);
        event.register(LeaningTowerKeyMappings.leftAlt);
        event.register(LeaningTowerKeyMappings.incrementLeft);
        event.register(LeaningTowerKeyMappings.incrementRight);
    }
}
