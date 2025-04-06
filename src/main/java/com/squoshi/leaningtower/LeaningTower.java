package com.squoshi.leaningtower;

import com.mojang.logging.LogUtils;
import com.squoshi.leaningtower.config.LeaningTowerConfig;
import com.squoshi.leaningtower.network.LeaningTowerNetworkWrapper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LeaningTower.MODID)
public class LeaningTower {
    public static final String MODID = "leaningtower";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LeaningTower() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);

        LeaningTowerConfig.registerConfigs();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LeaningTowerNetworkWrapper.register();
    }
}
