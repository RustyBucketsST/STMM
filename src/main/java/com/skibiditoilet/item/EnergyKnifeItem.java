package com.skibiditoilet.item;

import com.skibiditoilet.fx.ModFx;
import com.skibiditoilet.registry.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;

/** Speakerman gear: a short blade of glowing energy. Quick to swing, and every hit crackles. */
public class EnergyKnifeItem extends SwordItem {
    private static final int BLADE_COLOR = 0x46C8FF;

    public EnergyKnifeItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld() instanceof ServerWorld world) {
            ModFx.sparks(world, target.getBoundingBox().getCenter(), attacker.getEyePos(), BLADE_COLOR);
            world.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.ENERGY_KNIFE_SWING,
                    SoundCategory.PLAYERS, 1.0f, 0.9f + world.random.nextFloat() * 0.2f);
        }
        return super.postHit(stack, target, attacker);
    }

    /** The blade always glows. */
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
