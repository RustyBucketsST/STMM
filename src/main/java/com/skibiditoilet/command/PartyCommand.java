package com.skibiditoilet.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.skibiditoilet.story.PartyManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/** /party: invite friends to play missions together. Open to every player. */
public final class PartyCommand {
    private PartyCommand() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("party")
                .then(CommandManager.literal("invite")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> invite(context.getSource().getPlayerOrThrow(),
                                        EntityArgumentType.getPlayer(context, "player")))))
                .then(CommandManager.literal("accept")
                        .executes(context -> accept(context.getSource().getPlayerOrThrow())))
                .then(CommandManager.literal("decline")
                        .executes(context -> decline(context.getSource().getPlayerOrThrow())))
                .then(CommandManager.literal("leave")
                        .executes(context -> leave(context.getSource().getPlayerOrThrow())))
                .then(CommandManager.literal("kick")
                        .then(CommandManager.argument("member", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    PartyManager.Party party = player != null
                                            ? PartyManager.get(player.getServer()).partyOf(player.getUuid()) : null;
                                    if (party != null) {
                                        CommandSource.suggestMatching(party.members().stream()
                                                .filter(member -> !member.equals(player.getUuid()))
                                                .map(member -> name(player.getServer(), member)), builder);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> kick(context.getSource().getPlayerOrThrow(),
                                        StringArgumentType.getString(context, "member")))))
                .then(CommandManager.literal("list")
                        .executes(context -> list(context.getSource().getPlayerOrThrow()))));
    }

    private static int invite(ServerPlayerEntity inviter, ServerPlayerEntity invitee) {
        PartyManager.InviteResult result = PartyManager.get(inviter.getServer()).invite(inviter, invitee);
        if (result != PartyManager.InviteResult.SENT) {
            inviter.sendMessage(Text.translatable("party.skibidi.invite." + result.name().toLowerCase(Locale.ROOT),
                    invitee.getDisplayName()).formatted(Formatting.RED));
            return 0;
        }
        inviter.sendMessage(Text.translatable("party.skibidi.invite.sent", invitee.getDisplayName()));
        MutableText accept = button("party.skibidi.accept", "/party accept", Formatting.GREEN);
        MutableText decline = button("party.skibidi.decline", "/party decline", Formatting.RED);
        invitee.sendMessage(Text.translatable("party.skibidi.invite.received", inviter.getDisplayName())
                .append(" ").append(accept).append(" ").append(decline));
        return 1;
    }

    private static MutableText button(String key, String command, Formatting color) {
        return Texts.bracketed(Text.translatable(key)).styled(style -> style.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(command))));
    }

    private static int accept(ServerPlayerEntity player) {
        PartyManager parties = PartyManager.get(player.getServer());
        PartyManager.AcceptResult result = parties.accept(player);
        if (result != PartyManager.AcceptResult.JOINED) {
            player.sendMessage(Text.translatable("party.skibidi.accept." + result.name().toLowerCase(Locale.ROOT))
                    .formatted(Formatting.RED));
            return 0;
        }
        for (ServerPlayerEntity member : parties.onlineMembers(player)) {
            member.sendMessage(Text.translatable("party.skibidi.joined", player.getDisplayName()));
        }
        player.sendMessage(Text.translatable("party.skibidi.you_joined"));
        return 1;
    }

    private static int decline(ServerPlayerEntity player) {
        UUID inviter = PartyManager.get(player.getServer()).decline(player);
        if (inviter == null) {
            player.sendMessage(Text.translatable("party.skibidi.accept.no_invite").formatted(Formatting.RED));
            return 0;
        }
        ServerPlayerEntity inviterPlayer = player.getServer().getPlayerManager().getPlayer(inviter);
        if (inviterPlayer != null) {
            inviterPlayer.sendMessage(Text.translatable("party.skibidi.declined", player.getDisplayName()));
        }
        player.sendMessage(Text.translatable("party.skibidi.you_declined"));
        return 1;
    }

    private static int leave(ServerPlayerEntity player) {
        PartyManager parties = PartyManager.get(player.getServer());
        PartyManager.Party party = parties.leave(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.translatable("party.skibidi.not_in_party").formatted(Formatting.RED));
            return 0;
        }
        tellMembers(player.getServer(), party, Text.translatable("party.skibidi.left", player.getDisplayName()));
        player.sendMessage(Text.translatable("party.skibidi.you_left"));
        return 1;
    }

    private static int kick(ServerPlayerEntity leader, String memberName) throws CommandSyntaxException {
        PartyManager parties = PartyManager.get(leader.getServer());
        PartyManager.Party party = parties.partyOf(leader.getUuid());
        if (party == null || !party.leader().equals(leader.getUuid())) {
            leader.sendMessage(Text.translatable("party.skibidi.invite.not_leader").formatted(Formatting.RED));
            return 0;
        }
        UUID target = party.members().stream()
                .filter(member -> !member.equals(leader.getUuid()) && name(leader.getServer(), member).equalsIgnoreCase(memberName))
                .findFirst().orElse(null);
        if (target == null) {
            leader.sendMessage(Text.translatable("party.skibidi.kick.not_member", memberName).formatted(Formatting.RED));
            return 0;
        }
        parties.leave(target);
        tellMembers(leader.getServer(), party, Text.translatable("party.skibidi.kicked", memberName));
        ServerPlayerEntity targetPlayer = leader.getServer().getPlayerManager().getPlayer(target);
        if (targetPlayer != null) {
            targetPlayer.sendMessage(Text.translatable("party.skibidi.you_were_kicked"));
        }
        return 1;
    }

    private static int list(ServerPlayerEntity player) {
        PartyManager.Party party = PartyManager.get(player.getServer()).partyOf(player.getUuid());
        if (party == null) {
            player.sendMessage(Text.translatable("party.skibidi.not_in_party"));
            return 0;
        }
        MinecraftServer server = player.getServer();
        Text members = Texts.join(party.members(), Text.literal(", "), member -> {
            MutableText name = Text.literal(name(server, member));
            if (member.equals(party.leader())) {
                name.append(" ").append(Text.translatable("party.skibidi.leader")).formatted(Formatting.GOLD);
            }
            return server.getPlayerManager().getPlayer(member) != null ? name : name.formatted(Formatting.GRAY);
        });
        player.sendMessage(Text.translatable("party.skibidi.list", party.members().size(), PartyManager.MAX_SIZE, members));
        return party.members().size();
    }

    private static void tellMembers(MinecraftServer server, PartyManager.Party party, Text message) {
        for (UUID member : party.members()) {
            ServerPlayerEntity memberPlayer = server.getPlayerManager().getPlayer(member);
            if (memberPlayer != null) {
                memberPlayer.sendMessage(message);
            }
        }
    }

    private static String name(MinecraftServer server, UUID player) {
        @Nullable ServerPlayerEntity online = server.getPlayerManager().getPlayer(player);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return server.getUserCache() != null
                ? server.getUserCache().getByUuid(player).map(GameProfile::getName).orElse(player.toString())
                : player.toString();
    }
}
