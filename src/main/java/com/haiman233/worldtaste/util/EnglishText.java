package com.haiman233.worldtaste.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;

/**
 * English presentation helpers for the original WorldTaste content pack.
 *
 * <p>The original data contains thousands of Chinese display strings. We keep all
 * configuration keys and Slimefun IDs unchanged for save/recipe compatibility, but
 * translate common UI text and use stable IDs as an English fallback for names that
 * have not yet received a hand-written translation.</p>
 */
public final class EnglishText {

    private static final Pattern CJK = Pattern.compile("[\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uF900-\\uFAFF]");
    private static final Pattern FORMAT_PREFIX = Pattern.compile(
            "^((?:(?:&|§)[0-9A-FK-ORa-fk-or]|(?:\\{#[0-9A-Fa-f]{6}})|(?:&#[0-9A-Fa-f]{6}))*)");
    private static final Pattern SPLIT_WORDS = Pattern.compile("[_\\-.:/]+");

    private static final Map<String, String> EXACT = new LinkedHashMap<>();
    private static final Map<String, String> FRAGMENTS = new LinkedHashMap<>();

    static {
        // Slimefun guide groups.
        exact("&4&l中餐", "&4&lChinese Cuisine");
        exact("&4&l肉类与海鲜", "&4&lMeat & Seafood");
        exact("&5&l怪味餐", "&5&lStrange Cuisine");
        exact("&6&k&ld&a&l“毒薯管理员”&6&k&ld", "&6&k&ld&a&lPoison Potato Administrator&6&k&ld");
        exact("&6&k&ld&c&l河豚钓鱼佬&6&k&ld", "&6&k&ld&c&lPufferfish Angler&6&k&ld");
        exact("&6&l主题餐饮", "&6&lThemed Cuisine");
        exact("&6&l功能丸子", "&6&lSpecialty Meatballs");
        exact("&6&l厨房用具", "&6&lKitchen Tools");
        exact("&6&l快餐", "&6&lFast Food");
        exact("&6&l果切", "&6&lCut Fruit");
        exact("&6&l烤肉", "&6&lGrilled Meat");
        exact("&6&l饮品", "&6&lBeverages");
        exact("&7&k&ld&6&l生日快乐&7&k&ld", "&7&k&ld&6&lHappy Birthday&7&k&ld");
        exact("&7&k&ld&d&l肝帝的晚宴&7&k&ld", "&7&k&ld&d&lThe Grinder's Banquet&7&k&ld");
        exact("&7&k&ld&e&l星露谷拓展&7&k&ld", "&7&k&ld&e&lStardew Expansion&7&k&ld");
        exact("&7&kll{#D9D919}尘{#856363}世{#FF1CAE}百{#CD7F32}味&7&kll", "&7&kll{#D9D919}World{#856363}Taste&7&kll");
        exact("&a&l作物", "&a&lCrops");
        exact("&a&l作物种子", "&a&lCrop Seeds");
        exact("&a&l动物与卵", "&a&lAnimals & Eggs");
        exact("&b&k&ld&f&l罐头食品&b&k&ld", "&b&k&ld&f&lCanned Food&b&k&ld");
        exact("&b&l信息", "&b&lInformation");
        exact("&b&l汤与炖菜", "&b&lSoups & Stews");
        exact("&c&k&ld&e&l中秋节月饼&c&k&ld", "&c&k&ld&e&lMid-Autumn Mooncakes&c&k&ld");
        exact("&d&l冰激凌与糖", "&d&lIce Cream & Candy");
        exact("&d&l食材", "&d&lIngredients");
        exact("&d&l香烟", "&d&lCigarettes");
        exact("&e&k&ld&d&l逻辑食品&e&k&ld", "&e&k&ld&d&lLogic Food&e&k&ld");
        exact("&e&l厨房与门店装饰", "&e&lKitchen & Shop Decor");
        exact("&e&l发酵食品", "&e&lFermented Food");
        exact("&e&l甜品", "&e&lDesserts");
        exact("&e&l贴士", "&e&lTips");
        exact("&e&l零食", "&e&lSnacks");
        exact("&f&k&ld&a&l焙茶工艺&f&k&ld", "&f&k&ld&a&lTea Roasting&f&k&ld");
        exact("&f&l屠宰食材", "&f&lButchering Ingredients");
        exact("&f&l工具", "&f&lTools");
        exact("&f&l日料", "&f&lJapanese Cuisine");
        exact("&f&l烘焙", "&f&lBaking");

        // Machine/menu labels from menus.yml.
        exact("§2§l普忒头精炼机", "§2§lPotato Refinery");
        exact("§4§l屠宰机", "§4§lButchering Machine");
        exact("§6§l人造肉合成机", "§6§lSynthetic Meat Fabricator");
        exact("§6§l单击合成", "§6§lClick to Craft");
        exact("§6§l基因分析仪", "§6§lGenetic Analyzer");
        exact("§6§l恒温陈酿皿", "§6§lTemperature-Controlled Aging Vessel");
        exact("§6§l河豚工作台", "§6§lPufferfish Workbench");
        exact("§6§l蟹笼", "§6§lCrab Pot");
        exact("§6§l请在下方放入放血刀/砍刀", "§6§lPlace a Bleeding Knife/Cleaver Below");
        exact("§6§l请在下方放入需要屠宰的整只生物/生物头颅", "§6§lPlace the Whole Mob/Mob Head to Butcher Below");
        exact("§6单击合成", "§6Click to Craft");
        exact("§6此处放入蛋", "§6Place Eggs Here");
        exact("§6此处放入饲料", "§6Place Feed Here");
        exact("§7§l§kd§f§l终焉厨锅§7§l§kd", "§7§l§kd§f§lEnd Kitchen Pot§7§l§kd");
        exact("§a§l农耕种子分析仪", "§a§lFarming Seed Analyzer");
        exact("§a§l切毒普忒头机", "§a§lPoison Potato Cutter");
        exact("§b§l在下方放入诱饵", "§b§lPlace Bait Below");
        exact("§b§l水产培育机", "§b§lAquaculture Breeder");
        exact("§b§l百味捕鱼网", "§b§lWorldTaste Fishing Net");
        exact("§c§l产卵室", "§c§lSpawning Chamber");
        exact("§c§l恒温水产品处理机", "§c§lTemperature-Controlled Seafood Processor");
        exact("§c§l筒仓", "§c§lSilo");
        exact("§d§l孵化室", "§d§lIncubation Chamber");
        exact("§e§km§6§l百味万用炉§e§km", "§e§km§6§lWorldTaste Universal Oven§e§km");
        exact("§e§l电动捕鼠夹", "§e§lElectric Mousetrap");
        exact("§e§l种子分析仪", "§e§lSeed Analyzer");
        exact("§f§l水族箱", "§f§lAquarium");
        exact("§f§l焙茶炉", "§f§lTea Roaster");
        exact("§f§l电茶壶", "§f§lElectric Kettle");
        exact("§f§l电饭煲", "§f§lRice Cooker");
        exact("§f§l食品原料加工机", "§f§lFood Ingredient Processor");

        // Common food/lore strings.
        exact("§7§o恢复 §b§o1.0 §7§o点饥饿值和饱和度", "§7§oRestores §b§o1.0 §7§ohunger and saturation");
        exact("§7§o恢复 §b§o3.0 §7§o点饥饿值和饱和度", "§7§oRestores §b§o3.0 §7§ohunger and saturation");
        exact("§7§o饮用后可获得强力药水效果", "§7§oGrants powerful potion effects when consumed");
        exact("§7我建议你给老鼠吃这个", "§7I suggest feeding this to a rat");

        // Guide and interaction text. These also make concatenated messages readable.
        fragment("尘世百味", "WorldTaste");
        fragment("合成配方", "Crafting Recipe");
        fragment("选择配方", "Select Recipe");
        fragment("配方补全", "Recipe Fill");
        fragment("下一页", "Next Page");
        fragment("上一页", "Previous Page");
        fragment("返回指南", "Back to Guide");
        fragment("返回机器", "Back to Machine");
        fragment("返回", "Back");
        fragment("关闭配方选择，回到机器界面", "Close recipe selection and return to the machine");
        fragment("点击查看该材料配方", "Click to view this ingredient's recipe");
        fragment("在该机器中制作", "Crafted in this machine");
        fragment("机器本体（合成产物）", "Machine Block (Crafting Output)");
        fragment("数量", "Amount");
        fragment("概率", "Chance");
        fragment("耗时", "Time");
        fragment("材料", "Ingredient");
        fragment("产物", "Output");
        fragment("食物", "Food");
        fragment("作物", "Crop");
        fragment("饥饿值", "Hunger");
        fragment("饱和度", "Saturation");
        fragment("恢复", "Restores");
        fragment("使用", "Use");
        fragment("点击", "Click");
        fragment("左键", "Left-click");
        fragment("右键", "Right-click");
        fragment("请主手持有相应物品", "Hold the required item in your main hand");
        fragment("背包缺少", "Inventory is missing");
        fragment("没有空余输入槽", "No free input slots");
        fragment("该机器没有可补全的配方", "This machine has no recipe that can be filled");
        fragment("成功捕获了", "Caught");
        fragment("恭喜你钓到了", "You caught");
        fragment("云朵", "Cloud");
        fragment("乌云", "Dark Cloud");
    }

    private EnglishText() {}

    private static void exact(String source, String english) {
        EXACT.put(source, english);
    }

    private static void fragment(String source, String english) {
        FRAGMENTS.put(source, english);
    }

    public static boolean containsChinese(String text) {
        return text != null && CJK.matcher(text).find();
    }

    /** Translate exact/common UI phrases without changing IDs or formatting codes. */
    public static String translate(String text) {
        if (text == null || text.isEmpty() || !containsChinese(text)) return text;
        String exact = EXACT.get(text);
        if (exact != null) return exact;

        String result = text;
        for (Map.Entry<String, String> entry : FRAGMENTS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Translate an item/group display name. If no exact translation is known, use the
     * stable surrounding configuration ID and preserve the original formatting prefix.
     */
    public static String displayName(ConfigurationSection section, String original, String material) {
        if (original == null || original.isEmpty() || !containsChinese(original)) return original;
        String translated = translate(original);
        if (!containsChinese(translated)) return translated;

        String id = findStableId(section);
        if (id == null || id.isBlank()) id = material;
        String fallback = titleCaseId(id);
        if (fallback.isBlank()) fallback = "WorldTaste Item";

        Matcher matcher = FORMAT_PREFIX.matcher(original);
        String prefix = matcher.find() ? matcher.group(1) : "";
        return prefix + fallback;
    }

    /**
     * Translate common lore phrases and omit lines that still contain untranslated CJK.
     * This guarantees English-only player-facing lore without corrupting content IDs.
     */
    public static List<String> lore(List<String> original) {
        if (original == null || original.isEmpty()) return original;
        List<String> out = new ArrayList<>(original.size());
        for (String line : original) {
            String translated = translate(line);
            if (!containsChinese(translated)) out.add(translated);
        }
        return out;
    }

    private static String findStableId(ConfigurationSection section) {
        ConfigurationSection current = section;
        while (current != null) {
            String name = current.getName();
            if (isUsefulId(name)) return name;
            current = current.getParent();
        }
        return null;
    }

    private static boolean isUsefulId(String value) {
        if (value == null || value.isBlank() || value.matches("\\d+")) return false;
        String v = value.toLowerCase(Locale.ROOT);
        return !v.equals("item")
                && !v.equals("recipe")
                && !v.equals("input")
                && !v.equals("inputs")
                && !v.equals("output")
                && !v.equals("outputs")
                && !v.equals("icon")
                && !v.equals("display")
                && !v.equals("work")
                && !v.equals("drops")
                && !v.equals("weighteddrops");
    }

    public static String titleCaseId(String id) {
        if (id == null || id.isBlank()) return "";
        String cleaned = id.trim();
        if (cleaned.regionMatches(true, 0, "WT_", 0, 3)) cleaned = cleaned.substring(3);
        String[] words = SPLIT_WORDS.split(cleaned);
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            String lower = word.toLowerCase(Locale.ROOT);
            out.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return out.toString();
    }
}
