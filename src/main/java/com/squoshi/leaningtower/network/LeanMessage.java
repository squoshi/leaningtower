package com.squoshi.leaningtower.network;

import com.squoshi.leaningtower.LeanDirection;
import com.squoshi.leaningtower.procedures.LeaningProcedure;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LeanMessage {
    private final LeanDirection direction;
    private final boolean isLeaning;

    public LeanMessage(LeanDirection direction, boolean isLeaning) {
        this.direction = direction;
        this.isLeaning = isLeaning;
    }

    public static void encode(LeanMessage message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.direction);
        buffer.writeBoolean(message.isLeaning);
    }

    public static LeanMessage decode(FriendlyByteBuf buffer) {
        return new LeanMessage(buffer.readEnum(LeanDirection.class), buffer.readBoolean());
    }

    public static void handle(LeanMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> {
                if (message.isLeaning) {
                    LeaningProcedure.startLeaning(player, message.direction);
                } else {
                    LeaningProcedure.stopLeaning(player);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
