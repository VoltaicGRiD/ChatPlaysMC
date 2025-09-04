package com.voltaicgrid.chatplaysmc;



import net.fabricmc.fabric.api.registry.FlattenableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Identifier;

public class PaxelItem extends Item {
    private final int mineHeight;
    private final int mineWidth;
    private final int miningLevel;
    private final int enchantability;


    public PaxelItem(Item.Settings settings, int mineHeight, int mineWidth, int miningLevel, int enchantability) {
        super(settings);

        this.mineHeight = mineHeight;
        this.mineWidth = mineWidth;
        this.miningLevel = miningLevel;
        this.enchantability = enchantability;
    }

    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    public int getEnchantability() {
        return enchantability;
    }

    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (world.isClient) return ActionResult.PASS;

        BlockPos pos = ctx.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = ctx.getPlayer();
        ItemStack stack = ctx.getStack();

        // 1) AXE: try to strip by deriving the stripped block id
        BlockState stripped = deriveStrippedState(state);
        if (stripped != null) {
            world.setBlockState(pos, stripped, Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
            damageOnce(stack, player, ctx.getHand());
            return ActionResult.SUCCESS;
        }

        // 2) SHOVEL: path (air above required)
        if (world.getBlockState(pos.up()).isAir()) {
            BlockState path = flattenToPath(state);
            if (path != null) {
                world.setBlockState(pos, path, Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
                damageOnce(stack, player, ctx.getHand());
                return ActionResult.SUCCESS;
            }
        }

        // 3) HOE: till (air above required)
        if (world.getBlockState(pos.up()).isAir()) {
            BlockState tilled = till(state);
            if (tilled != null) {
                world.setBlockState(pos, tilled, Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                damageOnce(stack, player, ctx.getHand());
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }

    private static void damageOnce(ItemStack stack, PlayerEntity player, Hand hand) {
        //stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
    }

    /** Derive stripped state by prefixing "stripped_" to the block id (covers logs/wood/stems/hyphae/bamboo). */
    private static BlockState deriveStrippedState(BlockState state) {
        Block block = state.getBlock();
        Identifier id = Registries.BLOCK.getId(block);
        String path = id.getPath();

        // Only attempt for common strippables
        boolean looksStrippable = path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem")
                || path.endsWith("_hyphae") || path.equals("bamboo_block");
        if (!looksStrippable || path.startsWith("stripped_")) return null;

        Identifier strippedId = Identifier.of(id.getNamespace(), "stripped_" + path);
        Block stripped = Registries.BLOCK.get(strippedId);
        if (stripped == Blocks.AIR) return null;

        BlockState newState = stripped.getDefaultState();

        // Preserve orientation when possible
        if (state.contains(Properties.AXIS) && newState.contains(Properties.AXIS)) {
            newState = newState.with(Properties.AXIS, state.get(Properties.AXIS));
        } else if (state.contains(Properties.HORIZONTAL_FACING) && newState.contains(Properties.HORIZONTAL_FACING)) {
            newState = newState.with(Properties.HORIZONTAL_FACING, state.get(Properties.HORIZONTAL_FACING));
        }
        return newState;
    }

    /** Return DIRT_PATH for the vanilla flattenable set, otherwise null. Requires air-above check at call site. */
    private static BlockState flattenToPath(BlockState state) {
        if (state.isOf(Blocks.DIRT) || state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.COARSE_DIRT)
            || state.isOf(Blocks.ROOTED_DIRT) || state.isOf(Blocks.PODZOL) || state.isOf(Blocks.MYCELIUM)) {
            return Blocks.DIRT_PATH.getDefaultState(); // matches vanilla list
        }
        return null;
    }

    /** Hoe tilling behavior (single-click): coarse_dirt→dirt; dirt/grass/rooted_dirt/path→farmland. */
    private static BlockState till(BlockState state) {
        if (state.isOf(Blocks.COARSE_DIRT))   return Blocks.DIRT.getDefaultState();
        if (state.isOf(Blocks.DIRT) || state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.ROOTED_DIRT) || state.isOf(Blocks.DIRT_PATH)) {
            return Blocks.FARMLAND.getDefaultState();
        }
        return null;
    }

    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!(world instanceof ServerWorld serverWorld)) return true;

        final int height = mineHeight; // e.g., 2 for Stone, 2 for Diamond, 3 for Netherite
        final int width  = mineWidth;  // e.g., 1 for Stone, 3 for Diamond/Netherite

        // Determine lateral axis relative to player’s facing; we sweep left/right on this axis.
        final Direction facing = miner.getHorizontalFacing();
        final Direction right  = facing.rotateYClockwise(); // left = right.getOpposite()

        // Build vertical offsets according to your rule:
        //  - even heights include {0, -1, ...} (favoring below first)
        //  - odd heights include {+1, 0, -1, ...} (symmetric around the mined block)
        final int[] vOffs = verticalOffsets(height);

        // Lateral offsets are symmetric around 0: width=1 -> {0}, width=3 -> {-1,0,+1}
        final int[] hOffs = lateralOffsets(width);

        for (int dy : vOffs) {
            for (int dx : hOffs) {
                final BlockPos targetPos = pos.add(right.getOffsetX() * dx, dy, right.getOffsetZ() * dx);

                // Skip the center if you prefer vanilla to handle it; keep it if you want to re-handle drops.
                if (targetPos.equals(pos)) continue;

                final BlockState targetState = world.getBlockState(targetPos);

                // Skip air/fluids/unbreakable
                if (targetState.isAir() || !targetState.getFluidState().isEmpty()) continue;
                if (targetState.getHardness(serverWorld, targetPos) < 0.0F) continue;

                // Harvest requirement: compare your paxel’s mining level against block’s required level.
                final int required = requiredMiningLevel(targetState);
                if (this.miningLevel < required) continue;

                // Drop with correct loot context (respects Silk Touch/Fortune, gamerules, etc.)
                final BlockEntity be = serverWorld.getBlockEntity(targetPos);
                Block.dropStacks(targetState, serverWorld, targetPos, be, miner, stack);

                // Remove the block
                world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

                // Damage the tool once per extra block
                // stack.damage(1, miner, e -> e.sendToolBreakStatus(
                //     (miner.getMainHandStack() == stack) ? Hand.MAIN_HAND : Hand.OFF_HAND
                // ));
            }
        }

        return true;
    }

    /** Mining level required by block tags: 0=wood/gold, 1=stone, 2=iron, 3=diamond+. */
    private static int requiredMiningLevel(BlockState state) {
        if (state.isIn(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.isIn(BlockTags.NEEDS_IRON_TOOL))    return 2;
        if (state.isIn(BlockTags.NEEDS_STONE_TOOL))   return 1;
        return 0;
    }

    /** Lateral offsets around 0. Even widths bias to the player's right: 
     *  W=1 → {0}, W=2 → {0,+1}, W=3 → {0,+1,-1}, W=4 → {0,+1,-1,+2}, etc. */
    private static int[] lateralOffsets(int width) {
        if (width <= 1) return new int[]{0};
        int[] a = new int[width];
        int i = 0, step = 1;
        a[i++] = 0; // center column
        while (i < width) {
            a[i++] = step;           // right
            if (i < width) a[i++] = -step; // left
            step++;
        }
        return a;
    }

    /**
     * Vertical offsets around mined block (y=0).
     * Even heights: prefer current & below → {0, -1, -2, +1, -3, +2, ...}
     * Odd heights: symmetric → {+1, 0, -1, +2, -2, ...} so height=3 → {+1, 0, -1}
     */
    private static int[] verticalOffsets(int height) {
        if (height <= 1) return new int[]{0};
        int[] a = new int[height];
        if ((height & 1) == 0) {
            // even: start with 0 and -1 (mined & below), then expand outward
            int i = 0, down = -1, up = 1;
            a[i++] = 0; a[i++] = -1;
            while (i < height) {
                if (i < height) a[i++] = --down; // go further down
                if (i < height) a[i++] = up++;   // then up
            }
        } else {
            // odd: perfectly symmetric around 0
            int i = 0, up = 1, down = -1;
            a[i++] = +1; a[i++] = 0; a[i++] = -1;
            while (i < height) {
                if (i < height) a[i++] = ++up;   // further up
                if (i < height) a[i++] = --down; // further down
            }
        }
        return a;
    }

}
