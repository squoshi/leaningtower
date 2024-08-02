package com.squoshi.leaningtower;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LeaningTower.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class LeaningTower {
    public static final String MODID = "leaningtower";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LeaningTower() {}
}
