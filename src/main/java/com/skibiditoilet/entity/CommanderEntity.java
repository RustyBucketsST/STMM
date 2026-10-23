package com.skibiditoilet.entity;

import com.skibiditoilet.story.StorySync;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * The Alliance Commander. Stands where they're placed and hands out missions: talking to them opens the
 * story screen. Can't be hurt, and toilets ignore them; only a creative player can remove them.
 */
public class CommanderEntity extends AllianceEntity {
    public CommanderEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return AllianceEntity.createAttributes(20.0, 0.25, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(1, new LookAroundGoal(this));
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            StorySync.openStory(serverPlayer);
        }
        return ActionResult.success(this.getWorld().isClient);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        boolean creativePlayer = source.getAttacker() instanceof PlayerEntity player && player.getAbilities().creativeMode;
        return super.isInvulnerableTo(source) || !(creativePlayer || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY));
    }

    @Override
    public boolean isPartOfGame() {
        // Keeps toilets (and anything else) from picking the Commander as a target.
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }

    @Override
    public boolean shouldRenderName() {
        return true;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }
}
