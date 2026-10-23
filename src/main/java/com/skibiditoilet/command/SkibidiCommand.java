package com.skibiditoilet.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.skibiditoilet.character.CharacterData;
import com.skibiditoilet.character.CharacterSync;
import com.skibiditoilet.power.PowerData;
import com.skibiditoilet.power.PowerManager;
import com.skibiditoilet.power.PowerSync;
import com.skibiditoilet.story.Mission;
import com.skibiditoilet.story.StoryData;
import com.skibiditoilet.story.StoryMissions;
import com.skibiditoilet.story.StorySync;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * /skibidi admin commands (operators only): character, story and power resets, skipping ahead in the story,
 * and handing out points.
 */
public final class SkibidiCommand {
    private SkibidiCommand() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("skibidi")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("character")
                        .then(CommandManager.literal("reset")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .executes(context -> reset(context.getSource(),
                                                EntityArgumentType.getPlayers(context, "targets"))))))
                .then(CommandManager.literal("story")
                        .then(CommandManager.literal("reset")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .executes(context -> resetStory(context.getSource(),
                                                EntityArgumentType.getPlayers(context, "targets")))))
                        .then(CommandManager.literal("complete")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .then(CommandManager.argument("mission", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        StoryMissions.all().stream().map(Mission::id), builder))
                                                .executes(context -> completeStory(context.getSource(),
                                                        EntityArgumentType.getPlayers(context, "targets"),
                                                        StringArgumentType.getString(context, "mission")))))))
                .then(CommandManager.literal("powers")
                        .then(CommandManager.literal("reset")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .executes(context -> resetPowers(context.getSource(),
                                                EntityArgumentType.getPlayers(context, "targets")))))
                        .then(CommandManager.literal("points")
                                .then(CommandManager.argument("targets", EntityArgumentType.players())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> givePoints(context.getSource(),
                                                        EntityArgumentType.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))));
    }

    /** Wipes the character so the creator opens again. */
    private static int reset(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity player : targets) {
            CharacterSync.set(player, CharacterData.DEFAULT);
        }
        source.sendFeedback(() -> Text.translatable("commands.skibidi.character.reset", targets.size()), true);
        return targets.size();
    }

    /** Wipes story progress and rewards. (Points earned stay; see resetPowers.) */
    private static int resetStory(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity player : targets) {
            StorySync.set(player, StoryData.DEFAULT);
        }
        source.sendFeedback(() -> Text.translatable("commands.skibidi.story.reset", targets.size()), true);
        return targets.size();
    }

    /** Marks every mission up to and including this one as finished, with their rewards ready to claim. */
    private static int completeStory(ServerCommandSource source, Collection<ServerPlayerEntity> targets, String id) {
        Mission last = StoryMissions.get(id);
        if (last == null) {
            source.sendError(Text.translatable("story.skibidi.error.unknown"));
            return 0;
        }
        for (ServerPlayerEntity player : targets) {
            StoryData data = StoryData.of(player);
            for (Mission mission : StoryMissions.all()) {
                data = data.withCompleted(mission.id());
                if (mission == last) {
                    break;
                }
            }
            StorySync.set(player, data);
        }
        source.sendFeedback(() -> Text.translatable("commands.skibidi.story.complete", targets.size(),
                Text.translatable(last.translationKey())), true);
        return targets.size();
    }

    /** Wipes points, unlocked powers and built tech. */
    private static int resetPowers(ServerCommandSource source, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity player : targets) {
            PowerSync.set(player, PowerData.DEFAULT);
        }
        source.sendFeedback(() -> Text.translatable("commands.skibidi.powers.reset", targets.size()), true);
        return targets.size();
    }

    private static int givePoints(ServerCommandSource source, Collection<ServerPlayerEntity> targets, int amount) {
        for (ServerPlayerEntity player : targets) {
            PowerManager.addPoints(player, amount);
        }
        source.sendFeedback(() -> Text.translatable("commands.skibidi.powers.points", amount, targets.size()), true);
        return targets.size();
    }
}
