package com.amorcraft.client.model;

import com.amorcraft.AmorCraft;
import com.amorcraft.companion.CompanionEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CompanionModel extends GeoModel<CompanionEntity> {

    @Override
    public ResourceLocation getModelResource(CompanionEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(AmorCraft.MODID, "geo/entity/companion.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CompanionEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(AmorCraft.MODID, "textures/entity/companion.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CompanionEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(AmorCraft.MODID, "animations/entity/companion.animation.json");
    }
}