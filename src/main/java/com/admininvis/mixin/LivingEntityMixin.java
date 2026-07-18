package com.admininvis.mixin;

import com.admininvis.AdminInvis;
import com.admininvis.InvisState;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A totem of undying (and milk, and /effect clear) strips all effects, and waiting for the next
 * tick to re-apply invisibility leaves a one-tick window where the player is briefly visible to
 * everyone. Re-adding the effect inside the removal call itself closes that window: the remove and
 * re-add happen in the same tick, so clients never see the player appear.
 *
 * The /invis off path is unaffected: the command clears the toggle before removing the effect, so
 * these hooks see the toggle as off and don't re-add.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "removeAllEffects", at = @At("RETURN"))
	private void admininvis$keepInvisThroughClearAll(CallbackInfoReturnable<Boolean> cir) {
		admininvis$reapplyIfToggled();
	}

	@Inject(method = "removeEffect", at = @At("RETURN"))
	private void admininvis$keepInvisThroughRemove(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
		if (effect == MobEffects.INVISIBILITY) {
			admininvis$reapplyIfToggled();
		}
	}

	private void admininvis$reapplyIfToggled() {
		if ((Object) this instanceof ServerPlayer player
				&& player.level().getServer() != null
				&& InvisState.get(player.level().getServer()).isInvisible(player.getUUID())
				&& !player.hasEffect(MobEffects.INVISIBILITY)) {
			player.addEffect(AdminInvis.permanentInvisibility());
		}
	}
}
