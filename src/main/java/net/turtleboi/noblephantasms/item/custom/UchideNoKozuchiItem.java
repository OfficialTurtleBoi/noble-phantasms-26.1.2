package net.turtleboi.noblephantasms.item.custom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.world.ArtificialOreSavedData;

public class UchideNoKozuchiItem extends Item {
    private static final int MAX_GROWTH_STEPS = 16;

    public UchideNoKozuchiItem(Properties properties) {
        super(properties
                .axe(ToolMaterial.NETHERITE, 3.0F, -2.8F)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public float getDestroySpeed(ItemStack itemStack, BlockState state) {
        if (state.is(Blocks.BEDROCK)) {
            return 0.0F;
        }

        int tierScale = 1;
        if (state.is(BlockTags.INCORRECT_FOR_IRON_TOOL)) {
            tierScale = 2;
        }
        if (state.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)) {
            tierScale = 3;
        }
        if (state.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) {
            tierScale = 4;
        }
        return ToolMaterial.IRON.speed() / tierScale;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack itemStack, BlockState state) {
        return !state.is(Blocks.BEDROCK);
    }

    public static void handleBlockBreak(BreakBlockEvent event) {
        Player player = event.getPlayer();
        ItemStack mallet = player.getMainHandItem();
        if (!mallet.is(ModItems.UCHIDE_NO_KOZUCHI)) {
            return;
        }

        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        if (state.is(Blocks.BEDROCK)) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            return;
        }

        event.setCanceled(true);
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        event.setNotifyClient(true);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        ItemStack silkTool = mallet.copy();
        var silkTouch = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        silkTool.enchant(silkTouch, 1);
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, silkTool);

        if (!level.destroyBlock(pos, false, player)) {
            return;
        }
        ArtificialOreSavedData.get(level).clear(pos);
        level.levelEvent(2001, pos, Block.getId(state));
        for (ItemStack drop : drops) {
            Block.popResource(level, pos, drop);
        }
    }

    public static void handleRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack mallet = event.getEntity().getItemInHand(event.getHand());
        if (!mallet.is(ModItems.UCHIDE_NO_KOZUCHI)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        boolean growable = state.getBlock() instanceof BonemealableBlock grower
                && grower.getType() == BonemealableBlock.Type.GROWER;
        boolean ore = state.is(Tags.Blocks.ORES);
        if (!growable && !ore) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (growable) {
            BonemealableBlock grower = (BonemealableBlock) state.getBlock();
            if (growFully(level, pos, grower)) {
                level.levelEvent(1505, pos, 15);
            }
            return;
        }

        if (!ArtificialOreSavedData.get(level).isArtificial(pos)) {
            growOreVein(level, pos, state);
        }
    }

    private static int growOreVein(ServerLevel level, BlockPos pos, BlockState oreState) {
        ArtificialOreSavedData provenance = ArtificialOreSavedData.get(level);
        if (provenance.isArtificial(pos)) {
            return 0;
        }

        int targetGrowth = 2 + level.getRandom().nextInt(3) + level.getRandom().nextInt(3);
        TagKey<Block> hostTag = getOreHostTag(oreState);
        List<BlockPos> vein = new ArrayList<>();
        Set<BlockPos> veinPositions = new HashSet<>();
        vein.add(pos.immutable());
        veinPositions.add(pos.immutable());

        int placed = 0;
        while (placed < targetGrowth) {
            List<BlockPos> stoneCandidates = new ArrayList<>();
            List<BlockPos> airCandidates = new ArrayList<>();
            collectGrowthCandidates(level, vein, veinPositions, hostTag, stoneCandidates, airCandidates);
            List<BlockPos> candidates = !stoneCandidates.isEmpty() ? stoneCandidates : airCandidates;
            if (candidates.isEmpty()) {
                break;
            }

            BlockPos growthPos = candidates.get(level.getRandom().nextInt(candidates.size()));
            BlockState hostState = level.getBlockState(growthPos);
            level.setBlockAndUpdate(growthPos, oreState);
            provenance.markArtificial(growthPos);
            level.levelEvent(2001, growthPos, Block.getId(hostState));
            vein.add(growthPos);
            veinPositions.add(growthPos);
            placed++;
        }

        if (placed > 0) {
            provenance.markArtificial(pos);
        }
        return placed;
    }

    private static void collectGrowthCandidates(ServerLevel level, List<BlockPos> vein, Set<BlockPos> veinPositions,
                                                TagKey<Block> hostTag, List<BlockPos> stoneCandidates,
                                                List<BlockPos> airCandidates) {
        Set<BlockPos> seen = new HashSet<>(veinPositions);
        for (BlockPos veinPos : vein) {
            for (Direction direction : Direction.values()) {
                BlockPos candidate = veinPos.relative(direction).immutable();
                if (!seen.add(candidate)) {
                    continue;
                }

                BlockState candidateState = level.getBlockState(candidate);
                if (isValidOreHost(candidateState, hostTag)) {
                    stoneCandidates.add(candidate);
                } else if (candidateState.isAir()) {
                    airCandidates.add(candidate);
                }
            }
        }
    }

    private static TagKey<Block> getOreHostTag(BlockState oreState) {
        if (oreState.is(Tags.Blocks.ORES_IN_GROUND_STONE)) {
            return Tags.Blocks.ORE_BEARING_GROUND_STONE;
        }
        if (oreState.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)) {
            return Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE;
        }
        if (oreState.is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK)) {
            return Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK;
        }
        return null;
    }

    private static boolean isValidOreHost(BlockState state, TagKey<Block> hostTag) {
        if (hostTag != null) {
            return state.is(hostTag);
        }
        return state.is(Tags.Blocks.ORE_BEARING_GROUND_STONE)
                || state.is(Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE)
                || state.is(Tags.Blocks.ORE_BEARING_GROUND_NETHERRACK)
                || state.is(Tags.Blocks.STONES)
                || state.is(Tags.Blocks.END_STONES)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER);
    }

    private static boolean growFully(ServerLevel level, BlockPos pos, BonemealableBlock grower) {
        boolean grew = false;
        for (int i = 0; i < MAX_GROWTH_STEPS; i++) {
            BlockState before = level.getBlockState(pos);
            if (before.getBlock() != grower || !grower.isValidBonemealTarget(level, pos, before)) {
                break;
            }
            grower.performBonemeal(level, level.getRandom(), pos, before);
            BlockState after = level.getBlockState(pos);
            grew = true;
            if (after == before || after.getBlock() != grower) {
                break;
            }
        }
        return grew;
    }
}
