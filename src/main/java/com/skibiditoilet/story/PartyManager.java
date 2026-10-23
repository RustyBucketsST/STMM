package com.skibiditoilet.story;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Parties of up to four players who play missions together. Each player keeps their own story; a mission
 * started by any member brings in the members nearby. Parties are saved with the world; invites are not.
 */
public final class PartyManager extends PersistentState {
    public static final int MAX_SIZE = 4;
    private static final long INVITE_TICKS = 20 * 60;
    private static final String SAVE_ID = "skibidi_parties";

    /** The leader is always the first member. */
    public static final class Party {
        private final List<UUID> members = new ArrayList<>();

        public List<UUID> members() {
            return this.members;
        }

        public UUID leader() {
            return this.members.get(0);
        }
    }

    private record Invite(UUID inviter, long expiresAt) {
    }

    private final Map<UUID, Party> byMember = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();

    public static PartyManager get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(PartyManager::fromNbt, PartyManager::new, SAVE_ID);
    }

    @Nullable
    public Party partyOf(UUID player) {
        return this.byMember.get(player);
    }

    /** The player's party members who are online, not counting the player. */
    public List<ServerPlayerEntity> onlineMembers(ServerPlayerEntity player) {
        Party party = this.partyOf(player.getUuid());
        List<ServerPlayerEntity> online = new ArrayList<>();
        if (party != null) {
            for (UUID member : party.members) {
                ServerPlayerEntity memberPlayer = player.getServer().getPlayerManager().getPlayer(member);
                if (memberPlayer != null && memberPlayer != player) {
                    online.add(memberPlayer);
                }
            }
        }
        return online;
    }

    public enum InviteResult {
        SENT, SELF, NOT_LEADER, ALREADY_IN_PARTY, FULL
    }

    public InviteResult invite(ServerPlayerEntity inviter, ServerPlayerEntity invitee) {
        if (inviter == invitee) {
            return InviteResult.SELF;
        }
        Party party = this.partyOf(inviter.getUuid());
        if (party != null && !party.leader().equals(inviter.getUuid())) {
            return InviteResult.NOT_LEADER;
        }
        if (this.partyOf(invitee.getUuid()) != null) {
            return InviteResult.ALREADY_IN_PARTY;
        }
        if (party != null && party.members.size() >= MAX_SIZE) {
            return InviteResult.FULL;
        }
        this.invites.put(invitee.getUuid(), new Invite(inviter.getUuid(), inviter.getServer().getTicks() + INVITE_TICKS));
        return InviteResult.SENT;
    }

    public enum AcceptResult {
        JOINED, NO_INVITE, ALREADY_IN_PARTY, FULL
    }

    /** Joins the party of whoever invited this player, making one with the inviter as leader if needed. */
    public AcceptResult accept(ServerPlayerEntity invitee) {
        Invite invite = this.takeInvite(invitee);
        if (invite == null) {
            return AcceptResult.NO_INVITE;
        }
        if (this.partyOf(invitee.getUuid()) != null) {
            return AcceptResult.ALREADY_IN_PARTY;
        }
        Party party = this.partyOf(invite.inviter);
        if (party == null) {
            party = new Party();
            party.members.add(invite.inviter);
            this.byMember.put(invite.inviter, party);
        }
        if (party.members.size() >= MAX_SIZE) {
            return AcceptResult.FULL;
        }
        party.members.add(invitee.getUuid());
        this.byMember.put(invitee.getUuid(), party);
        this.markDirty();
        return AcceptResult.JOINED;
    }

    /** Returns who sent the declined invite, or null if there was none. */
    @Nullable
    public UUID decline(ServerPlayerEntity invitee) {
        Invite invite = this.takeInvite(invitee);
        return invite != null ? invite.inviter : null;
    }

    @Nullable
    private Invite takeInvite(ServerPlayerEntity invitee) {
        Invite invite = this.invites.remove(invitee.getUuid());
        return invite != null && invite.expiresAt >= invitee.getServer().getTicks() ? invite : null;
    }

    /** Leaves the party. The next member takes over as leader, and a party of one breaks up. */
    @Nullable
    public Party leave(UUID player) {
        Party party = this.byMember.remove(player);
        if (party == null) {
            return null;
        }
        party.members.remove(player);
        if (party.members.size() == 1) {
            this.byMember.remove(party.members.get(0));
        }
        this.markDirty();
        return party;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList parties = new NbtList();
        for (Party party : this.byMember.values().stream().distinct().toList()) {
            NbtList members = new NbtList();
            for (UUID member : party.members) {
                members.add(NbtHelper.fromUuid(member));
            }
            parties.add(members);
        }
        nbt.put("Parties", parties);
        return nbt;
    }

    private static PartyManager fromNbt(NbtCompound nbt) {
        PartyManager manager = new PartyManager();
        for (NbtElement element : nbt.getList("Parties", NbtElement.LIST_TYPE)) {
            Party party = new Party();
            for (NbtElement member : (NbtList) element) {
                UUID uuid = NbtHelper.toUuid(member);
                party.members.add(uuid);
                manager.byMember.put(uuid, party);
            }
        }
        return manager;
    }
}
