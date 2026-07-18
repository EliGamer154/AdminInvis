package com.admininvis;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.effect.MobEffects;

public final class InvisCommand {
	private InvisCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("invis")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					toggle(context.getSource(), player);
					return Command.SINGLE_SUCCESS;
				})
				.then(Commands.argument("targets", EntityArgument.players())
						.executes(context -> {
							int count = 0;
							for (ServerPlayer target : EntityArgument.getPlayers(context, "targets")) {
								toggle(context.getSource(), target);
								count++;
							}
							return count;
						})));
	}

	/** Toggles the target's permanent invisibility, telling both the target and whoever ran the command. */
	private static void toggle(CommandSourceStack source, ServerPlayer target) {
		boolean nowInvisible = InvisState.get(target.level().getServer()).toggle(target.getUUID());
		if (nowInvisible) {
			target.addEffect(AdminInvis.permanentInvisibility());
			target.sendSystemMessage(Component.literal("Invisibility ON. It stays on through totems, milk and death until toggled off with /invis."));
		} else {
			target.removeEffect(MobEffects.INVISIBILITY);
			target.sendSystemMessage(Component.literal("Invisibility OFF."));
		}
		if (source.getPlayer() != target) {
			String name = target.getGameProfile().name();
			source.sendSuccess(() -> Component.literal(nowInvisible
					? "Made " + name + " permanently invisible."
					: "Removed " + name + "'s invisibility."), true);
		}
	}
}
