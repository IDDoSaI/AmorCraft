package com.amorcraft.client.renderer;

import com.amorcraft.client.model.CompanionModel;
import com.amorcraft.companion.CompanionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CompanionRenderer extends GeoEntityRenderer<CompanionEntity> {

    public CompanionRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CompanionModel());
        this.shadowRadius = 0.5F; // Радиус тени под ногами компаньона
    }
}
