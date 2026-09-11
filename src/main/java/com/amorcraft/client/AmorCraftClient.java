package com.amorcraft.client;

import com.amorcraft.AmorCraft;
import com.amorcraft.client.renderer.CompanionRenderer;
import com.amorcraft.registry.ModEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = AmorCraft.MODID, dist = Dist.CLIENT)
public class AmorCraftClient {

    public AmorCraftClient(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerRenderers);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        AmorCraft.LOGGER.info("AmorCraft client initialized.");
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMPANION.get(), CompanionRenderer::new);
    }
}