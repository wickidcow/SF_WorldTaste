package com.haiman233.worldtaste.guide;

import com.haiman233.worldtaste.jeg.JegHook;
import com.haiman233.worldtaste.machines.WTRecipe;
import com.haiman233.worldtaste.machines.WTRecipeMachine;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.ArrayList;
import java.util.List;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Large-recipe display menu used for machines with more than a normal 3x3 input. */
public final class BigRecipeMenu {

    private static final int SLOT_ICON = 8;
    private static final int SLOT_OUTPUT = 24;
    private static final int SLOT_BACK = 35;
    private static final int SLOT_PAGE = 53;

    private BigRecipeMenu() {}

    public static boolean isLargeRecipeMachine(WTRecipeMachine machine) {
        for (WTRecipe r : machine.getRecipes()) {
            int n = 0;
            for (ItemStack in : r.getInput()) if (in != null) n++;
            if (n > 9) return true;
        }
        return false;
    }

    public static void open(Player p, WTRecipeMachine machine, int index, Runnable backOpener) {
        List<WTRecipe> recipes = machine.getRecipes();
        int workCount = recipes.size();
        int total = workCount + 1;
        int idx = ((index % total) + total) % total;
        if (idx == 0) openCraftPage(p, machine, workCount, backOpener);
        else openWorkPage(p, machine, recipes.get(idx - 1), idx - 1, workCount, backOpener);
    }

    private static void openCraftPage(Player p, WTRecipeMachine machine, int workCount, Runnable backOpener) {
        String title = ChatColor.stripColor(machine.getItemName()) + " · Crafting Recipe";
        ChestMenu menu = new ChestMenu(title);
        menu.setEmptySlotsClickable(false);
        for (int i = 0; i < 54; i++) menu.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());

        int[] craftSlots = {3, 4, 5, 12, 13, 14, 21, 22, 23};
        ItemStack[] craft = machine.getRecipe();
        for (int i = 0; i < 9 && i < craft.length; i++) {
            if (craft[i] == null) continue;
            ItemStack display = describeCraftInput(craft[i], i);
            menu.addItem(craftSlots[i], display, (pl, s, cursor, action) -> {
                SlimefunItem sf = SlimefunItem.getByItem(display);
                if (sf instanceof WTRecipeMachine sub) {
                    BigRecipeMenu.open(pl, sub, 0, () -> BigRecipeMenu.open(pl, machine, 0, backOpener));
                }
                return false;
            });
        }
        ItemStack rtIcon = machine.getRecipeType().toItem();
        menu.addItem(10, rtIcon != null ? rtIcon : ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        menu.addItem(16, describeMachine(machine, null), ChestMenuUtils.getEmptyClickHandler());
        menu.addItem(SLOT_ICON,
                infoItem("Crafting Recipe", "Use " + recipeTypeName(machine) + " to craft this machine"),
                ChestMenuUtils.getEmptyClickHandler());
        menu.addItem(SLOT_BACK, backItem(backOpener == null), (pl, s, cursor, action) -> {
            if (backOpener != null) backOpener.run();
            else JegHook.openGuide(pl);
            return false;
        });
        if (workCount > 0) {
            menu.addItem(SLOT_PAGE, pageItem("Next Page · Work Recipes"), (pl, s, cursor, action) -> {
                BigRecipeMenu.open(pl, machine, 1, backOpener);
                return false;
            });
        }
        menu.open(p);
    }

    private static void openWorkPage(Player p, WTRecipeMachine machine, WTRecipe r, int workIdx, int workCount, Runnable backOpener) {
        String title = ChatColor.stripColor(machine.getItemName()) + " · Recipe " + (workIdx + 1) + "/" + workCount;
        ChestMenu menu = new ChestMenu(title);
        menu.setEmptySlotsClickable(false);
        for (int i = 0; i < 54; i++) menu.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());

        boolean[] reserved = new boolean[54];
        reserved[SLOT_ICON] = reserved[SLOT_OUTPUT] = reserved[SLOT_BACK] = reserved[SLOT_PAGE] = true;
        ItemStack[] input = r.getInput();
        boolean[] used = new boolean[54];
        int extra = 0;
        for (int i = 0; i < input.length; i++) {
            if (input[i] == null) continue;
            int slot = r.inSlot(i);
            if (slot < 0 || slot >= 54 || reserved[slot]) slot = -1;
            if (slot < 0) {
                for (int s = 52; s >= 0; s--) {
                    if (!reserved[s] && !used[s]) { slot = s; break; }
                }
            }
            if (slot < 0 || used[slot]) { extra++; continue; }
            used[slot] = true;
            ItemStack display = describeInput(input[i], i);
            menu.addItem(slot, display, (pl, s, cursor, action) -> {
                SlimefunItem sf = SlimefunItem.getByItem(display);
                if (sf instanceof WTRecipeMachine sub) {
                    BigRecipeMenu.open(pl, sub, 0, () -> BigRecipeMenu.open(pl, machine, workIdx + 1, backOpener));
                }
                return false;
            });
        }

        ItemStack[] output = r.getOutput();
        ItemStack mainOut = (output.length > 0 && output[0] != null) ? output[0] : new ItemStack(Material.BARRIER);
        menu.addItem(SLOT_OUTPUT, describeOutput(mainOut, r, output.length), ChestMenuUtils.getEmptyClickHandler());
        menu.addItem(SLOT_ICON, describeMachine(machine, r), ChestMenuUtils.getEmptyClickHandler());
        menu.addItem(SLOT_BACK, backItem(backOpener == null), (pl, s, cursor, action) -> {
            if (backOpener != null) backOpener.run();
            else JegHook.openGuide(pl);
            return false;
        });

        if (workCount > 1) {
            ItemStack pageBtn = pageItem("Recipe " + (workIdx + 1) + "/" + workCount);
            ItemMeta pm = pageBtn.getItemMeta();
            if (pm != null) {
                List<String> lore = pm.getLore() != null ? new ArrayList<>(pm.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.GRAY + "Left-click: Next Page");
                lore.add(ChatColor.GRAY + "Right-click: Previous Page");
                pm.setLore(lore);
                pageBtn.setItemMeta(pm);
            }
            menu.addItem(SLOT_PAGE, pageBtn, (pl, s, cursor, action) -> {
                if (action.isRightClicked()) BigRecipeMenu.open(pl, machine, workIdx == 0 ? 0 : workIdx, backOpener);
                else BigRecipeMenu.open(pl, machine, workIdx + 2, backOpener);
                return false;
            });
        } else {
            menu.addItem(SLOT_PAGE, backItem(backOpener == null), (pl, s, cursor, action) -> {
                if (backOpener != null) backOpener.run();
                else JegHook.openGuide(pl);
                return false;
            });
        }
        menu.open(p);
    }

    private static String recipeTypeName(WTRecipeMachine machine) {
        try {
            return ChatColor.stripColor(machine.getRecipeType().toItem().getItemMeta().getDisplayName());
        } catch (Throwable t) {
            return machine.getRecipeType().getKey().getKey();
        }
    }

    private static ItemStack describeInput(ItemStack in, int index) {
        ItemStack clone = in.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GREEN + "Ingredient " + (index + 1));
            if (clone.getAmount() > 1) lore.add(ChatColor.RED + "Amount: " + clone.getAmount());
            lore.add(ChatColor.DARK_GRAY + "Click to view this ingredient's recipe");
            meta.setLore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    private static ItemStack describeCraftInput(ItemStack in, int index) {
        ItemStack clone = in.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GREEN + "Crafting Ingredient " + (index + 1));
            if (clone.getAmount() > 1) lore.add(ChatColor.RED + "Amount: " + clone.getAmount());
            meta.setLore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    private static ItemStack describeOutput(ItemStack out, WTRecipe r, int totalOutputs) {
        ItemStack clone = out.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GREEN + "Output 1");
            int ch = r.chance(0);
            if (ch < 100) lore.add(ChatColor.YELLOW + "Chance: " + ch + "%");
            if (clone.getAmount() > 1) lore.add(ChatColor.RED + "Amount: " + clone.getAmount());
            if (totalOutputs > 1) lore.add(ChatColor.DARK_GRAY + "Plus " + (totalOutputs - 1) + " additional output(s)");
            meta.setLore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    private static ItemStack describeMachine(WTRecipeMachine machine, WTRecipe r) {
        ItemStack icon = machine.getItem().clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            if (r != null) {
                lore.add(ChatColor.GRAY + "Time: " + (r.getTicks() / 2) + "s");
                lore.add(ChatColor.DARK_GRAY + "Crafted in this machine");
            } else {
                lore.add(ChatColor.DARK_GRAY + "Machine Block (Crafting Output)");
            }
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static ItemStack infoItem(String name, String desc) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + desc);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
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

    private static ItemStack backItem(boolean toGuide) {
        ItemStack it = new ItemStack(Material.OAK_DOOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + (toGuide ? "Back to Guide" : "Back"));
            it.setItemMeta(meta);
        }
        return it;
    }
}
