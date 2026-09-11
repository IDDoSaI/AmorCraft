package com.amorcraft.companion.ai;

import com.amorcraft.companion.CompanionEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class CompanionEatFoodGoal extends Goal {

    private final CompanionEntity companion;
    private int eatTime = 0;
    private int foodSlot = -1;

    public CompanionEatFoodGoal(CompanionEntity companion) {
        this.companion = companion;
        // Флаг MOVE означает, что пока компаньон ест, он останавливается
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Начинаем искать еду, если голод опустился ниже 70%
        if (this.companion.getCompanionData().getHunger() >= 70.0F) {
            return false;
        }

        this.foodSlot = findFoodSlot();
        return this.foodSlot != -1;
    }

    @Override
    public boolean canContinueToUse() {
        return this.eatTime > 0 && this.foodSlot != -1;
    }

    @Override
    public void start() {
        this.eatTime = 30; // Процесс еды длится 1.5 секунды (30 тиков)
        this.companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.eatTime--;

        // Каждые 4 тика воспроизводим звук чавканья
        if (this.eatTime % 4 == 0) {
            this.companion.playSound(SoundEvents.GENERIC_EAT, 0.5F, 1.0F);
        }

        // Завершение трапезы
        if (this.eatTime <= 0) {
            finishEating();
        }
    }

    private void finishEating() {
        if (this.foodSlot == -1) return;

        ItemStack stack = this.companion.getInventory().getItem(this.foodSlot);
        FoodProperties food = stack.get(DataComponents.FOOD);

        if (food != null) {
            float currentHunger = this.companion.getCompanionData().getHunger();
            this.companion.getCompanionData().setHunger(currentHunger + food.nutrition() * 5.0F);

            // Тратим 1 единицу еды из инвентаря
            stack.shrink(1);

            // Финальный звук насыщения
            this.companion.playSound(SoundEvents.PLAYER_BURP, 0.5F, 1.0F);
        }

        this.foodSlot = -1;
    }

    // Поиск слота с едой в инвентаре компаньона
    private int findFoodSlot() {
        for (int i = 0; i < this.companion.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.companion.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                return i;
            }
        }
        return -1;
    }
}