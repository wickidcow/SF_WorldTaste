package com.haiman233.worldtaste.load;

import com.haiman233.worldtaste.WT;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SeasonalItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import java.time.Month;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Loads WorldTaste guide groups. */
public final class GroupLoader {

    private static final Map<String, Material> GUIDE_ICONS = Map.ofEntries(
            Map.entry("worldtaste", Material.CAKE),
            Map.entry("ws_cfzs", Material.BRICKS),
            Map.entry("ws_zhongzi", Material.WHEAT_SEEDS),
            Map.entry("ws_zhishi", Material.HONEY_BOTTLE),
            Map.entry("ws_yan", Material.PAPER),
            Map.entry("ws_hongbei", Material.BREAD),
            Map.entry("ws_tuzai", Material.IRON_SWORD),
            Map.entry("ws_info", Material.BOOK),
            Map.entry("ws_jiqi", Material.FURNACE),
            Map.entry("ws_zuowu", Material.WHEAT),
            Map.entry("ws_kuaican", Material.COOKED_BEEF),
            Map.entry("ws_zhongcan", Material.BOWL),
            Map.entry("ws_tang", Material.MUSHROOM_STEW),
            Map.entry("ws_kaorou", Material.COOKED_PORKCHOP),
            Map.entry("ws_tieshi", Material.WRITABLE_BOOK),
            Map.entry("ws_shicai", Material.CARROT),
            Map.entry("ws_roulei", Material.COD),
            Map.entry("ws_bingjiling", Material.SNOWBALL),
            Map.entry("ws_riliao", Material.DRIED_KELP),
            Map.entry("ws_dangao", Material.CAKE),
            Map.entry("ws_lingshi", Material.COOKIE),
            Map.entry("ws_gongju", Material.IRON_PICKAXE),
            Map.entry("ws_dongwu", Material.EGG),
            Map.entry("ws_yingliao", Material.POTION),
            Map.entry("ws_dacan", Material.SUSPICIOUS_STEW),
            Map.entry("ws_wanzi", Material.SLIME_BALL),
            Map.entry("ws_guoqie", Material.MELON_SLICE),
            Map.entry("ws_guantou", Material.IRON_NUGGET),
            Map.entry("ws_yuebing", Material.PUMPKIN_PIE),
            Map.entry("ws_luojishipin", Material.COMPARATOR),
            Map.entry("ws_chagongyi", Material.CAMPFIRE),
            Map.entry("ws_shengri", Material.CAKE));

    private GroupLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource(WT.plugin, "groups.yml");
        for (String key : y.getKeys(false)) {
            try {
                ConfigurationSection s = y.getConfigurationSection(key);
                if (s == null) continue;
                String type = s.getString("type", "normal").toLowerCase(Locale.ROOT);
                if (type.equals("nested") || type.equals("parent")) registerNested(key, s);
            } catch (Exception e) {
                WT.log("groups " + key + " registration failed; skipping: " + e);
            }
        }

        int ok = WT.groups.size();
        for (String key : y.getKeys(false)) {
            try {
                ConfigurationSection s = y.getConfigurationSection(key);
                if (s == null) continue;
                String type = s.getString("type", "normal").toLowerCase(Locale.ROOT);
                if (type.equals("nested") || type.equals("parent")) continue;
                registerChild(key, s, type);
            } catch (Exception e) {
                WT.log("groups " + key + " registration failed; skipping: " + e);
            }
        }
        WT.plugin.getLogger().info("groups.yml: registered " + (WT.groups.size() - ok) + " subgroups, total " + WT.groups.size());
    }

    private static void registerNested(String key, ConfigurationSection s) {
        ItemStack display = Read.item(s.getConfigurationSection("item"), false);
        if (display == null) {
            WT.log("groups " + key + ": display item missing");
            return;
        }
        int tier = s.getInt("tier", 3);
        NestedItemGroup g = new NestedItemGroup(nsKey(key), guideIcon(key, display), tier);
        g.register(WT.plugin);
        WT.groups.put(key.toLowerCase(Locale.ROOT), g);
    }

    private static void registerChild(String key, ConfigurationSection s, String type) {
        ItemStack display = Read.item(s.getConfigurationSection("item"), false);
        if (display == null) {
            WT.log("groups " + key + ": display item missing");
            return;
        }
        ItemStack guideDisplay = guideIcon(key, display);
        int tier = s.getInt("tier", 3);
        switch (type) {
            case "seasonal": {
                int month = s.getInt("month", 1);
                SeasonalItemGroup g = new SeasonalItemGroup(nsKey(key), Month.of(Math.max(1, Math.min(12, month))), tier, guideDisplay);
                g.register(WT.plugin);
                WT.groups.put(key.toLowerCase(Locale.ROOT), g);
                break;
            }
            case "sub":
            case "button":
            default: {
                String parentId = s.getString("parent");
                ItemGroup parent = parentId == null ? null : WT.groups.get(parentId.toLowerCase(Locale.ROOT));
                if (parent instanceof NestedItemGroup nested) {
                    SubItemGroup g = new SubItemGroup(nsKey(key), nested, guideDisplay, tier);
                    g.register(WT.plugin);
                    WT.groups.put(key.toLowerCase(Locale.ROOT), g);
                } else {
                    ItemGroup g = new ItemGroup(nsKey(key), guideDisplay, tier);
                    g.register(WT.plugin);
                    WT.groups.put(key.toLowerCase(Locale.ROOT), g);
                }
                break;
            }
        }
    }

    /**
     * WorldTaste has many unique textured-head category icons. Sending dozens of
     * different skull profiles in one guide page can cause a large client-side
     * texture burst. Keep the actual WorldTaste items unchanged, but use lightweight
     * vanilla icons for guide categories that were player heads.
     */
    private static ItemStack guideIcon(String key, ItemStack original) {
        if (original.getType() != Material.PLAYER_HEAD) return original;

        Material type = GUIDE_ICONS.getOrDefault(key.toLowerCase(Locale.ROOT), Material.BOOK);
        ItemStack safe = new ItemStack(type);
        ItemMeta source = original.getItemMeta();
        ItemMeta target = safe.getItemMeta();
        if (source != null && target != null) {
            if (source.hasDisplayName()) target.setDisplayName(source.getDisplayName());
            if (source.hasLore()) target.setLore(source.getLore());
            safe.setItemMeta(target);
        }
        return safe;
    }

    private static NamespacedKey nsKey(String key) {
        return new NamespacedKey(WT.plugin, key.toLowerCase(Locale.ROOT));
    }
}
