package com.squoshi.leaningtower.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair; // Import Pair from Apache Commons
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "leaningtower", bus = Mod.EventBusSubscriber.Bus.MOD)
public class LeaningTowerConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        // Define the client configuration options and the corresponding config spec
        final Pair<Client, ForgeConfigSpec> clientConfig = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientConfig.getRight();
        CLIENT = clientConfig.getLeft();
    }

    // Client-side configuration
    public static class Client {
        public final BooleanValue holdLean;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("LeaningTower client configuration settings")
                    .push("client");

            holdLean = builder
                    .comment("If true, lean key must be held; if false, lean key acts as a toggle.")
                    .define("holdLean", true);

            builder.pop();
        }
    }

    // Method to register configuration
    public static void registerConfigs() {
        LOGGER.info("Registering LeaningTower configuration");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LeaningTowerConfig.CLIENT_SPEC);
    }
}
