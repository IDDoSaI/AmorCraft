package com.amorcraft;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.amorcraft.command.AmorCommands;
import com.amorcraft.registry.ModEntities;
import com.amorcraft.companion.CompanionEntity;

@Mod(AmorCraft.MODID)
public class AmorCraft {

    public static final String MODID = "amorcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AmorCraft(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("AmorCraft is initializing.");

        ModEntities.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerAttributes);
        NeoForge.EVENT_BUS.addListener(AmorCommands::register);
    }
    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.COMPANION.get(), CompanionEntity.createAttributes().build());
    }
}