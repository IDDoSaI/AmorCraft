package com.amorcraft.companion.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

public class CompanionData {

    // Базовые шкалы потребностей (от 0.0 до 100.0)
    private float hunger = 100.0F;
    private float energy = 100.0F;
    private float mood = 80.0F;

    // Счётчик тиков для оптимизированного обновления (раздел 46 дорожной карты)
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL_TICKS = 20; // 1 раз в секунду

    public CompanionData() {}

    // Периодическое обновление потребностей на сервере
    public void tick() {
        this.tickCounter++;
        if (this.tickCounter >= UPDATE_INTERVAL_TICKS) {
            this.tickCounter = 0;
            updateNeeds();
        }
    }

    private void updateNeeds() {
        // Медленное естественное падение голода и энергии
        // За 1 игровой день (20 минут = 1200 секунд) убудет около 24 единиц
        this.hunger = Mth.clamp(this.hunger - 0.02F, 0.0F, 100.0F);
        this.energy = Mth.clamp(this.energy - 0.015F, 0.0F, 100.0F);

        // Настроение слегка снижается, если голод падает ниже нормы (< 40.0)
        if (this.hunger < 40.0F) {
            this.mood = Mth.clamp(this.mood - 0.03F, 0.0F, 100.0F);
        }
    }

    // Сохранение в NBT (мир)
    public void save(CompoundTag compound) {
        CompoundTag dataTag = new CompoundTag();
        dataTag.putFloat("Hunger", this.hunger);
        dataTag.putFloat("Energy", this.energy);
        dataTag.putFloat("Mood", this.mood);
        compound.put("CompanionData", dataTag);
    }

    // Загрузка из NBT
    public void load(CompoundTag compound) {
        if (compound.contains("CompanionData", Tag.TAG_COMPOUND)) {
            CompoundTag dataTag = compound.getCompound("CompanionData");
            if (dataTag.contains("Hunger", Tag.TAG_FLOAT)) {
                this.hunger = Mth.clamp(dataTag.getFloat("Hunger"), 0.0F, 100.0F);
            }
            if (dataTag.contains("Energy", Tag.TAG_FLOAT)) {
                this.energy = Mth.clamp(dataTag.getFloat("Energy"), 0.0F, 100.0F);
            }
            if (dataTag.contains("Mood", Tag.TAG_FLOAT)) {
                this.mood = Mth.clamp(dataTag.getFloat("Mood"), 0.0F, 100.0F);
            }
        }
    }

    // Геттеры и сеттеры
    public float getHunger() {
        return hunger;
    }

    public void setHunger(float hunger) {
        this.hunger = Mth.clamp(hunger, 0.0F, 100.0F);
    }

    public float getEnergy() {
        return energy;
    }

    public void setEnergy(float energy) {
        this.energy = Mth.clamp(energy, 0.0F, 100.0F);
    }

    public float getMood() {
        return mood;
    }

    public void setMood(float mood) {
        this.mood = Mth.clamp(mood, 0.0F, 100.0F);
    }
}