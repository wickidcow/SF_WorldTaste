package com.haiman233.worldtaste.behavior;

import com.haiman233.worldtaste.items.CropBlock;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * 作物方块破坏处理：
 * <ol>
 *   <li>破坏作物主格：禁用原版掉落，成熟则掉成品/种子（未成熟掉 1 种子）；2 格高作物（如瓶子草）
 *       同时清除上方残留格；</li>
 *   <li>破坏 2 格高作物的上格：联动破坏主格（掉成品/种子）并清数据；</li>
 *   <li>破坏种子附着的支撑方块（耕地/末地石等）：联动破坏上方作物；</li>
 *   <li>非玩家事件（爆炸、耕地踩踏）：作物被波及/失去支撑时同样掉成品并删除粘液数据，避免残留。</li>
 * </ol>
 */
public final class CropListener implements Listener {

    public static final CropListener INSTANCE = new CropListener();

    private CropListener() {}

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        SlimefunItem sf = BlockStorage.check(b);
        if (sf instanceof CropBlock crop) {
            // 禁用原版掉落：作物方块已被 tick 转成 WHEAT 等原版材质，否则会额外掉原版作物/种子
            e.setDropItems(false);
            breakCrop(b, crop);
            return;
        }
        // 2 格高作物：破坏上格 → 联动破坏主格
        Block below = b.getRelative(BlockFace.DOWN);
        SlimefunItem belowSf = BlockStorage.check(below);
        if (belowSf instanceof CropBlock cropBelow) {
            e.setDropItems(false);
            breakCrop(below, cropBelow);
            return;
        }
        // 支撑方块被破坏 → 联动破坏上方作物（保留支撑方块自身的原版掉落）
        Block above = b.getRelative(BlockFace.UP);
        SlimefunItem aboveSf = BlockStorage.check(above);
        if (aboveSf instanceof CropBlock cropAbove) {
            breakCrop(above, cropAbove);
        }
    }

    /** 爆炸波及：被炸方块的上/下若有作物（支撑被炸 / 2 格高上格被炸），联动破坏并清数据。 */
    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        for (Block b : e.blockList()) {
            cleanupAdjacent(b);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block b : e.blockList()) {
            cleanupAdjacent(b);
        }
    }

    /** 耕地被踩踏成泥土：上方作物失去支撑，联动破坏并清数据。 */

    /** 骨粉催熟作物：原版 age 达最大时即时补记成熟标记（避免依赖 tick 轮询，提升响应与性能）。 */
    @EventHandler(ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent e) {
        for (org.bukkit.block.BlockState bs : e.getBlocks()) {
            SlimefunItem sf = BlockStorage.check(bs.getBlock());
            if (sf instanceof CropBlock crop) {
                crop.markExternalMature(bs.getBlock());
            }
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (e.getBlock().getType() != Material.FARMLAND) return;
        Block above = e.getBlock().getRelative(BlockFace.UP);
        SlimefunItem sf = BlockStorage.check(above);
        if (sf instanceof CropBlock crop) {
            breakCrop(above, crop);
        }
    }

    /** 破坏作物主格：掉成品/种子、清数据、清除 2 格高作物的上格残留。 */
    private static void breakCrop(Block b, CropBlock crop) {
        // 2 格高作物（如 PITCHER_CROP）：主格破坏时上格一并清除，避免悬空残留
        Block up = b.getRelative(BlockFace.UP);
        if (up.getType() == b.getType()) {
            up.setType(Material.AIR);
        }
        crop.onBreak(b);
        b.setType(Material.AIR);
        BlockStorage.clearBlockInfo(b);
    }

    private static void cleanupAdjacent(Block exploded) {
        cleanupCrop(exploded.getRelative(BlockFace.UP));    // 支撑被炸
        cleanupCrop(exploded.getRelative(BlockFace.DOWN));  // 2 格高上格被炸
    }

    private static void cleanupCrop(Block b) {
        SlimefunItem sf = BlockStorage.check(b);
        if (sf instanceof CropBlock crop) {
            breakCrop(b, crop);
        }
    }
}
