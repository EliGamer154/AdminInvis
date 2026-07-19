package com.admininvis;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public final class InvisCommand {
	private InvisCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("invis")
				.requires(source -> isAdmin(source) || isAllowed(source))
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					toggle(context.getSource(), player);
					return Command.SINGLE_SUCCESS;
				})
				.then(Commands.literal("add")
						.requires(InvisCommand::isAdmin)
						.then(Commands.argument("player", GameProfileArgument.gameProfile())
								.executes(InvisCommand::addToList)))
				.then(Commands.literal("remove")
						.requires(InvisCommand::isAdmin)
						.then(Commands.argument("player", GameProfileArgument.gameProfile())
								.executes(InvisCommand::removeFromList)))
				.then(Commands.literal("list")
						.requires(InvisCommand::isAdmin)
						.executes(InvisCommand::showList))
				.then(Commands.argument("targets", EntityArgument.players())
						.requires(InvisCommand::isAdmin)
						.executes(context -> {
							int count = 0;
							for (ServerPlayer target : EntityArgument.getPlayers(context, "targets")) {
								toggle(context.getSource(), target);
								count++;
							}
							return count;
						})));
	}

	private static boolean isAdmin(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** Non-admins can use /invis on themselves if an admin put them on the list. */
	private static boolean isAllowed(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player != null && InvisState.get(source.getServer()).isAllowed(player.getUUID());
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

	private static int addToList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		InvisState state = InvisState.get(source.getServer());
		int count = 0;
		for (NameAndId profile : GameProfileArgument.getGameProfiles(context, "player")) {
			if (!state.addAllowed(profile.id(), profile.name())) {
				source.sendFailure(Component.literal(profile.name() + " is already on the /invis list."));
				continue;
			}
			count++;
			source.sendSuccess(() -> Component.literal("Added " + profile.name() + " to the /invis list."), true);
			ServerPlayer online = source.getServer().getPlayerList().getPlayer(profile.id());
			if (online != null) {
				online.sendSystemMessage(Component.literal("You can now use /invis to toggle permanent invisibility."));
				// Resync their command tree so /invis shows up in tab-complete immediately
				source.getServer().getCommands().sendCommands(online);
			}
		}
		return count;
	}

	private static int removeFromList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		InvisState state = InvisState.get(source.getServer());
		int count = 0;
		for (NameAndId profile : GameProfileArgument.getGameProfiles(context, "player")) {
			if (!state.removeAllowed(profile.id())) {
				source.sendFailure(Component.literal(profile.name() + " is not on the /invis list."));
				continue;
			}
			count++;
			source.sendSuccess(() -> Component.literal("Removed " + profile.name() + " from the /invis list."), true);
			ServerPlayer online = source.getServer().getPlayerList().getPlayer(profile.id());
			// Losing list access also turns their invisibility off, so nobody is left stuck invisible.
			if (state.clearInvisible(profile.id()) && online != null) {
				online.removeEffect(MobEffects.INVISIBILITY);
			}
			if (online != null) {
				online.sendSystemMessage(Component.literal("You can no longer use /invis."));
				source.getServer().getCommands().sendCommands(online);
			}
		}
		return count;
	}

	private static int showList(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		List<String> names = InvisState.get(source.getServer()).allowedNames();
		source.sendSuccess(() -> Component.literal(names.isEmpty()
				? "No players are on the /invis list."
				: "Players with /invis access (" + names.size() + "): " + String.join(", ", names)), false);
		return names.size();
	}
}
