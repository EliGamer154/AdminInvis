package com.admininvis;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Set;

/**
 * Detects whether a death message mentions one of the given names. Death messages are translatable
 * components ("%1$s was slain by %2$s", "%1$s fell too far and was finished by %2$s using %3$s", ...)
 * whose arguments carry the entity names, so the walk checks every argument and sibling.
 */
public final class DeathMessageAnonymizer {
	private DeathMessageAnonymizer() {
	}

	public static boolean mentionsAny(Component component, Set<String> names) {
		if (names.contains(component.getString())) {
			return true;
		}
		if (component.getContents() instanceof TranslatableContents translatable) {
			for (Object arg : translatable.getArgs()) {
				if (arg instanceof Component argComponent && mentionsAny(argComponent, names)) {
					return true;
				}
				if (arg instanceof String argString && names.contains(argString)) {
					return true;
				}
			}
		}
		for (Component sibling : component.getSiblings()) {
			if (mentionsAny(sibling, names)) {
				return true;
			}
		}
		return false;
	}
}
