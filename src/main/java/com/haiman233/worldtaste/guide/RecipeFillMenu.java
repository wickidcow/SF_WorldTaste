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

/** Recipe-fill menu for WorldTaste machines. */
public final class RecipeFillMenu {
    private static final int PER_PAGE = 45;
    private static final int SLOT_PAGE = 53;
    private static final int SLOT_BACK = 45;
    private static final int FILL_GROUP_SIZE = 8;

    private RecipeFillMenu() {}

    public static void open(Player p, WTRecipeMachine machine, BlockMenu menu) {
        open(p, machine, menu, 0);
    }

    public static void open(Player p, WTRecipeMachine machine, BlockMenu menu, int page) {
        List<WTRecipe> recipes = machine.getRecipes();
        if (recipes.isEmpty()) {
            p.sendMessage(ChatColor.RED + "This machine has no recipe that can be filled.");
            return;
        }
        int pages = Math.max(1, (recipes.size() + PER_PAGE - 1) / PER_PAGE);
        int pg = Math.max(0, Math.min(page, pages - 1));
        String title = ChatColor.stripColor(machine.getItemName()) + " · Select Recipe"
                + (pages > 1 ? " " + (pg + 1) + "/" + pages : "");
        ChestMenu gui = new ChestMenu(title);
        gui.setEmptySlotsClickable(false);
        for (int i = 0; i < 54; i++) gui.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());

        int start = pg * PER_PAGE;
        int shown = Math.min(PER_PAGE, recipes.size() - start);
        for (int i = 0; i < shown; i++) {
            final int index = start + i;
            final WTRecipe recipe = recipes.get(index);
            gui.addItem(i, describeRecipe(recipe, index), (pl, s, cursor, action) -> {
                if (action.isRightClicked()) fillN(pl, machine, recipe, menu);
                else fill(pl, machine, recipe, menu);
                return false;
            });
        }

        gui.addItem(SLOT_BACK, backMachineItem(), (pl, s, cursor, action) -> {
            BlockMenu inv = resolveInv(pl, machine, menu);
            if (inv != null) inv.open(pl);
            return false;
        });

        if (pages > 1) {
            ItemStack pageBtn = pageItem("Recipe " + (pg + 1) + "/" + pages);
            ItemMeta pm = pageBtn.getItemMeta();
            if (pm != null) {
                List<String> lore = pm.getLore() != null ? new ArrayList<>(pm.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Left-click: Next Page");
                lore.add(ChatColor.GRAY + "Right-click: Previous Page");
                pm.setLore(lore);
                pageBtn.setItemMeta(pm);
            }
            gui.addItem(SLOT_PAGE, pageBtn, (pl, s, cursor, action) -> {
                if (action.isRightClicked()) open(pl, machine, menu, pg - 1);
                else open(pl, machine, menu, pg + 1);
                return false;
            });
        }
        gui.open(p);
    }

    private static void fill(Player p, WTRecipeMachine machine, WTRecipe r, BlockMenu provided) {
        int missing = fillOne(p, machine, r, provided);
        p.sendMessage(missing == 0 ? ChatColor.GREEN + "Filled one recipe's ingredients."
                : ChatColor.YELLOW + "Fill completed, but " + missing + " ingredient type(s) were missing.");
    }

    private static void fillN(Player p, WTRecipeMachine machine, WTRecipe r, BlockMenu provided) {
        int missing = fillCore(p, machine, r, provided, FILL_GROUP_SIZE);
        p.sendMessage(missing == 0 ? ChatColor.GREEN + "Filled ingredients for " + FILL_GROUP_SIZE + " recipe batches."
                : ChatColor.YELLOW + "Fill completed, but " + missing + " ingredient type(s) were missing.");
    }

    private static BlockMenu resolveInv(Player p, WTRecipeMachine machine, BlockMenu provided) {
        BlockMenu inv = provided;
        if (inv == null) {
            Block target = p.getTargetBlockExact(5);
            if (target != null) inv = BlockStorage.getInventory(target.getLocation());
        }
        if (inv == null || inv.getPreset() == null || inv.getPreset().getSlimefunItem() != machine) {
            p.sendMessage(ChatColor.RED + "Recipe fill failed: open " + ChatColor.stripColor(machine.getItemName()) + " and try again.");
            return null;
        }
        return inv;
    }

    private static int fillOne(Player p, WTRecipeMachine machine, WTRecipe r, BlockMenu provided) {
        return fillCore(p, machine, r, provided, 1);
    }

    private static int fillCore(Player p, WTRecipeMachine machine, WTRecipe r, BlockMenu provided, int portions) {
        BlockMenu inv = resolveInv(p, machine, provided);
        if (inv == null) return 0;
        int[] layoutSlots = machine.getInputSlots().clone();
        java.util.Arrays.sort(layoutSlots);
        boolean[] usedInput = new boolean[layoutSlots.length];
        int missing = 0;
        ItemStack[] input = r.getInput();
        for (int i = 0; i < input.length; i++) {
            ItemStack need = input[i];
            if (need == null) continue;
            int slot = r.inSlot(i);
            if (slot < 0) slot = findStackOrEmpty(inv, need, layoutSlots, usedInput);
            if (slot < 0) {
                p.sendMessage(ChatColor.RED + "Recipe fill failed: there are no free input slots.");
                return missing;
            }
            ItemStack cur = inv.getItemInSlot(slot);
            if (cur != null && !cur.getType().isAir() && !SlimefunUtils.isItemSimilar(cur, need, true)) {
                p.sendMessage(ChatColor.YELLOW + "Slot " + (slot + 1) + " is occupied by another item; skipping that ingredient.");
                continue;
            }
            int have = (cur == null || cur.getType().isAir()) ? 0 : cur.getAmount();
            int capacity = (cur == null || cur.getType().isAir()) ? need.getMaxStackSize() : cur.getMaxStackSize();
            int target = have + need.getAmount() * portions;
            if (have >= target) continue;
            int take = Math.min(target - have, capacity - have);
            if (take <= 0) continue;
            ItemStack collected = takeFromPlayer(p, need, take);
            if (collected == null) {
                missing++;
                p.sendMessage(ChatColor.RED + "Inventory is missing " + displayName(need) + " ×" + take);
                continue;
            }
            if (have == 0) inv.replaceExistingItem(slot, collected);
            else {
                ItemStack merged = cur.clone();
                merged.setAmount(have + collected.getAmount());
                inv.replaceExistingItem(slot, merged);
            }
        }
        return missing;
    }

    private static int findStackOrEmpty(BlockMenu inv, ItemStack need, int[] layoutSlots, boolean[] usedInput) {
        for (int k = 0; k < layoutSlots.length; k++) {
            if (usedInput[k]) continue;
            ItemStack cur = inv.getItemInSlot(layoutSlots[k]);
            if (cur != null && !cur.getType().isAir() && SlimefunUtils.isItemSimilar(cur, need, true)
                    && cur.getAmount() < cur.getMaxStackSize()) {
                usedInput[k] = true;
                return layoutSlots[k];
            }
        }
        for (int k = 0; k < layoutSlots.length; k++) {
            if (usedInput[k]) continue;
            ItemStack cur = inv.getItemInSlot(layoutSlots[k]);
            if (cur == null || cur.getType().isAir()) {
                usedInput[k] = true;
                return layoutSlots[k];
            }
        }
        return -1;
    }

    private static ItemStack takeFromPlayer(Player p, ItemStack need, int amount) {
        PlayerInventory inv = p.getInventory();
        ItemStack result = null;
        int left = amount;
        for (int k = 0; k < inv.getSize(); k++) {
            ItemStack it = inv.getItem(k);
            if (it == null || it.getType().isAir() || !SlimefunUtils.isItemSimilar(it, need, true)) continue;
            int take = Math.min(left, it.getAmount());
            if (take <= 0) continue;
            if (result == null) {
                result = it.clone();
                result.setAmount(take);
            } else result.setAmount(result.getAmount() + take);
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
            lore.add(ChatColor.GREEN + "Recipe " + (index + 1));
            lore.add(ChatColor.GRAY + "Ingredients: " + countInputs(r) + " · Time: " + (r.getTicks() / 2) + "s");
            lore.add(ChatColor.DARK_GRAY + "Left-click: Fill one batch (click again to add more)");
            lore.add(ChatColor.DARK_GRAY + "Right-click: Fill " + FILL_GROUP_SIZE + " batches (click again to add more)");
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static int countInputs(WTRecipe r) {
        int n = 0;
        for (ItemStack in : r.getInput()) if (in != null) n++;
        return n;
    }

    private static String displayName(ItemStack stack) {
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) return ChatColor.stripColor(stack.getItemMeta().getDisplayName());
        return stack.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    private static ItemStack pageItem(String name) {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack backMachineItem() {
        ItemStack it = new ItemStack(Material.OAK_DOOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Back to Machine");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Close recipe selection and return to the machine");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }
}
