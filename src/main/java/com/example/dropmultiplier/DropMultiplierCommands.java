package com.example.dropmultiplier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class DropMultiplierCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dropmultiplier")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("reload")
                        .executes(ctx -> {
                            DropMultiplierMod.CONFIG = DropMultiplierConfig.load();
                            ctx.getSource().sendFeedback(() -> Text.literal("DropMultiplier config reloaded."), false);
                            return 1;
                        }))
                .then(CommandManager.literal("set")
                        .then(CommandManager.literal("block")
                                .then(CommandManager.argument("block_id", IdentifierArgumentType.identifier())
                                        .then(CommandManager.argument("multiplier", IntegerArgumentType.integer(DropMultiplierConfig.MIN_MULTIPLIER, DropMultiplierConfig.MAX_MULTIPLIER))
                                                .executes(ctx -> {
                                                    Identifier id = IdentifierArgumentType.getIdentifier(ctx, "block_id");
                                                    int m = IntegerArgumentType.getInteger(ctx, "multiplier");

                                                    if (!Registries.BLOCK.containsId(id)) {
                                                        ctx.getSource().sendError(Text.literal("Unknown block id: " + id));
                                                        return 0;
                                                    }

                                                    DropMultiplierMod.CONFIG.setBlockMultiplier(id, m);
                                                    DropMultiplierMod.CONFIG.save();
                                                    ctx.getSource().sendFeedback(() -> Text.literal("Block multiplier for " + id + " set to " + m + "x."), false);
                                                    return 1;
                                                }))))
                        .then(CommandManager.literal("entity")
                                .then(CommandManager.argument("entity_id", IdentifierArgumentType.identifier())
                                        .then(CommandManager.argument("multiplier", IntegerArgumentType.integer(DropMultiplierConfig.MIN_MULTIPLIER, DropMultiplierConfig.MAX_MULTIPLIER))
                                                .executes(ctx -> {
                                                    Identifier id = IdentifierArgumentType.getIdentifier(ctx, "entity_id");
                                                    int m = IntegerArgumentType.getInteger(ctx, "multiplier");

                                                    if (!Registries.ENTITY_TYPE.containsId(id)) {
                                                        ctx.getSource().sendError(Text.literal("Unknown entity id: " + id));
                                                        return 0;
                                                    }

                                            DropMultiplierMod.CONFIG.setEntityMultiplier(id, m);
                                            DropMultiplierMod.CONFIG.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("Entity multiplier for " + id + " set to " + m + "x."), false);
                                            return 1;
                                        })))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.literal("block")
                                .then(CommandManager.argument("block_id", IdentifierArgumentType.identifier())
                                        .executes(ctx -> {
                                            Identifier id = IdentifierArgumentType.getIdentifier(ctx, "block_id");
                                            DropMultiplierMod.CONFIG.removeBlockMultiplier(id);
                                            DropMultiplierMod.CONFIG.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("Removed block multiplier for " + id + "."), false);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("entity")
                                .then(CommandManager.argument("entity_id", IdentifierArgumentType.identifier())
                                        .executes(ctx -> {
                                            Identifier id = IdentifierArgumentType.getIdentifier(ctx, "entity_id");
                                            DropMultiplierMod.CONFIG.removeEntityMultiplier(id);
                                            DropMultiplierMod.CONFIG.save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("Removed entity multiplier for " + id + "."), false);
                                            return 1;
                                        }))))
        );
    }
}
