package com.admininvis.mixin;

import com.admininvis.InvisState;
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

/**
 * The death message shown in chat (and on the victim's death screen) is built here. If the kill is
 * credited to a player whose /invis toggle is on, the vanilla "X was slain by Y" is swapped for the
 * plain "X died" so the invisible admin's name never appears.
 */
@Mixin(CombatTracker.class)
public abstract class CombatTrackerMixin {
	@Shadow
	@Final
	private LivingEntity mob;

	@Inject(method = "getDeathMessage", at = @At("RETURN"), cancellable = true)
	private void admininvis$hideInvisibleKiller(CallbackInfoReturnable<Component> cir) {
		MinecraftServer server = this.mob.level().getServer();
		if (server == null) {
			return;
		}
		if (this.mob.getKillCredit() instanceof ServerPlayer killer
				&& InvisState.get(server).isInvisible(killer.getUUID())) {
			cir.setReturnValue(Component.translatable("death.attack.generic", this.mob.getDisplayName()));
		}
	}
}
