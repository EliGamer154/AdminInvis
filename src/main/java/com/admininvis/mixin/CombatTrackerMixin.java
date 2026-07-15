package com.admininvis.mixin;

import com.admininvis.AdminInvis;
import com.admininvis.DeathMessageAnonymizer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/**
 * Every player death message (chat broadcast and the victim's death screen) is built here. Rather
 * than guessing which message variant credits the killer, the finished message is scanned for the
 * name of any online player who is currently invisible - by /invis, potion, or anything else - and
 * that name is shown as "Unknown". The victim's own name is never hidden.
 */
@Mixin(CombatTracker.class)
public abstract class CombatTrackerMixin {
	@Shadow
	@Final
	private LivingEntity mob;

	@Inject(method = "getDeathMessage", at = @At("RETURN"), cancellable = true)
	private void admininvis$hideInvisibleKiller(CallbackInfoReturnable<Component> cir) {
		MinecraftServer server = this.mob.level().getServer();
		Component message = cir.getReturnValue();
		if (server == null || message == null) {
			return;
		}

		Set<String> hiddenNames = new HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player != this.mob && player.isInvisible()) {
				hiddenNames.add(player.getGameProfile().name());
				hiddenNames.add(player.getDisplayName().getString());
			}
		}
		if (hiddenNames.isEmpty()) {
			return;
		}

		Component replaced = DeathMessageAnonymizer.replaceNames(message, hiddenNames);
		if (replaced != message) {
			AdminInvis.LOGGER.info("Hid an invisible player's name in a death message: {}", replaced.getString());
			cir.setReturnValue(replaced);
		}
	}
}
