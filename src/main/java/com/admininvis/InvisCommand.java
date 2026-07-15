package com.admininvis;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
					boolean nowInvisible = InvisState.get(player.level().getServer()).toggle(player.getUUID());
					if (nowInvisible) {
						player.addEffect(AdminInvis.permanentInvisibility());
						player.sendSystemMessage(Component.literal("Invisibility ON. It stays on through totems, milk and death until you run /invis again."));
					} else {
						player.removeEffect(MobEffects.INVISIBILITY);
						player.sendSystemMessage(Component.literal("Invisibility OFF."));
					}
					return Command.SINGLE_SUCCESS;
				}));
	}
}
