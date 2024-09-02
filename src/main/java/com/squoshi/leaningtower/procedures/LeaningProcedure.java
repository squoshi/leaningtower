package com.squoshi.leaningtower.procedures;

import com.squoshi.leaningtower.LeanDirection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LeaningProcedure {
    public static void startLeaning(ServerPlayer player, LeanDirection direction) {
        // Adjust player position based on leaning direction
        Vec3 directionVector = switch (direction) {
            case LEFT -> new Vec3(-0.2, 0, 0);
            case RIGHT -> new Vec3(0.2, 0, 0);
            default -> Vec3.ZERO;
        };
        player.setPos(player.getX() + directionVector.x, player.getY(), player.getZ() + directionVector.z);
    }

    public static void stopLeaning(ServerPlayer player) {
        // Reset player position or do nothing depending on the game logic
    }
}
