package com.bin.pvpbot;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.mob.ZombieEntity;

import java.util.List;

public class PvPBotMod implements ModInitializer {

    private static final String BOT_TAG = "BinBot";

    @Override
    public void onInitialize() {
package com.bin.pvpbot;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.TypeFilter;

import java.util.ArrayList;
import java.util.List;

public class PvPBotMod implements ModInitializer {

    private static final String BOT_TAG = "BinBot";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("bot")
                .then(CommandManager.literal("start")
                    .executes(ctx -> spawnBots(ctx.getSource(), registryAccess, 1))
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> spawnBots(
                            ctx.getSource(), registryAccess,
                            IntegerArgumentType.getInteger(ctx, "count")
                        ))
                    )
                )
                .then(CommandManager.literal("kill")
                    .executes(ctx -> killBots(ctx.getSource()))
                )
                .then(CommandManager.literal("list")
                    .executes(ctx -> listBots(ctx.getSource()))
                )
            );
        });
    }

    private int spawnBots(ServerCommandSource source,
                          CommandRegistryAccess registryAccess,
                          int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {

        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = source.getWorld();

        var reg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> protection  = reg.getOrThrow(Enchantments.PROTECTION);
        RegistryEntry<Enchantment> sharpness   = reg.getOrThrow(Enchantments.SHARPNESS);
        RegistryEntry<Enchantment> unbreaking  = reg.getOrThrow(Enchantments.UNBREAKING);
        RegistryEntry<Enchantment> featherFall = reg.getOrThrow(Enchantments.FEATHER_FALLING);

        for (int i = 0; i < count; i++) {
            ZombieEntity bot = new ZombieEntity(EntityType.ZOMBIE, world);

            double angle = (2 * Math.PI / Math.max(count, 1)) * i;
            bot.refreshPositionAndAngles(
                player.getX() + Math.cos(angle) * 3,
                player.getY(),
                player.getZ() + Math.sin(angle) * 3,
                0, 0
            );

            bot.setCustomName(Text.literal("§c" + BOT_TAG + " §7#" + (i + 1)));
            bot.setCustomNameVisible(true);

            bot.equipStack(EquipmentSlot.HEAD,     enchant(new ItemStack(Items.NETHERITE_HELMET),     protection, 4, unbreaking, 3));
            bot.equipStack(EquipmentSlot.CHEST,    enchant(new ItemStack(Items.NETHERITE_CHESTPLATE), protection, 4, unbreaking, 3));
            bot.equipStack(EquipmentSlot.LEGS,     enchant(new ItemStack(Items.NETHERITE_LEGGINGS),   protection, 4, unbreaking, 3));
            bot.equipStack(EquipmentSlot.FEET,     enchant(new ItemStack(Items.NETHERITE_BOOTS),      featherFall, 4, unbreaking, 3));
            bot.equipStack(EquipmentSlot.MAINHAND, enchant(new ItemStack(Items.NETHERITE_SWORD),      sharpness,  5, unbreaking, 3));

            for (EquipmentSlot slot : EquipmentSlot.values())
                bot.setEquipmentDropChance(slot, 0f);

            setAttr(bot.getAttributeInstance(EntityAttributes.MAX_HEALTH),    40.0);
            setAttr(bot.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), 0.35);
            setAttr(bot.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE),  8.0);
            setAttr(bot.getAttributeInstance(EntityAttributes.FOLLOW_RANGE),  64.0);
            bot.setHealth(40.0f);

            world.spawnEntity(bot);
            bot.setTarget(player);
        }

        player.sendMessage(Text.literal(count == 1
            ? "§c[BinBot] §fЯ в деле. Готов надрать тебе задницу."
            : "§c[BinBot] §fНас " + count + ". Тебе не выжить."), false);
        return count;
    }

    private int killBots(ServerCommandSource source) {
        List<ZombieEntity> bots = getBots(source.getWorld());
        if (bots.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§e[BinBot] Нет активных ботов."), false);
            return 0;
        }
        bots.forEach(ZombieEntity::kill);
        int count = bots.size();
        source.sendFeedback(() -> Text.literal("§a[BinBot] §fУничтожено: §c" + count), false);
        return count;
    }

    private int listBots(ServerCommandSource source) {
        List<ZombieEntity> bots = getBots(source.getWorld());
        if (bots.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§e[BinBot] Нет активных ботов."), false);
        } else {
            source.sendFeedback(() -> Text.literal("§a[BinBot] §fАктивных: §c" + bots.size()), false);
            bots.forEach(bot -> source.sendFeedback(
                () -> Text.literal("  §7• " + bot.getCustomName().getString()
                    + " §fHP: §c" + (int) bot.getHealth() + "§7/§c40"),
                false
            ));
        }
        return bots.size();
    }

    private List<ZombieEntity> getBots(ServerWorld world) {
        List<ZombieEntity> result = new ArrayList<>();
        world.getEntitiesByType(TypeFilter.instanceOf(ZombieEntity.class), e -> true)
             .stream()
             .filter(e -> e.getCustomName() != null && e.getCustomName().getString().contains(BOT_TAG))
             .forEach(result::add);
        return result;
    }

    private void setAttr(EntityAttributeInstance inst, double value) {
        if (inst != null) inst.setBaseValue(value);
    }

    private ItemStack enchant(ItemStack stack,
                               RegistryEntry<Enchantment> e1, int l1,
                               RegistryEntry<Enchantment> e2, int l2) {
        ItemEnchantmentsComponent.Builder b = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        b.add(e1, l1);
        b.add(e2, l2);
        stack.set(DataComponentTypes.ENCHANTMENTS, b.build());
        return stack;
    }
}
