package com.admininvis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** The set of players whose /invis toggle is on. Saved with the world, so it survives restarts. */
public class InvisState extends SavedData {
	public static final Codec<InvisState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.listOf().fieldOf("invisiblePlayers").forGetter(s -> List.copyOf(s.invisiblePlayers))
	).apply(instance, InvisState::new));

	public static final SavedDataType<InvisState> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(AdminInvis.MOD_ID, "invis_players"), InvisState::new, CODEC, null);

	private final Set<UUID> invisiblePlayers;

	public InvisState() {
		this(List.of());
	}

	private InvisState(List<UUID> invisiblePlayers) {
		this.invisiblePlayers = new HashSet<>(invisiblePlayers);
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
}
