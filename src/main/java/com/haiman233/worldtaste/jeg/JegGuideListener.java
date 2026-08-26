package com.haiman233.worldtaste.jeg;

import com.balugaq.jeg.api.objects.events.GuideEvents;
import com.haiman233.worldtaste.WT;
import com.haiman233.worldtaste.guide.BigRecipeMenu;
import com.haiman233.worldtaste.machines.WTRecipeMachine;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * JEG 指南物品点击拦截：点击尘世百味机器物品时，取消 JEG 默认配方页，
 * 改为打开 {@link BigRecipeMenu} 完整配方表页面（对齐 LogiTech 大配方展示）。
 * 仅当 JEG 存在时由插件加载本类（见 {@link JegGuideListener#register()}）。
 */
public final class JegGuideListener implements Listener {

    /** 仅在 JEG 可用时调用（类加载安全：本类直接引用 JEG API）。 */
    public static void register() {
        if (JegHook.available()) {
            Bukkit.getPluginManager().registerEvents(new JegGuideListener(), WT.plugin);
            WT.plugin.getLogger().info("JEG 集成：大配方菜单已启用");
        }
    }

    private JegGuideListener() {}

    @EventHandler(ignoreCancelled = true)
    public void onItemClick(GuideEvents.ItemButtonClickEvent e) {
        ItemStack clicked = e.getClickedItem();
        if (clicked == null || clicked.getType().isAir()) return;
        SlimefunItem sf = SlimefunItem.getByItem(clicked);
        if (sf instanceof WTRecipeMachine machine && BigRecipeMenu.isLargeRecipeMachine(machine)) {
            // 仅大型配方机器（如终焉厨锅）拦截并打开大配方菜单；普通机器保留 JEG 默认展示
            e.setCancelled(true);
            BigRecipeMenu.open(e.getPlayer(), machine, 0, null);
        }
    }
}