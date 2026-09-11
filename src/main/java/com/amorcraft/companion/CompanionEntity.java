package com.amorcraft.companion;

import com.amorcraft.companion.ai.CompanionEatFoodGoal;
import com.amorcraft.companion.data.CompanionData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CompanionEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<String> DATA_CHARACTER_ID =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    public static final String DEFAULT_CHARACTER_ID = "companion";

    // Контейнер данных состояния компаньона
    private final CompanionData companionData = new CompanionData();

    private final SimpleContainer inventory = new SimpleContainer(8);

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");

    public CompanionEntity(EntityType<? extends CompanionEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARACTER_ID, DEFAULT_CHARACTER_ID);
    }

    public String getCharacterId() {
        return this.entityData.get(DATA_CHARACTER_ID);
    }

    public void setCharacterId(String characterId) {
        this.entityData.set(DATA_CHARACTER_ID, characterId);
    }

    public CompanionData getCompanionData() {
        return this.companionData;
    }

    // Тик сущности: на сервере обновляем состояние потребностей
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.companionData.tick();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }

        // 1. Сначала получаем предмет в руке игрока
        ItemStack heldItem = player.getItemInHand(hand);

        // 2. Shift + ПКМ: передать предмет из руки игрока в инвентарь компаньона
        if (player.isShiftKeyDown() && !heldItem.isEmpty()) {
            if (!this.level().isClientSide()) {
                ItemStack remaining = this.inventory.addItem(heldItem);
                player.setItemInHand(hand, remaining);
                player.sendSystemMessage(Component.literal(
                        "§b[AmorCraft] §fВы положили предмет в инвентарь §e" + this.getCharacterId()
                ));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 3. Обычный ПКМ с едой: кормим компаньона вручную
        FoodProperties food = heldItem.get(DataComponents.FOOD);
        if (food != null) {
            if (!this.level().isClientSide()) {
                float currentHunger = this.companionData.getHunger();
                float restoredHunger = food.nutrition() * 5.0F;
                this.companionData.setHunger(currentHunger + restoredHunger);

                this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }

                player.sendSystemMessage(Component.literal(
                        "§a[AmorCraft] §fВы покормили §e" + this.getCharacterId() +
                                "§f. Голод: §a" + String.format("%.1f", this.companionData.getHunger()) + "/100"
                ));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 4. Обычный ПКМ с пустой рукой: показываем статус
        if (heldItem.isEmpty() && !this.level().isClientSide()) {
            String status = String.format(
                    "§6[%s] §fГолод: §a%.1f/100 §f| Энергия: §b%.1f/100 §f| Настроение: §d%.1f/100",
                    this.getCharacterId(),
                    this.companionData.getHunger(),
                    this.companionData.getEnergy(),
                    this.companionData.getMood()
            );
            player.sendSystemMessage(Component.literal(status));
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    // Сохранение состояния в мир
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("CharacterId", this.getCharacterId());
        this.companionData.save(compound);
        compound.put("Inventory", this.inventory.createTag(this.registryAccess()));
    }

    // Загрузка состояния из мира
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CharacterId", Tag.TAG_STRING)) {
            this.setCharacterId(compound.getString("CharacterId"));
        }
        this.companionData.load(compound);
        if (compound.contains("Inventory", Tag.TAG_LIST)) {
            this.inventory.fromTag(compound.getList("Inventory", Tag.TAG_COMPOUND), this.registryAccess());
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Приоритет 1: если голоден и есть еда — бросаем всё и едим!
        this.goalSelector.addGoal(1, new CompanionEatFoodGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(WALK_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
