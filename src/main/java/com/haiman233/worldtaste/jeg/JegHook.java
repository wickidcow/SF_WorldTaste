package com.haiman233.worldtaste.jeg;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * JEG（JustEnoughGuide）集成：为尘世百味的机器注册「配方补全」功能——
 * 玩家在机器 GUI 中可通过 JEG 一键从背包填充输入槽材料。
 *
 * <p>全程反射调用，不编译依赖 JEG：JEG 未安装或 API 变更时静默跳过，
 * 不影响本插件任何其他功能。</p>
 */
public final class JegHook {

    private static final String REGISTRY_CLASS = "com.balugaq.jeg.api.recipe_complete.RecipeCompletableRegistry";

    private JegHook() {}

    /** JEG 是否可用（检测配方补全注册表类是否存在）。 */
    public static boolean available() {
        try {
            Class.forName(REGISTRY_CLASS);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 注册机器的输入槽为 JEG 配方补全槽位。
     *
     * @param item       机器 Slimefun 物品
     * @param inputSlots 输入槽 GUI 索引（有序填充：绑定槽配方要求精确槽位）
     */
    /** 打开 JEG 指南主菜单（大配方菜单返回用；JEG 未安装时静默）。 */
    public static void openGuide(org.bukkit.entity.Player p) {
        try {
            Class<?> clazz = Class.forName("com.balugaq.jeg.utils.GuideUtil");
            Class<?> modeCls = Class.forName("io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode");
            Object mode = io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide.class
                    .getMethod("getDefaultMode").invoke(null);
            clazz.getMethod("openMainMenuAsync", org.bukkit.entity.Player.class, modeCls, int.class)
                    .invoke(null, p, mode, 1);
        } catch (Throwable ignored) {
            // JEG 未安装或 API 变更：静默
        }
    }

    public static void registerRecipeCompletable(SlimefunItem item, int[] inputSlots) {
        try {
            Class<?> clazz = Class.forName(REGISTRY_CLASS);
            clazz.getMethod("registerRecipeCompletable", SlimefunItem.class, int[].class, boolean.class)
                    .invoke(null, item, inputSlots, Boolean.FALSE);
        } catch (Throwable ignored) {
            // JEG 未安装或 API 变更：静默跳过
        }
    }
}