package com.amorcraft.relationship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.UUID;

public class CompanionRelationship {

    private final UUID playerUUID;

    // Шкалы отношений: от -100.0 (ненависть/вражда) до +100.0 (полное доверие/любовь)
    private float trust = 0.0F;
    private float affection = 0.0F;

    public CompanionRelationship(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public float getTrust() {
        return trust;
    }

    public void modifyTrust(float amount) {
        this.trust = Mth.clamp(this.trust + amount, -100.0F, 100.0F);
    }

    public float getAffection() {
        return affection;
    }

    public void modifyAffection(float amount) {
        this.affection = Mth.clamp(this.affection + amount, -100.0F, 100.0F);
    }

    // Сохранение в NBT
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("PlayerUUID", this.playerUUID);
        tag.putFloat("Trust", this.trust);
        tag.putFloat("Affection", this.affection);
        return tag;
    }

    // Загрузка из NBT
    public static CompanionRelationship load(CompoundTag tag) {
        UUID uuid = tag.getUUID("PlayerUUID");
        CompanionRelationship rel = new CompanionRelationship(uuid);
        if (tag.contains("Trust")) rel.trust = Mth.clamp(tag.getFloat("Trust"), -100.0F, 100.0F);
        if (tag.contains("Affection")) rel.affection = Mth.clamp(tag.getFloat("Affection"), -100.0F, 100.0F);
        return rel;
    }
}