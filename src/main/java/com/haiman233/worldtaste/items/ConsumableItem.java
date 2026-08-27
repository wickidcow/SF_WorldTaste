package com.haiman233.worldtaste.items;

import com.haiman233.worldtaste.WT;
import com.haiman233.worldtaste.behavior.Behaviors.ConsumableOpts;
import com.haiman233.worldtaste.behavior.Behaviors.Potion;
import com.haiman233.worldtaste.util.EnglishText;
import com.haiman233.worldtaste.util.Stacks;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Consumable WorldTaste item with configurable hunger, saturation and potion effects. */
public class ConsumableItem extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    private final ConsumableOpts opts;

    public ConsumableItem(ItemGroup group, SlimefunItemStack item, RecipeType rt, ItemStack[] recipe, ConsumableOpts opts) {
        super(group, item, rt, recipe);
        this.opts = opts;
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            Player p = e.getPlayer();
            if (p.isSneaking()) return;
            if (opts.requireHungry && p.getFoodLevel() >= 20) return;
            PlayerInventory inv = p.getInventory();
            ItemStack off = inv.getItemInOffHand();

            if (opts.offhandTool != null) {
                if (off == null || off.getType() != opts.offhandTool) {
                    p.sendMessage("You must use this from your main hand while holding "
                            + opts.offhandTool.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')
                            + " in your off hand!");
                    return;
                }
            } else if (off != null && SlimefunItem.getByItem(off) != null) {
                p.sendMessage("You must eat this from your main hand while keeping Slimefun items out of your off hand!");
                return;
            }

            ItemStack main = inv.getItemInMainHand();
            if (main == null || main.getAmount() <= 0) return;
            Stacks.consumeOneInMainHand(inv);
            if (opts.offhandTool != null && opts.consumeOffhand) {
                Stacks.consumeOneInOffHand(inv);
            }

            int food = opts.randomFood != null ? (ThreadLocalRandom.current().nextInt(opts.randomFood) + 1)
                    : (opts.food != null ? opts.food.intValue() : 0);
            if (opts.foodSet != null) p.setFoodLevel(opts.foodSet);
            else if (food > 0) p.setFoodLevel(p.getFoodLevel() + food);
            if (opts.saturationSet != null) p.setSaturation(opts.saturationSet);
            else if (opts.saturation != null) p.setSaturation((float) (p.getSaturation() + opts.saturation));
            if (opts.exhaustion != null) p.setExhaustion((float) (p.getExhaustion() - opts.exhaustion));
            if (opts.exhaustionSet != null) p.setExhaustion(opts.exhaustionSet.floatValue());
            if (opts.absorption != null) p.setAbsorptionAmount(opts.absorption);
            if (opts.remainingAirAdd != null) p.setRemainingAir(p.getRemainingAir() + opts.remainingAirAdd);
            if (opts.gameMode != null) {
                try { p.setGameMode(org.bukkit.GameMode.valueOf(opts.gameMode.toUpperCase(java.util.Locale.ROOT))); }
                catch (IllegalArgumentException ignored) {}
            }
            if (opts.satRegen != null) p.setSaturatedRegenRate(opts.satRegen);
            if (opts.unsatRegen != null) p.setUnsaturatedRegenRate(opts.unsatRegen);
            if (opts.starvation != null) p.setStarvationRate(opts.starvation);
            if (opts.maxAir != null) p.setMaximumAir(opts.maxAir);
            if (opts.remainingAir != null) p.setRemainingAir(opts.remainingAir);
            if (opts.freezeTicks != null) p.setFreezeTicks(opts.freezeTicks);

            for (Potion pt : opts.potions) {
                PotionEffectType type = PotionEffectType.getByName(pt.type);
                if (type != null) p.addPotionEffect(new PotionEffect(type, pt.duration, pt.amplifier, false));
                else WT.log("Unknown potion effect type: " + pt.type);
            }

            if (opts.message != null) {
                String message = EnglishText.translate(opts.message);
                if (!EnglishText.containsChinese(message)) p.sendMessage(message);
            }
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_STRIDER_EAT, 1f, 1f);
        };
    }
}
