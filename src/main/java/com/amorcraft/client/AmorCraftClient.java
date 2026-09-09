package com.amorcraft.client;

import com.amorcraft.AmorCraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = AmorCraft.MODID, dist = Dist.CLIENT)
public class AmorCraftClient {

    public AmorCraftClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        AmorCraft.LOGGER.info("AmorCraft client initialized.");
    }
}