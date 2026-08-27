package com.haiman233.worldtaste.items;

import com.haiman233.worldtaste.WT;
import com.haiman233.worldtaste.util.Stacks;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Special scripted items implemented directly in Java. */
public final class SpecialItems {

    private SpecialItems() {}

    public static SlimefunItem create(String id, ItemGroup group, SlimefunItemStack sfis,
                                      RecipeType rt, ItemStack[] recipe, String script) {
        switch (script) {
            case "yurenjie/buyunping": return new CloudBottleItem(group, sfis, rt, recipe);
            case "jurenwan": return new GiantPillItem(group, sfis, rt, recipe);
            default: return null;
        }
    }

    private static class CloudBottleItem extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {
        CloudBottleItem(ItemGroup g, SlimefunItemStack i, RecipeType rt, ItemStack[] r) { super(g, i, rt, r); }

        @Override
        public ItemUseHandler getItemHandler() {
            return e -> {
                Player p = e.getPlayer();
                Location l = p.getLocation();
                if (l.getY() < 192 || l.getY() > 196) {
                    p.sendMessage("§cYou must be in the cloud layer (Y=192-196) to use a Cloud Bottle!");
                    return;
                }
                ItemStack off = p.getInventory().getItemInOffHand();
                if (off != null && SlimefunItem.getByItem(off) != null) {
                    p.sendMessage("You must use the Cloud Bottle in your main hand and keep Slimefun items out of your off hand!");
                    return;
                }
                ItemStack main = p.getInventory().getItemInMainHand();
                if (main == null || main.getAmount() <= 0) return;

                boolean clear = !p.getWorld().hasStorm() && !p.getWorld().isThundering();
                String dropId = clear ? "WT_CLOUD" : "WT_THUNDERCLOUD";
                SlimefunItem sf = SlimefunItem.getById(dropId);
                if (sf == null) {
                    WT.log("Cloud Bottle output is not registered: " + dropId);
                    return;
                }
                Stacks.consumeOneInMainHand(p.getInventory());
                p.getWorld().dropItemNaturally(l, sf.getItem().clone());
                p.sendMessage("§bSuccessfully captured a " + (clear ? "Cloud" : "Dark Cloud") + "!");
                p.getWorld().playSound(l, Sound.ENTITY_PLAYER_SPLASH, 1f, 1f);
            };
        }
    }

    private static class GiantPillItem extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {
        GiantPillItem(ItemGroup g, SlimefunItemStack i, RecipeType rt, ItemStack[] r) { super(g, i, rt, r); }

        @Override
        public ItemUseHandler getItemHandler() {
            return e -> {
                Player p = e.getPlayer();
                if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
                    p.sendMessage("Hold the required item in your main hand.");
                    return;
                }
                ItemStack main = p.getInventory().getItemInMainHand();
                if (main == null || main.getAmount() <= 0) return;
                Stacks.consumeOneInMainHand(p.getInventory());
                Block target = p.getTargetBlock(null, 5);
                Location loc = target.getLocation().add(0, 1, 0);
                loc.getWorld().spawnEntity(loc, EntityType.GIANT);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_STRIDER_EAT, 1f, 1f);
            };
        }
    }
}
