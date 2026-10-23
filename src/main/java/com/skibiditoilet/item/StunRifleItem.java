package com.skibiditoilet.item;

import com.skibiditoilet.entity.LaserBeam;
import com.skibiditoilet.registry.ModEffects;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Cameraman tech: a rifle that fires a bolt of electricity. It barely hurts, but whatever it hits is
 * stunned and freezes in place for a few seconds.
 */
public class StunRifleItem extends Item {
    private static final double RANGE = 24.0;
    private static final float DAMAGE = 2.0f;
    private static final int STUN_TICKS = 60;
    private static final int COOLDOWN_TICKS = 30;
    private static final int BEAM_COLOR = 0xFFF27A;

    public StunRifleItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                ModSounds.STUN_RIFLE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        if (world instanceof ServerWorld serverWorld) {
            Entity hit = LaserBeam.fire(serverWorld, user, user.getRotationVec(1.0f), RANGE, DAMAGE, BEAM_COLOR, 0.08f);
            if (hit instanceof LivingEntity target && target.isAlive()) {
                target.addStatusEffect(new StatusEffectInstance(ModEffects.STUNNED, STUN_TICKS), user);
            }
            stack.damage(1, user, player -> player.sendToolBreakStatus(hand));
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
