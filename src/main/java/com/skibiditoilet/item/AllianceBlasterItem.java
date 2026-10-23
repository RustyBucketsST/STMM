package com.skibiditoilet.item;

import com.skibiditoilet.entity.LaserBeam;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Right-click to fire an instant laser beam that hits the first mob in its path. */
public class AllianceBlasterItem extends Item {
    private static final double RANGE = 32.0;
    private static final float DAMAGE = 6.0f;
    private static final int COOLDOWN_TICKS = 10;
    private static final int BEAM_COLOR = 0x4FC8FF;

    public AllianceBlasterItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                ModSounds.ALLIANCE_BLASTER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        if (world instanceof ServerWorld serverWorld) {
            LaserBeam.fire(serverWorld, user, user.getRotationVec(1.0f), RANGE, DAMAGE, BEAM_COLOR, 0.12f);
            stack.damage(1, user, player -> player.sendToolBreakStatus(hand));
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
