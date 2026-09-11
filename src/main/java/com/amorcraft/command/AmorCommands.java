package com.amorcraft.command;

import com.amorcraft.companion.CompanionEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.List;

public class AmorCommands {

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("amor")
                        .requires(source -> source.hasPermission(2)) // Доступно с правами оператора/читов
                        .then(Commands.literal("companion")
                                // /amor companion info
                                .then(Commands.literal("info")
                                        .executes(ctx -> showInfo(ctx.getSource()))
                                )
                                // /amor companion sethunger <0-100>
                                .then(Commands.literal("sethunger")
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 100.0F))
                                                .executes(ctx -> setHunger(ctx.getSource(), FloatArgumentType.getFloat(ctx, "value")))
                                        )
                                )
                                // /amor companion setenergy <0-100>
                                .then(Commands.literal("setenergy")
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 100.0F))
                                                .executes(ctx -> setEnergy(ctx.getSource(), FloatArgumentType.getFloat(ctx, "value")))
                                        )
                                )
                        )
        );
    }

    private static int showInfo(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionEntity companion = getTargetCompanion(player);
        if (companion == null) {
            source.sendFailure(Component.literal("§c[AmorCraft] Поблизости (в радиусе 10 блоков) нет компаньона!"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "§6=== Статус компаньона [" + companion.getCharacterId() + "] ===\n" +
                        "§fГолод: §a" + String.format("%.1f", companion.getCompanionData().getHunger()) + "/100\n" +
                        "§fЭнергия: §b" + String.format("%.1f", companion.getCompanionData().getEnergy()) + "/100\n" +
                        "§fНастроение: §d" + String.format("%.1f", companion.getCompanionData().getMood()) + "/100\n" +
                        "§fПредметы в инвентаре: " + formatInventory(companion)
        ), false);

        return 1;
    }

    private static int setHunger(CommandSourceStack source, float value) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionEntity companion = getTargetCompanion(player);
        if (companion == null) {
            source.sendFailure(Component.literal("§c[AmorCraft] Поблизости нет компаньона!"));
            return 0;
        }

        companion.getCompanionData().setHunger(value);
        source.sendSuccess(() -> Component.literal(
                "§a[AmorCraft] Голод компаньона установлен на: §e" + value
        ), true);

        return 1;
    }

    private static int setEnergy(CommandSourceStack source, float value) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CompanionEntity companion = getTargetCompanion(player);
        if (companion == null) {
            source.sendFailure(Component.literal("§c[AmorCraft] Поблизости нет компаньона!"));
            return 0;
        }

        companion.getCompanionData().setEnergy(value);
        source.sendSuccess(() -> Component.literal(
                "§a[AmorCraft] Энергия компаньона установлена на: §e" + value
        ), true);

        return 1;
    }

    // Ищет ближайшего компаньона в радиусе 10 блоков
    private static CompanionEntity getTargetCompanion(ServerPlayer player) {
        List<CompanionEntity> list = player.serverLevel().getEntitiesOfClass(
                CompanionEntity.class,
                player.getBoundingBox().inflate(10.0D)
        );
        if (list.isEmpty()) return null;
        list.sort(Comparator.comparingDouble(c -> c.distanceToSqr(player)));
        return list.get(0);
    }

    private static String formatInventory(CompanionEntity companion) {
        StringBuilder sb = new StringBuilder();
        boolean hasItems = false;
        for (int i = 0; i < companion.getInventory().getContainerSize(); i++) {
            ItemStack stack = companion.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                hasItems = true;
                sb.append("\n  - Слот ").append(i).append(": ").append(stack.getHoverName().getString()).append(" x").append(stack.getCount());
            }
        }
        return hasItems ? sb.toString() : "§7[Пусто]";
    }
}