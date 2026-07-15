package com.admininvis;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Rewrites a death message so that any of the given names is shown as "Unknown". Death messages are
 * translatable components ("%1$s was slain by %2$s", "%1$s fell too far and was finished by %2$s
 * using %3$s", ...) whose arguments carry the entity names, so the walk swaps out any argument or
 * sibling that renders as a hidden name. The replacement deliberately drops the original component's
 * style: a player name's style carries hover/click events that would still leak who it was.
 */
public final class DeathMessageAnonymizer {
	public static final String REPLACEMENT = "Unknown";

	private DeathMessageAnonymizer() {
	}

	/** @return the rewritten message, or the same instance if no hidden name appears in it */
	public static Component replaceNames(Component component, Set<String> hiddenNames) {
		if (hiddenNames.contains(component.getString())) {
			return Component.literal(REPLACEMENT);
		}

		boolean changed = false;
		MutableComponent rebuilt;
		if (component.getContents() instanceof TranslatableContents translatable) {
			Object[] args = translatable.getArgs();
			Object[] newArgs = new Object[args.length];
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Component argComponent) {
					Component swapped = replaceNames(argComponent, hiddenNames);
					changed |= swapped != argComponent;
					newArgs[i] = swapped;
				} else if (args[i] instanceof String argString && hiddenNames.contains(argString)) {
					newArgs[i] = REPLACEMENT;
					changed = true;
				} else {
					newArgs[i] = args[i];
				}
			}
			rebuilt = Component.translatableWithFallback(translatable.getKey(), translatable.getFallback(), newArgs);
		} else {
			rebuilt = component.plainCopy();
		}

		List<Component> newSiblings = new ArrayList<>();
		for (Component sibling : component.getSiblings()) {
			Component swapped = replaceNames(sibling, hiddenNames);
			changed |= swapped != sibling;
			newSiblings.add(swapped);
		}

		if (!changed) {
			return component;
		}
		rebuilt.setStyle(component.getStyle());
		newSiblings.forEach(rebuilt::append);
		return rebuilt;
	}
}
