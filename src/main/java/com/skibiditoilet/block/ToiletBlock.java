package com.skibiditoilet.block;

import com.skibiditoilet.character.CharacterSync;
import com.skibiditoilet.entity.SeatEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.Map;

/** A placeable toilet. Right-click to sit on it, sneak to get up. */
public class ToiletBlock extends HorizontalFacingBlock {
    // Matches models/block/toilet.json, with the bowl facing north.
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5, 0, 4, 11, 6, 12),   // pedestal
            Block.createCuboidShape(3, 6, 1, 13, 10, 13),  // bowl and rim
            Block.createCuboidShape(3, 9, 12, 13, 16, 16)); // tank

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            SHAPES.put(direction, rotate(NORTH_SHAPE, direction));
        }
    }

    public ToiletBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isSneaking() || player.hasVehicle()) {
            return ActionResult.PASS;
        }
        // Consume the click even if someone is already sitting here, so the client and
        // server agree and the held item isn't used on the toilet.
        if (!world.isClient && SeatEntity.sitDown(world, pos, player) && player instanceof ServerPlayerEntity serverPlayer) {
            // Sitting on a toilet is where you change your look.
            CharacterSync.openCustomizer(serverPlayer);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    /** Rotates a north-facing shape the same way the blockstate's "y" rotation turns the model. */
    private static VoxelShape rotate(VoxelShape shape, Direction facing) {
        VoxelShape[] result = {VoxelShapes.empty()};
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> result[0] = VoxelShapes.union(result[0], switch (facing) {
            case EAST -> VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX);
            case SOUTH -> VoxelShapes.cuboid(1 - maxX, minY, 1 - maxZ, 1 - minX, maxY, 1 - minZ);
            case WEST -> VoxelShapes.cuboid(minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX);
            default -> VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
        }));
        return result[0];
    }
}
