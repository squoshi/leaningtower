package com.squoshi.leaningtower.capability;

import net.minecraft.nbt.CompoundTag;

public class Leaning {
    private boolean isLeaning;

    public boolean isLeaning() {
        return isLeaning;
    }

    public void setLeaning(boolean isLeaning) {
        this.isLeaning = isLeaning;
    }

    public void copyFrom(Leaning other) {
        this.isLeaning = other.isLeaning();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isLeaning", isLeaning);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.isLeaning = nbt.getBoolean("isLeaning");
    }
}
