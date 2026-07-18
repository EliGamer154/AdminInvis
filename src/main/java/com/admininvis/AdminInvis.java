package com.admininvis;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminInvis implements ModInitializer {
	public static final String MOD_ID = "admininvis";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> InvisCommand.register(dispatcher));

		// Safety net: LivingEntityMixin re-adds invisibility inside the removal call itself (no visible
		// gap), and this tick handler catches everything else - respawning after death, relogging, or
		// an effect expiring - so the toggle always wins eventually.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			InvisState state = InvisState.get(server);
			if (state.isEmpty()) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (state.isInvisible(player.getUUID()) && !player.hasEffect(MobEffects.INVISIBILITY)) {
					player.addEffect(permanentInvisibility());
				}
			}
		});

		// Loading the classes here forces both mixins to apply now, so a broken injection crashes
		// loudly at startup instead of failing silently on the first kill or totem pop.
		LOGGER.info("AdminInvis initialized, hooked into {} and {}",
				CombatTracker.class.getSimpleName(), net.minecraft.world.entity.LivingEntity.class.getSimpleName());
	}

	/** Infinite duration, no particles, but with the HUD icon so the admin can tell it's on. */
	public static MobEffectInstance permanentInvisibility() {
		return new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false, true);
	}
}
