package com.haiman233.worldtaste.behavior;

import com.haiman233.worldtaste.items.ConsumableItem;
import com.haiman233.worldtaste.items.CropBlock;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import java.util.List;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 种植与放置保护（统一走 {@link BlockPlaceEvent}，不依赖 Slimefun 内部放置流程）：
 * <ol>
 *   <li>所有作物种子（{@link CropBlock}）放置校验：显式 plantOn 或按材质推断的种植要求
 *       （耕地/灵魂沙/丛林原木/末地石等原版机制），不满足则取消放置并提示。
 *       悬挂类作物（COCOA）按附着面（Directional 反方向）校验。</li>
 *   <li>食物（{@link ConsumableItem}）：普通右键=食用（此时取消放置，避免"吃+放"双消耗），
 *       潜行右键=放置——放置时登记进 {@link BlockStorage}，挖掘时 Slimefun 掉落带粘液数据的物品
 *       （持久化于 Slimefun 数据库，爆炸/活塞/水流等破坏路径也由 Slimefun 统一处理）。</li>
 *   <li>其他 NotPlaceable 装饰：直接允许放置并登记（不限制潜行）。</li>
 * </ol>
 */
public final class PlantGuardListener implements Listener {

    public static final PlantGuardListener INSTANCE = new PlantGuardListener();

    private PlantGuardListener() {}

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item == null || item.getType().isAir()) {
            // 幽灵放置：手持物品已被消费（如手持 1 个食物食用后主手清空），客户端仍声明放置——
            // Bukkit 会按客户端声明放置一个无 NBT 的原版头颅；正常放置必然手持物品，直接取消
            e.setCancelled(true);
            return;
        }
        SlimefunItem sf = SlimefunItem.getByItem(item);
        if (sf == null) {
            // 兜底：无 id 展示物品（如未注册物品的机器/多方块产物）按外观反查注册物品
            sf = com.haiman233.worldtaste.util.Stacks.findRegisteredByAppearance(item);
        }
        if (sf == null) return;

        // 作物种子：种植要求校验（全部种子类生效）
        if (sf instanceof CropBlock crop) {
            List<Material> allowed = crop.getPlantOn();
            if (allowed != null && !allowed.isEmpty()) {
                Block block = e.getBlock();
                boolean ok = allowed.contains(block.getRelative(BlockFace.DOWN).getType());
                // 悬挂类作物（如 COCOA）：按附着面校验
                if (!ok && block.getBlockData() instanceof Directional dir) {
                    Block attached = block.getRelative(dir.getFacing().getOppositeFace());
                    ok = allowed.contains(attached.getType());
                }
                if (!ok) {
                    e.setCancelled(true);
                    Player p = e.getPlayer();
                    if (p != null) p.sendMessage("This crop can only be planted on " + names(allowed) + "!");
                }
            }
            return;
        }

        // 不可放置类物品
        if (sf instanceof NotPlaceable) {
            // 食物（头颅材质可放置方块）：普通右键（非潜行）已被食用逻辑消费，
            // 取消放置避免"吃+放"双消耗与头颅残留；潜行右键=明确放置意图，放行并登记
            // Slimefun 方块（挖掘保留粘液数据）
            if (sf instanceof ConsumableItem) {
                if (!e.getPlayer().isSneaking()) {
                    e.setCancelled(true);
                    return;
                }
                BlockStorage.store(e.getBlock(), sf.getId());
                return;
            }
            // 其他不可放置装饰：允许放置并登记
            BlockStorage.store(e.getBlock(), sf.getId());
        }
    }

    private static String names(List<Material> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(list.get(i).name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
        }
        return sb.toString();
    }
}