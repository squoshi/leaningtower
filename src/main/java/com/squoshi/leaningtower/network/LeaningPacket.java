package com.squoshi.leaningtower.network;

import com.squoshi.leaningtower.capability.Leaning;
import com.squoshi.leaningtower.capability.LeaningCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LeaningPacket {
    private final boolean leaning;

    public LeaningPacket(boolean leaning) {
        this.leaning = leaning;
    }

    public static void encode(LeaningPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.leaning);
    }

    public static LeaningPacket decode(FriendlyByteBuf buffer) {
        return new LeaningPacket(buffer.readBoolean());
    }

    public static void handle(LeaningPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(LeaningCapability.LEANING).ifPresent(leaning -> {
                    leaning.setLeaning(packet.leaning);
                });
            }
        });
        context.setPacketHandled(true);
    }
}
