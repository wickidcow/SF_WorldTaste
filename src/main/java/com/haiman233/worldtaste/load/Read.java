package com.haiman233.worldtaste.load;

import com.haiman233.worldtaste.WT;
import com.haiman233.worldtaste.compat.SlimefunItemResolver;
import com.haiman233.worldtaste.util.Colors;
import com.haiman233.worldtaste.util.EnglishText;
import com.haiman233.worldtaste.util.Stacks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

/** Shared WorldTaste item and recipe reader. */
public final class Read {

    private Read() {}

    private static final Pattern HEX64 = Pattern.compile("^[0-9A-Fa-f]{64}$");

    private static final Map<String, PlayerSkin> HASH_SKINS = new HashMap<>();
    private static final Map<String, PlayerSkin> BASE64_SKINS = new HashMap<>();
    private static final Map<String, PlayerSkin> URL_SKINS = new HashMap<>();

    /** Read an item section. When {@code countable=true}, apply its configured amount. */
    public static ItemStack item(ConfigurationSection s, boolean countable) {
        if (s == null) return null;
        String material = s.getString("material", "");
        if (material.isEmpty()) return null;

        String type = s.getString("material_type", "mc");
        String lower = material.toLowerCase(java.util.Locale.ROOT);
        if (material.startsWith("ey") || material.startsWith("ew")) type = "skull";
        else if (lower.startsWith("http")) type = "skull_url";
        else if (HEX64.matcher(material).matches()) type = "skull_hash";

        ItemStack stack = resolve(type, material);
        if (stack == null) return null;

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = s.getString("name");
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(Colors.c(EnglishText.displayName(s, name, material)));
            }
            List<String> lore = EnglishText.lore(s.getStringList("lore"));
            if (lore != null && !lore.isEmpty()) meta.setLore(Colors.c(lore));
            String color = s.getString("color");
            if (color != null && !color.isEmpty()) applyColor(meta, color);
            stack.setItemMeta(meta);
        }

        if (s.getBoolean("glow", false)) Stacks.glow(stack);
        if (countable) {
            int amt = s.getInt("amount", 1);
            if (amt > 0) stack.setAmount(Math.min(amt, stack.getMaxStackSize()));
        }
        return stack;
    }

    private static ItemStack resolve(String type, String material) {
        switch (type.toLowerCase(java.util.Locale.ROOT)) {
            case "none":
                return new ItemStack(Material.AIR);
            case "skull_hash": {
                PlayerSkin skin = HASH_SKINS.get(material);
                if (skin == null) {
                    skin = PlayerSkin.fromHashCode(material);
                    HASH_SKINS.put(material, skin);
                }
                return PlayerHead.getItemStack(skin);
            }
            case "skull":
            case "skull_base64": {
                PlayerSkin skin = BASE64_SKINS.get(material);
                if (skin == null) {
                    skin = PlayerSkin.fromBase64(material);
                    BASE64_SKINS.put(material, skin);
                }
                return PlayerHead.getItemStack(skin);
            }
            case "skull_url": {
                PlayerSkin skin = URL_SKINS.get(material);
                if (skin == null) {
                    skin = PlayerSkin.fromURL(material);
                    URL_SKINS.put(material, skin);
                }
                return PlayerHead.getItemStack(skin);
            }
            case "slimefun": {
                String id = material.toUpperCase(java.util.Locale.ROOT);
                SlimefunItem sf = SlimefunItemResolver.resolve(id);
                if (sf != null) return sf.getItem().clone();
                ItemStack pre = WT.preload.get(id);
                if (pre != null) return pre.clone();
                WT.log("Slimefun item not found: " + id + "; falling back to STONE");
                return new ItemStack(Material.STONE);
            }
            default: {
                Material m = matchMaterial(material);
                if (m == null) {
                    WT.log("Unknown material: " + material + "; falling back to STONE");
                    return new ItemStack(Material.STONE);
                }
                return new ItemStack(m);
            }
        }
    }

    private static Material matchMaterial(String name) {
        if (name == null) return null;
        Material m = Material.matchMaterial(name);
        if (m != null) return m;
        if (name.equalsIgnoreCase("GRASS")) return Material.matchMaterial("SHORT_GRASS");
        if (name.equalsIgnoreCase("SCUTE")) return Material.matchMaterial("TURTLE_SCUTE");
        return Material.matchMaterial(name.replace('-', '_'));
    }

    /** Release the temporary skin cache after content loading completes. */
    public static void clearSkinCache() {
        HASH_SKINS.clear();
        BASE64_SKINS.clear();
        URL_SKINS.clear();
    }

    /** Read recipe slots named {@code 1..size}. Empty slots remain null. */
    public static ItemStack[] recipe(ConfigurationSection recipeSec, int size) {
        ItemStack[] out = new ItemStack[size];
        if (recipeSec == null) return out;
        for (int i = 0; i < size; i++) {
            ConfigurationSection slot = recipeSec.getConfigurationSection(String.valueOf(i + 1));
            if (slot != null) out[i] = item(slot, true);
        }
        return out;
    }

    private static void applyColor(ItemMeta meta, String color) {
        String[] rgb = color.split(",");
        if (rgb.length != 3) return;
        try {
            int r = Integer.parseInt(rgb[0].trim());
            int g = Integer.parseInt(rgb[1].trim());
            int b = Integer.parseInt(rgb[2].trim());
            Color c = Color.fromRGB(r, g, b);
            if (meta instanceof LeatherArmorMeta leather) leather.setColor(c);
            else if (meta instanceof PotionMeta potion) potion.setColor(c);
        } catch (RuntimeException ignored) {
            WT.log("Invalid color format: " + color);
        }
    }
}
