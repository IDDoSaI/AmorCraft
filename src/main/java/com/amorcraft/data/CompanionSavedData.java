package com.amorcraft.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class CompanionSavedData extends SavedData {

    private static final String DATA_NAME = "amorcraft_companions";

    // Хранилище: characterId -> полные NBT-данные персонажа (отношения, память, инвентарь)
    private final Map<String, CompoundTag> characterProfiles = new HashMap<>();

    public CompanionSavedData() {}

    public static CompanionSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        CompanionSavedData data = new CompanionSavedData();
        if (tag.contains("Profiles")) {
            CompoundTag profilesTag = tag.getCompound("Profiles");
            for (String characterId : profilesTag.getAllKeys()) {
                data.characterProfiles.put(characterId, profilesTag.getCompound(characterId));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag profilesTag = new CompoundTag();
        for (Map.Entry<String, CompoundTag> entry : this.characterProfiles.entrySet()) {
            profilesTag.put(entry.getKey(), entry.getValue());
        }
        tag.put("Profiles", profilesTag);
        return tag;
    }

    public static CompanionSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CompanionSavedData::new, CompanionSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public CompoundTag getProfile(String characterId) {
        return this.characterProfiles.get(characterId);
    }

    public void setProfile(String characterId, CompoundTag profile) {
        this.characterProfiles.put(characterId, profile);
        this.setDirty(); // Помечаем, что данные изменились и мир должен их записать на диск
    }
}