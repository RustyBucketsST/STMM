package com.skibiditoilet.character;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

/**
 * Until a player finishes the creator they can't be hurt, and (so a modified client can't skip the
 * creator and play invincible) they can't hurt anything or interact with the world either.
 */
public final class CreatorRules {
    private CreatorRules() {
    }

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity attacker && !CharacterData.of(attacker).created()) {
                return false;
            }
            // Damage like the void and /kill still gets through.
            return !(entity instanceof ServerPlayerEntity player)
                    || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    || CharacterData.of(player).created();
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                notCreated(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                notCreated(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                notCreated(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) ->
                notCreated(player) ? TypedActionResult.fail(player.getStackInHand(hand)) : TypedActionResult.pass(player.getStackInHand(hand)));
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> !notCreated(player));
    }

    /** Checked on the server only; the client's copy of the flag can't be trusted. */
    private static boolean notCreated(PlayerEntity player) {
        return player instanceof ServerPlayerEntity serverPlayer && !CharacterData.of(serverPlayer).created();
    }
}
