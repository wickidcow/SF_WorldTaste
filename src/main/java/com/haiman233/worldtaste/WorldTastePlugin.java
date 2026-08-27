package com.haiman233.worldtaste;

import com.haiman233.worldtaste.load.Setup;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Standalone WorldTaste addon for Slimefun.
 *
 * <p>The original YAML content remains data-compatible with the upstream project,
 * while the runtime loader is implemented in native Java and does not require
 * RykenSlimefunCustomizer or a GraalVM scripting engine.</p>
 */
public final class WorldTastePlugin extends JavaPlugin implements SlimefunAddon {

    private static WorldTastePlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        WT.plugin = this;
        getLogger().info("Loading WorldTaste...");
        try {
            Setup.loadAll();
            getLogger().info("WorldTaste loaded successfully.");
        } catch (Throwable e) {
            getLogger().severe("WorldTaste failed to load: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("WorldTaste disabled.");
        instance = null;
    }

    public static WorldTastePlugin getInstance() {
        return instance;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/wickidcow/SF_WorldTaste/issues";
    }
}
