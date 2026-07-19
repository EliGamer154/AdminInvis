package com.admininvis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Saved with the world: the set of players whose /invis toggle is on, and the allowlist of
 * non-admin players who are permitted to use /invis on themselves.
 */
public class InvisState extends SavedData {
	/** A player on the allowlist. The name is only kept for display in /invis list. */
	public record AllowedPlayer(UUID id, String name) {
		static final Codec<AllowedPlayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(AllowedPlayer::id),
				Codec.STRING.fieldOf("name").forGetter(AllowedPlayer::name)
		).apply(instance, AllowedPlayer::new));
	}

	public static final Codec<InvisState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.listOf().fieldOf("invisiblePlayers").forGetter(s -> List.copyOf(s.invisiblePlayers)),
			AllowedPlayer.CODEC.listOf().optionalFieldOf("allowedPlayers", List.of())
					.forGetter(s -> s.allowedPlayers.entrySet().stream()
							.map(e -> new AllowedPlayer(e.getKey(), e.getValue())).toList())
	).apply(instance, InvisState::new));

	public static final SavedDataType<InvisState> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(AdminInvis.MOD_ID, "invis_players"), InvisState::new, CODEC, null);

	private final Set<UUID> invisiblePlayers;
	private final Map<UUID, String> allowedPlayers;

	public InvisState() {
		this(List.of(), List.of());
	}

	private InvisState(List<UUID> invisiblePlayers, List<AllowedPlayer> allowedPlayers) {
		this.invisiblePlayers = new HashSet<>(invisiblePlayers);
		this.allowedPlayers = new LinkedHashMap<>();
		for (AllowedPlayer allowed : allowedPlayers) {
			this.allowedPlayers.put(allowed.id(), allowed.name());
		}
	}

	public static InvisState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isInvisible(UUID playerId) {
		return invisiblePlayers.contains(playerId);
	}

	public boolean isEmpty() {
		return invisiblePlayers.isEmpty();
	}

	/** @return true if the player is invisible after the toggle */
	public boolean toggle(UUID playerId) {
		boolean nowInvisible;
		if (invisiblePlayers.contains(playerId)) {
			invisiblePlayers.remove(playerId);
			nowInvisible = false;
		} else {
			invisiblePlayers.add(playerId);
			nowInvisible = true;
		}
		setDirty();
		return nowInvisible;
	}

	/** Turns the toggle off if it was on. @return true if it was on */
	public boolean clearInvisible(UUID playerId) {
		boolean wasOn = invisiblePlayers.remove(playerId);
		if (wasOn) {
			setDirty();
		}
		return wasOn;
	}

	public boolean isAllowed(UUID playerId) {
		return allowedPlayers.containsKey(playerId);
	}

	/** @return false if the player was already on the list */
	public boolean addAllowed(UUID playerId, String name) {
		boolean added = allowedPlayers.put(playerId, name) == null;
		if (added) {
			setDirty();
		}
		return added;
	}

	/** @return false if the player was not on the list */
	public boolean removeAllowed(UUID playerId) {
		boolean removed = allowedPlayers.remove(playerId) != null;
		if (removed) {
			setDirty();
		}
		return removed;
	}

	public List<String> allowedNames() {
		return List.copyOf(allowedPlayers.values());
	}
}
