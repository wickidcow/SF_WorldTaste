package com.haiman233.worldtaste.behavior;

import com.haiman233.worldtaste.WT;
import com.haiman233.worldtaste.load.Yaml;
import com.haiman233.worldtaste.util.Stacks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

/** WorldTaste fishing behavior for the custom rod and bait tables. */
public final class FishingListener implements Listener {

    public static final FishingListener INSTANCE = new FishingListener();

    private static String rodId = "WT_BAIWEIDIAOGAN";
    private static final Map<String, Bait> baits = new HashMap<>();

    private FishingListener() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(WT.plugin, "data/fishing.yml");
        rodId = y.getString("rod", rodId);
        ConfigurationSection bs = y.getConfigurationSection("baits");
        baits.clear();
        if (bs != null) {
            for (String bait : bs.getKeys(false)) {
                List<Drop> drops = new ArrayList<>();
                for (Map<?, ?> m : bs.getMapList(bait)) {
                    Object id = m.get("id");
                    Object w = m.get("weight");
                    if (id instanceof String && w instanceof Number) {
                        drops.add(new Drop((String) id, ((Number) w).intValue()));
                    }
                }
                baits.put(bait, new Bait(drops));
            }
        }
        int total = baits.values().stream().mapToInt(b -> b.drops.size()).sum();
        WT.plugin.getLogger().info("Behavior data: fishing rod=" + rodId + " baits=" + baits.size() + " drops=" + total);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH || e.getCaught() == null) return;
        Player p = e.getPlayer();
        SlimefunItem rod = SlimefunItem.getByItem(p.getInventory().getItemInMainHand());
        if (rod == null || !rod.getId().equals(rodId)) return;
        SlimefunItem bait = SlimefunItem.getByItem(p.getInventory().getItemInOffHand());
        if (bait == null) return;
        Bait table = baits.get(bait.getId());
        if (table == null) return;

        Drop d = select(table);
        if (d == null) return;
        ItemStack stack = resolve(d.id);
        if (stack == null) return;

        e.setCancelled(true);
        Stacks.consumeOneInOffHand(p.getInventory());
        e.getCaught().remove();
        e.getHook().remove();

        stack.setAmount(1);
        java.util.Map<Integer, ItemStack> leftover = p.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            Item ent = p.getWorld().dropItem(p.getLocation().add(0, 1, 0), leftover.get(0));
            ent.setPickupDelay(2);
        }
        p.sendMessage("§bYou caught " + displayName(stack) + " §b×1");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    private static Drop select(Bait table) {
        if (table.total <= 0) return null;
        double r = Math.random() * table.total;
        for (Drop d : table.drops) {
            r -= d.weight;
            if (r <= 0) return d;
        }
        return table.drops.get(table.drops.size() - 1);
    }

    private static ItemStack resolve(String id) {
        SlimefunItem sf = SlimefunItem.getById(id);
        if (sf != null) return sf.getItem().clone();
        Material m = Material.matchMaterial(id);
        return m == null ? null : new ItemStack(m);
    }

    private static String displayName(ItemStack stack) {
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            return stack.getItemMeta().getDisplayName();
        }
        return stack.getType().name().toLowerCase().replace('_', ' ');
    }

    private static final class Drop {
        final String id;
        final int weight;
        Drop(String id, int weight) { this.id = id; this.weight = weight; }
    }

    private static final class Bait {
        final List<Drop> drops;
        final int total;
        Bait(List<Drop> drops) {
            this.drops = drops;
            int t = 0;
            for (Drop d : drops) t += d.weight;
            this.total = t;
        }
    }
}
