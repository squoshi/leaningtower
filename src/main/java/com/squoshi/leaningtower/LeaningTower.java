package com.squoshi.leaningtower;

import com.mojang.logging.LogUtils;
import com.squoshi.leaningtower.network.NetworkHandler;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

@Mod(LeaningTower.MODID)
public class LeaningTower {
    public static final String MODID = "leaningtower";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LeaningTower() {
        // Register the event bus for mod events
        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(this);
        NetworkHandler.register();
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                new ResourceLocation(MODID, "animation"),
                42,
                LeaningTower::registerPlayerAnimation
        );
    }

    private static IAnimation registerPlayerAnimation(AbstractClientPlayer player) {
        return new ModifierLayer<>();
    }
}