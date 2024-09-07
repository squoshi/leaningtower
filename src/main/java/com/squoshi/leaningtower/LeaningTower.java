package com.squoshi.leaningtower;

import com.mojang.logging.LogUtils;
import com.squoshi.leaningtower.config.LeaningTowerConfig;
import com.squoshi.leaningtower.network.LeaningTowerNetworkWrapper;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LeaningTower.MODID)
public class LeaningTower {
    public static final String MODID = "leaningtower";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LeaningTower() {
        // Register mod event listeners
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onCommonSetup);

        // Register the configuration
        LeaningTowerConfig.registerConfigs();

        // Register other listeners
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                new ResourceLocation(MODID, "animation"),
                42,
                LeaningTower::registerPlayerAnimation
        );
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LeaningTowerNetworkWrapper.register();
    }

    private static IAnimation registerPlayerAnimation(AbstractClientPlayer player) {
        return new ModifierLayer<>();
    }
}
