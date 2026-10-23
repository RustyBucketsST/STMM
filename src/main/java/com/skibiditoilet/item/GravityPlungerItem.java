package com.skibiditoilet.item;

import com.skibiditoilet.entity.LaserBeam;
import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Cameraman tech: a plunger with a gravity field in the cup. Hits like a plunger (double damage to toilets)
 * and slows what it hits; right-click yanks a mob over to you from across the room.
 */
public class GravityPlungerItem extends SwordItem {
    private static final double RANGE = 14.0;
    private static final int COOLDOWN_TICKS = 60;
    private static final int FIELD_COLOR = 0xB07CFF;

    public GravityPlungerItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1), attacker);
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.success(stack, true);
        }
        Entity target = LaserBeam.aim(serverWorld, user, user.getRotationVec(1.0f), RANGE);
        if (!(target instanceof LivingEntity living) || target instanceof PlayerEntity) {
            return TypedActionResult.fail(stack);
        }
        // Pulled harder the further away it is, so it lands about at your feet.
        Vec3d toUser = user.getPos().subtract(living.getPos());
        double distance = toUser.length();
        Vec3d pull = toUser.normalize().multiply(0.25 + distance * 0.11);
        living.setVelocity(pull.x, 0.3 + Math.min(0.4, distance * 0.02), pull.z);
        living.velocityModified = true;
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1), user);
        ModFx.beam(serverWorld, user.getEyePos().subtract(0.0, 0.3, 0.0), living.getBoundingBox().getCenter(), FIELD_COLOR, 0.1f);
        ModFx.ring(serverWorld, living.getPos(), 1.5f, FIELD_COLOR);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.GRAVITY_PLUNGER_PULL, SoundCategory.PLAYERS,
                1.0f, 1.0f);
        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        stack.damage(2, user, player -> player.sendToolBreakStatus(hand));
        return TypedActionResult.success(stack, false);
    }
}
