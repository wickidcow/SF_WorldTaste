package com.haiman233.worldtaste.items;

import com.haiman233.worldtaste.behavior.Behaviors.CropCfg;
import com.haiman233.worldtaste.behavior.Behaviors.CropDrop;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;

/**
 * 作物方块（machines.yml 中 script 为 seed/* 的物品）。对齐原脚本 WT_setupCrop：
 * 按 growMs 与生长阶段推进 Ageable 年龄；成熟后破坏掉落作物/种子。
 */
public class CropBlock extends SlimefunItem {

    /** 与 wt_crop.js 一致的小生长阶段。 */
    private static final double[] SMALL_STEPS = {1/10d, 1/6d, 1/3d, 0.5, 2/3d, 5/6d, 1d, 7/6d};
    /** 生长进度持久化键（BlockStorage，随 Slimefun 数据库落盘）：生长起点时间戳 / 成熟标记。 */
    private static final String KEY_START = "wt-crop-start";
    private static final String KEY_GROWN = "wt-crop-grown";

    private final CropCfg cfg;
    /** 预算的生长阈值 growMs*SMALL_STEPS[i]（不变量，double 保精确语义）。构造期一次计算，避免每 tick 每生长作物重复 8 次乘法。 */
    private final double[] growMsSteps;
    private final Map<Location, Long> lastUse = new HashMap<>();
    private final Set<Location> grown = new HashSet<>();
    /** 方块当前阶段缓存：仅阶段变化时才写方块，避免每 tick getState()/setBlockData() 的对象开销（spark 热点优化）。 */
    private final Map<Location, Integer> stage = new HashMap<>();
    /** CHORUS 上方清理节拍：每 2 tick 一次（原版随机刻频率远低于此，节流不影响拦截效果）。 */
    private int chorusCheck;
    /** 外部催熟检测节拍：每 8 tick 一次（骨粉等由 BlockFertilizeEvent 即时标记，此兜底仅防其他手段）。 */
    private int matureCheck;

    public CropBlock(ItemGroup group, SlimefunItemStack item, RecipeType rt, ItemStack[] recipe, CropCfg cfg) {
        super(group, item, rt, recipe);
        this.cfg = cfg;
        this.growMsSteps = new double[SMALL_STEPS.length];
        for (int i = 0; i < SMALL_STEPS.length; i++) growMsSteps[i] = cfg.growMs * SMALL_STEPS[i];
    }

    /** 种植要求：显式 plantOn 优先，否则按材质推断（原版机制）；null 表示不限制。 */
    public List<Material> getPlantOn() {
        return cfg.resolvedPlantOn();
    }

    /** 不由 Slimefun 框架掉落种子本身（仅由 CropListener 在成熟时掉落作物/种子，对齐 wt_crop.js）。 */
    @Override
    public List<ItemStack> getDrops() {
        return java.util.Collections.emptyList();
    }

    @Override
    public void preRegister() {
        super.preRegister();
        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() { return true; }
            @Override
            public void tick(Block b, SlimefunItem item, Config data) {
                CropBlock.this.tick(b);
            }
        });
    }

    private void tick(Block b) {
        Location l = b.getLocation();
        Material type = b.getType();
        boolean isSeedHead = (type == Material.PLAYER_HEAD || type == Material.PLAYER_WALL_HEAD);
        if (type != cfg.material && !isSeedHead) {
            // 仍登记为我们的作物（玩家未破坏）时，可能是原版机制替换了方块
            // （如紫颂随机生长/甘蔗物理变化）：恢复作物材质继续生长，避免误注销；
            // 否则视为被移除（耕地破坏、爆炸、踩踏等），清理状态并注销，
            // 避免幽灵 tick 把 AIR 设回作物刷原版种子。
            if (me.mrCookieSlime.Slimefun.api.BlockStorage.hasBlockInfo(b)) {
                b.setType(cfg.material);
            } else {
                lastUse.remove(l);
                grown.remove(l);
                stage.remove(l);
                me.mrCookieSlime.Slimefun.api.BlockStorage.clearBlockInfo(b);
                return;
            }
        }
        // 阻止原版紫颂类随机生长：CHORUS_FLOWER 种在末地石上会被原版随机刻
        // 在上方长出 CHORUS_PLANT（紫颂树），替换/延伸作物并破坏 Slimefun 收获流程；
        // 节流后（每 2 tick）仍远快于原版随机刻频率，作物生长完全由本插件控制。
        if (cfg.material == Material.CHORUS_FLOWER && (++chorusCheck & 1) == 0) {
            Block above = b.getRelative(BlockFace.UP);
            Material upType = above.getType();
            if (upType == Material.CHORUS_PLANT || upType == Material.CHORUS_FLOWER) {
                above.setType(Material.AIR);
            }
        }
        if (isSeedHead) {
            // 新放置/重放的种子：清除可能残留的成熟标记（防同位置秒熟），重新开始生长
            grown.remove(l);
            lastUse.put(l, System.currentTimeMillis());
            stage.remove(l);
            // 重种：清除持久化生长数据，重新开始
            me.mrCookieSlime.Slimefun.api.BlockStorage.addBlockInfo(b, KEY_START, null);
            me.mrCookieSlime.Slimefun.api.BlockStorage.addBlockInfo(b, KEY_GROWN, null);
            setStage(b, 0);
            return;
        }
        if (grown.contains(l)) return;
        long now = System.currentTimeMillis();
        Long last = lastUse.get(l);
        if (last == null) {
            // 内存状态缺失（重启/新放置）：从持久化恢复；成熟标记优先，避免重新计时
            if (me.mrCookieSlime.Slimefun.api.BlockStorage.getLocationInfo(l, KEY_GROWN) != null) {
                grown.add(l);
                return;
            }
            String saved = me.mrCookieSlime.Slimefun.api.BlockStorage.getLocationInfo(l, KEY_START);
            if (saved != null) {
                try { last = Long.parseLong(saved); } catch (NumberFormatException ignored) { last = null; }
            }
            if (last == null) {
                lastUse.put(l, now);
                me.mrCookieSlime.Slimefun.api.BlockStorage.addBlockInfo(b, KEY_START, String.valueOf(now));
                stage.remove(l);
                setStage(b, 0);
                return;
            }
            lastUse.put(l, last);
        }
        // 外部催熟兜底（节流每 8 tick；骨粉等常规路径由 BlockFertilizeEvent 即时标记，
        // 此处仅防命令/其他插件直接改 age）：原版 age 已达最大则补记成熟并持久化
        if ((++matureCheck & 7) == 0 && isNaturallyMature(b)) {
            grown.add(l);
            me.mrCookieSlime.Slimefun.api.BlockStorage.addBlockInfo(b, KEY_GROWN, "1");
            return;
        }
        long elapsed = now - last;
        int target = cfg.maxAge;
        for (int i = 0; i < growMsSteps.length; i++) {
            if (elapsed < growMsSteps[i]) {
                target = (i > 0) ? (int) Math.floor(cfg.maxAge * ((double) i / SMALL_STEPS.length)) : -1;
                break;
            }
        }
        if (target >= 0) {
            setStageIfChanged(l, b, target);
            if (target == cfg.maxAge) {
                grown.add(l);
                me.mrCookieSlime.Slimefun.api.BlockStorage.addBlockInfo(b, KEY_GROWN, "1");
            }
        }
    }

    private void setStage(Block b, int age) {
        if (b.getType() != cfg.material) b.setType(cfg.material);
        BlockState st = b.getState();
        if (st.getBlockData() instanceof Ageable a) {
            int target = Math.min(age, a.getMaximumAge());
            if (a.getAge() != target) {
                a.setAge(target);
                st.setBlockData(a);
                st.update(true);
            }
        }
    }

    /** 外部催熟（骨粉等）即时标记：原版 age 已满则补记成熟标记并持久化（BlockFertilizeEvent 调用）。 */
    public void markExternalMature(Block b) {
        if (isNaturallyMature(b)) {
            grown.add(b.getLocation());
            me.mrCookieSlime.Slimefun.api.BlockStorage.addBlockInfo(b, KEY_GROWN, "1");
        }
    }

    /** 方块原版 age 是否已达最大（骨粉等外部催熟路径：grown 未标记但实际已成熟）。 */
    private boolean isNaturallyMature(Block b) {
        if (b.getType() != cfg.material) return false;
        BlockState st = b.getState();
        if (st.getBlockData() instanceof Ageable a) {
            return a.getAge() >= a.getMaximumAge();
        }
        return false;
    }

    /** 阶段缓存写入：目标年龄与缓存一致时跳过方块写操作（spark 热点优化，行为不变）。 */
    private void setStageIfChanged(Location l, Block b, int age) {
        Integer cur = stage.get(l);
        if (cur != null && cur.intValue() == age) return;
        setStage(b, age);
        stage.put(l, age);
    }

    /** 破坏时调用：若已成熟则掉落作物/种子。返回是否处理过。 */
    public boolean onBreak(Block b) {
        Location l = b.getLocation();
        boolean wasGrown = grown.remove(l);
        lastUse.remove(l);
        stage.remove(l);
        java.util.Random rnd = new java.util.Random();
        if (!wasGrown) {
            // 骨粉等外部催熟使原版 age 已达最大：视为成熟，掉成品
            if (isNaturallyMature(b)) {
                wasGrown = true;
            } else {
                // 未成熟破坏：必掉 1 个对应种子（cropId 缺失时从掉落表回退找含 SEED 的项）
                dropSeed(b, 1, 1.0, rnd);
                return false;
            }
        }
        List<CropDrop> drops = cfg.drops;
        if (cfg.weighted && !drops.isEmpty()) {
            // 对齐 FishingListener.select 的健壮加权选择：
            //   · total<=0（权重全非正的脏数据）时不产出，避免 rnd.nextDouble()*total 为负后逻辑错乱；
            //   · 兜底选末项，保证 total>0 时浮点边界/末项权重为 0 仍至少产出一个掉落（原实现循环走完会什么都不掉）。
            // R8：total 改用 CropCfg.weightTotal（load 期预算），消除每次收获对 drops 的求和（O(n)→O(1)，对齐 R4）。
            double total = cfg.weightTotal;
            if (total <= 0) return true;
            double r = rnd.nextDouble() * total;
            CropDrop picked = drops.get(drops.size() - 1);
            for (CropDrop d : drops) {
                r -= d.weight;
                if (r <= 0) { picked = d; break; }
            }
            dropItem(b, picked.id);
        } else {
            for (CropDrop d : drops) {
                if (rnd.nextDouble() < d.chance) dropItem(b, d.id);
            }
        }
        // 成熟破坏：原有掉落不变，额外按 seedDropChance 概率掉 1..seedDropMax 个种子
        dropSeed(b, 1 + rnd.nextInt(Math.max(1, cfg.seedDropMax)), cfg.seedDropChance, rnd);
        return true;
    }

    private void dropItem(Block b, String id) {
        SlimefunItem sf = SlimefunItem.getById(id);
        ItemStack stack;
        if (sf != null) stack = sf.getItem();
        else {
            Material m = Material.matchMaterial(id);
            if (m == null) {
                com.haiman233.worldtaste.WT.log("作物 " + getId() + " 的掉落物无法解析: " + id);
                return;
            }
            stack = new ItemStack(m);
        }
        b.getWorld().dropItemNaturally(b.getLocation(), stack.clone());
    }

    /** 掉落作物种子：cropId 优先，缺失时从掉落表回退找含 SEED 的项；chance&lt;1 时按概率判定。 */
    private void dropSeed(Block b, int count, double chance, java.util.Random rnd) {
        if (chance < 1.0 && rnd.nextDouble() >= chance) return;
        String seedId = cfg.cropId;
        if (seedId == null) {
            for (CropDrop d : cfg.drops) {
                if (d.id != null && d.id.contains("SEED")) { seedId = d.id; break; }
            }
        }
        if (seedId == null) return;
        SlimefunItem sf = SlimefunItem.getById(seedId);
        ItemStack stack = (sf != null) ? sf.getItem().clone() : null;
        if (stack == null) {
            Material m = Material.matchMaterial(seedId);
            if (m == null) return;
            stack = new ItemStack(m);
        }
        stack.setAmount(count);
        b.getWorld().dropItemNaturally(b.getLocation(), stack);
    }
}
