package com.admininvis;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

import java.util.UUID;

/**
 * Dev-environment-only (see the isDevelopmentEnvironment guard in {@link AdminInvis}): spawns a fake
 * player named TestDummy so /invis and death messages can be exercised on a dev server without a
 * real client. Never registered in production.
 */
public final class DevFakePlayerCommand {
	private DevFakePlayerCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("invistest-spawn").executes(context -> {
			MinecraftServer server = context.getSource().getServer();
			GameProfile profile = new GameProfile(UUID.randomUUID(), "TestDummy");
			ServerPlayer fake = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
			server.getPlayerList().placeNewPlayer(new FakeConnection(), fake, CommonListenerCookie.createInitial(profile, false));
			context.getSource().sendSuccess(() -> Component.literal("spawned TestDummy"), false);
			return Command.SINGLE_SUCCESS;
		}));
	}

	/** A connection with no network channel; everything that would touch the channel is a no-op. */
	private static class FakeConnection extends Connection {
		FakeConnection() {
			super(PacketFlow.SERVERBOUND);
		}

		@Override
		public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> info, T listener) {
		}

		@Override
		public void setupOutboundProtocol(ProtocolInfo<?> info) {
		}

		@Override
		public void setListenerForServerboundHandshake(PacketListener listener) {
		}

		@Override
		public void send(Packet<?> packet) {
		}

		@Override
		public void setReadOnly() {
		}

		@Override
		public void handleDisconnection() {
		}

		@Override
		public void flushChannel() {
		}
	}
}
