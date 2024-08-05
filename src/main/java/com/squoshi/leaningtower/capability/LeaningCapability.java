package com.squoshi.leaningtower.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class LeaningCapability {
    public static final Capability<Leaning> LEANING = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Player> event) {
        event.addCapability(new ResourceLocation("leaningtower", "leaning"), new LeaningProvider());
    }

    @SubscribeEvent
    public static void onPlayerCloned(Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(LEANING).ifPresent(oldLeaning -> {
                event.getEntity().getCapability(LEANING).ifPresent(newLeaning -> {
                    newLeaning.copyFrom(oldLeaning);
                });
            });
        }
    }

    public static class LeaningProvider implements ICapabilitySerializable<CompoundTag> {
        private final Leaning instance = new Leaning();
        private final LazyOptional<Leaning> lazyOptional = LazyOptional.of(() -> instance);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == LEANING ? lazyOptional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return instance.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.deserializeNBT(nbt);
        }
    }
}
