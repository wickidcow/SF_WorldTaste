package com.haiman233.worldtaste.guide;

import com.haiman233.worldtaste.machines.WTRecipe;
import com.haiman233.worldtaste.machines.WTRecipeMachine;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.ArrayList;
import java.util.List;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 机器配方补全：列出机器全部工作配方（产物代表），玩家选择后自动从背包
 * 将材料填入机器 GUI 的对应输入槽（含大型配方的自定义绑定槽）。
 * <p>替代 JEG 配方补全——JEG 补全只能填「合成配方」材料，对机器工作配方
 * （尤其终焉厨锅这类大型绑定槽配方）无效。</p>
 */
public final class RecipeFillMenu {

    private RecipeFillMenu() {}

    /** 打开配方选择菜单。 */
    public static void open(Player p, WTRecipeMachine machine) {
        List<WTRecipe> recipes = machine.getRecipes();
        if (recipes.isEmpty()) {
            p.sendMessage(ChatColor.RED + "该机器没有可补全的配方");
            return;
        }
        ChestMenu menu = new ChestMenu(ChatColor.stripColor(machine.getItemName()) + " · 选择要补全的配方");
        menu.setEmptySlotsClickable(false);
        for (int i = 0; i < 54; i++) {
            menu.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
        int shown = Math.min(recipes.size(), 45);
        for (int i = 0; i < shown; i++) {
            final int index = i;
            menu.addItem(i, describeRecipe(recipes.get(i), i), (pl, s, cursor, action) -> {
                fill(pl, machine, recipes.get(index));
                return false;
            });
        }
        if (recipes.size() > shown) {
            menu.addItem(45, moreItem(recipes.size() - shown), ChestMenuUtils.getEmptyClickHandler());
        }
        menu.open(p);
    }

    /** 从玩家背包把所选配方的材料填入机器输入槽（玩家须站在机器旁）。 */
    private static void fill(Player p, WTRecipeMachine machine, WTRecipe r) {
        Block target = p.getTargetBlockExact(5);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "补全失败：请站在机器旁");
            return;
        }
        BlockMenu inv = BlockStorage.getInventory(target.getLocation());
        if (inv == null || inv.getPreset() == null || inv.getPreset().getSlimefunItem() != machine) {
            p.sendMessage(ChatColor.RED + "补全失败：请看着该机器（" + ChatColor.stripColor(machine.getItemName()) + "）");
            return;
        }
        int[] inputSlots = machine.getInputSlots();
        boolean[] usedInput = new boolean[inputSlots.length];
        int missing = 0;
        ItemStack[] input = r.getInput();
        for (int i = 0; i < input.length; i++) {
            ItemStack need = input[i];
            if (need == null) continue;
            int slot = r.inSlot(i);
            if (slot < 0) {
                slot = -1;
                for (int k = 0; k < inputSlots.length; k++) {
                    if (usedInput[k]) continue;
                    ItemStack cur = inv.getItemInSlot(inputSlots[k]);
                    if (cur == null || cur.getType().isAir()) {
                        slot = inputSlots[k];
                        usedInput[k] = true;
                        break;
                    }
                }
            }
            if (slot < 0) {
                p.sendMessage(ChatColor.RED + "补全失败：没有空余输入槽");
                return;
            }
            ItemStack cur = inv.getItemInSlot(slot);
            if (cur != null && !cur.getType().isAir() && !SlimefunUtils.isItemSimilar(cur, need, true)) {
                p.sendMessage(ChatColor.YELLOW + "槽位 " + (slot + 1) + " 被其他物品占用，跳过该材料");
                continue;
            }
            int have = (cur == null || cur.getType().isAir()) ? 0 : cur.getAmount();
            int take = need.getAmount() - have;
            if (take <= 0) continue;
            ItemStack collected = takeFromPlayer(p, need, take);
            if (collected == null) {
                missing++;
                p.sendMessage(ChatColor.RED + "背包缺少 " + displayName(need) + " ×" + take);
                continue;
            }
            if (have == 0) {
                inv.replaceExistingItem(slot, collected);
            } else {
                ItemStack merged = cur.clone();
                merged.setAmount(have + collected.getAmount());
                inv.replaceExistingItem(slot, merged);
            }
        }
        p.sendMessage(missing == 0
                ? ChatColor.GREEN + "材料已填充完成"
                : ChatColor.YELLOW + "填充完成，但缺少 " + missing + " 种材料");
    }

    /** 从玩家背包收集 amount 个匹配 need 的物品（不足返回 null，已取的不回滚）。 */
    private static ItemStack takeFromPlayer(Player p, ItemStack need, int amount) {
        PlayerInventory inv = p.getInventory();
        ItemStack result = null;
        int left = amount;
        for (int k = 0; k < inv.getSize(); k++) {
            ItemStack it = inv.getItem(k);
            if (it == null || it.getType().isAir()) continue;
            if (!SlimefunUtils.isItemSimilar(it, need, true)) continue;
            int take = Math.min(left, it.getAmount());
            if (take <= 0) continue;
            if (result == null) {
                result = it.clone();
                result.setAmount(take);
            } else {
                result.setAmount(result.getAmount() + take);
            }
            int rest = it.getAmount() - take;
            if (rest <= 0) inv.setItem(k, null);
            else {
                ItemStack reduced = it.clone();
                reduced.setAmount(rest);
                inv.setItem(k, reduced);
            }
            left -= take;
            if (left <= 0) break;
        }
        return left <= 0 ? result : null;
    }

    private static ItemStack describeRecipe(WTRecipe r, int index) {
        ItemStack[] out = r.getOutput();
        ItemStack icon = (out.length > 0 && out[0] != null) ? out[0].clone() : new ItemStack(Material.BARRIER);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GREEN + "配方 " + (index + 1));
            lore.add(ChatColor.GRAY + "材料 " + countInputs(r) + " 项 · 耗时 " + (r.getTicks() / 2) + "s");
            lore.add(ChatColor.DARK_GRAY + "点击自动填充材料（请站在机器旁）");
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static int countInputs(WTRecipe r) {
        int n = 0;
        for (ItemStack in : r.getInput()) {
            if (in != null) n++;
        }
        return n;
    }

    private static String displayName(ItemStack stack) {
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(stack.getItemMeta().getDisplayName());
        }
        return stack.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    private static ItemStack moreItem(int extra) {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "还有 " + extra + " 个配方");
            it.setItemMeta(meta);
        }
        return it;
    }
}