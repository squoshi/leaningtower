package com.squoshi.leaningtower.server;

import com.squoshi.leaningtower.LeanDirection;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerLeaningData {
    private static final Map<UUID, LeanDirection> DIRECTION_MAP = new ConcurrentHashMap<>();

    public static void setDirection(ServerPlayer player, LeanDirection dir) {
        DIRECTION_MAP.put(player.getUUID(), dir);
    }

    public static LeanDirection getDirection(ServerPlayer player) {
        return DIRECTION_MAP.getOrDefault(player.getUUID(), LeanDirection.NONE);
    }

    public static void clear(ServerPlayer player) {
        DIRECTION_MAP.remove(player.getUUID());
    }
}
