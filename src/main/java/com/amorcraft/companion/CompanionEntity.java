package com.amorcraft.companion;

import com.amorcraft.companion.ai.CompanionEatFoodGoal;
import com.amorcraft.companion.data.CompanionData;
import com.amorcraft.relationship.CompanionRelationship;
import com.amorcraft.data.CompanionSavedData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.damagesource.DamageSource;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CompanionEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<String> DATA_CHARACTER_ID =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    public static final String DEFAULT_CHARACTER_ID = "companion";

    // Контейнер данных состояния компаньона
    private final CompanionData companionData = new CompanionData();

    private final SimpleContainer inventory = new SimpleContainer(8);

    // Карта отношений: UUID игрока -> данные отношений
    private final Map<UUID, CompanionRelationship> relationships = new HashMap<>();
    // Флаг, загружена ли память из мира для этого тела
    private boolean isLoadedFromWorldData = false;

    public CompanionRelationship getOrCreateRelationship(UUID playerUUID) {
        return this.relationships.computeIfAbsent(playerUUID, CompanionRelationship::new);
    }

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
            // Если тело только что появилось — подтягиваем память из мира
            if (!this.isLoadedFromWorldData) {
                this.loadFromWorldData();
                this.isLoadedFromWorldData = true;
            }
            this.companionData.tick();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // 1. Shift + ПКМ: передать предмет в инвентарь компаньона
        if (player.isShiftKeyDown() && !heldItem.isEmpty()) {
            if (!this.level().isClientSide()) {
                boolean isFood = heldItem.has(DataComponents.FOOD);
                int countBefore = heldItem.getCount();

                // Пробуем положить предмет в инвентарь компаньона
                ItemStack remaining = this.inventory.addItem(heldItem);
                int transferred = countBefore - remaining.getCount();
                player.setItemInHand(hand, remaining);

                // Если хотя бы 1 предмет поместился
                if (transferred > 0) {
                    CompanionRelationship rel = this.getOrCreateRelationship(player.getUUID());

                    if (isFood) {
                        // Реакция на еду: если компаньон голодал (< 50%), благодарность удваивается!
                        boolean wasStarving = this.companionData.getHunger() < 50.0F;
                        float trustGain = wasStarving ? 5.0F : 2.0F;
                        float affectionGain = wasStarving ? 4.0F : 2.0F;
                        float moodGain = wasStarving ? 10.0F : 4.0F;

                        rel.modifyTrust(trustGain);
                        rel.modifyAffection(affectionGain);
                        this.companionData.setMood(this.companionData.getMood() + moodGain);

                        String extra = wasStarving ? " §e(он очень голодал и искренне благодарен!)" : "";
                        player.sendSystemMessage(Component.literal(
                                "§a[AmorCraft] §fВы поделились едой с §e" + this.getCharacterId() + "§f" + extra +
                                        "\n  Доверие: §e" + String.format("%.1f", rel.getTrust()) +
                                        " §f| Симпатия: §d" + String.format("%.1f", rel.getAffection())
                        ));
                    } else {
                        // Если передали обычный предмет (не еду)
                        player.sendSystemMessage(Component.literal(
                                "§b[AmorCraft] §fВы положили предмет в инвентарь §e" + this.getCharacterId()
                        ));
                        this.syncToWorldData();
                    }
                } else {
                    // Инвентарь полон
                    player.sendSystemMessage(Component.literal(
                            "§c[AmorCraft] У §e" + this.getCharacterId() + " §cнет свободного места в инвентаре!"
                    ));
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 2. Обычный клик пустой рукой: посмотреть краткий статус
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

    // Синхронизация текущего состояния в постоянную память мира
    public void syncToWorldData() {
        if (this.level() instanceof ServerLevel serverLevel) {
            CompoundTag profile = new CompoundTag();
            profile.putString("CharacterId", this.getCharacterId());
            this.companionData.save(profile);

            // Сохраняем отношения
            CompoundTag relTag = new CompoundTag();
            for (Map.Entry<UUID, CompanionRelationship> entry : this.relationships.entrySet()) {
                relTag.put(entry.getKey().toString(), entry.getValue().save());
            }
            profile.put("Relationships", relTag);

            // Сохраняем инвентарь
            profile.put("Inventory", this.inventory.createTag(this.registryAccess()));

            CompanionSavedData.get(serverLevel).setProfile(this.getCharacterId(), profile);
        }
    }

    // Восстановление памяти и состояния из постоянной памяти мира
    public void loadFromWorldData() {
        if (this.level() instanceof ServerLevel serverLevel) {
            CompoundTag profile = CompanionSavedData.get(serverLevel).getProfile(this.getCharacterId());
            if (profile != null) {
                this.companionData.load(profile);

                // Восстанавливаем отношения
                if (profile.contains("Relationships", Tag.TAG_COMPOUND)) {
                    CompoundTag relTag = profile.getCompound("Relationships");
                    this.relationships.clear();
                    for (String key : relTag.getAllKeys()) {
                        CompoundTag singleRel = relTag.getCompound(key);
                        CompanionRelationship rel = CompanionRelationship.load(singleRel);
                        this.relationships.put(rel.getPlayerUUID(), rel);
                    }
                }

                // Восстанавливаем инвентарь
                if (profile.contains("Inventory", Tag.TAG_LIST)) {
                    this.inventory.fromTag(profile.getList("Inventory", Tag.TAG_COMPOUND), this.registryAccess());
                }
            }
        }
    }

    // Сохранение состояния в мир
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("CharacterId", this.getCharacterId());
        this.companionData.save(compound);
        compound.put("Inventory", this.inventory.createTag(this.registryAccess()));
        CompoundTag relTag = new CompoundTag();
        for (Map.Entry<UUID, CompanionRelationship> entry : this.relationships.entrySet()) {
            relTag.put(entry.getKey().toString(), entry.getValue().save());
        }
        compound.put("Relationships", relTag);
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
        if (compound.contains("Relationships", Tag.TAG_COMPOUND)) {
            CompoundTag relTag = compound.getCompound("Relationships");
            this.relationships.clear();
            for (String key : relTag.getAllKeys()) {
                CompoundTag singleRel = relTag.getCompound(key);
                CompanionRelationship rel = CompanionRelationship.load(singleRel);
                this.relationships.put(rel.getPlayerUUID(), rel);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide() && source.getEntity() instanceof Player player) {
            CompanionRelationship rel = this.getOrCreateRelationship(player.getUUID());
            rel.modifyTrust(-10.0F);
            rel.modifyAffection(-5.0F);
            this.companionData.setMood(this.companionData.getMood() - 15.0F);

            player.sendSystemMessage(Component.literal(
                    "§c[AmorCraft] §e" + this.getCharacterId() + " §cзапомнил ваш удар! (Доверие: " +
                            String.format("%.1f", rel.getTrust()) + ", Симпатия: " + String.format("%.1f", rel.getAffection()) + ")"
            ));
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            if (source.getEntity() instanceof Player killer) {
                CompanionRelationship rel = this.getOrCreateRelationship(killer.getUUID());
                // Смертельный удар от игрока — сокрушительный удар по доверию и симпатии
                rel.modifyTrust(-50.0F);
                rel.modifyAffection(-40.0F);
                this.companionData.setMood(0.0F);

                killer.sendSystemMessage(Component.literal(
                        "§4[AmorCraft] §cВы убили §e" + this.getCharacterId() +
                                "§c! Его тело погибло, но память о вашем предательстве осталась в мире... (Доверие: " +
                                String.format("%.1f", rel.getTrust()) + ")"
                ));
            }

            // Навечно фиксируем финальное состояние и память в мире
            this.syncToWorldData();
        }
        super.die(source);
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
