package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class RandomEventsSurvival extends JavaPlugin  {

    @Override
    public void onEnable() {
        Objects.requireNonNull(this.getCommand("test")).setExecutor(new TestCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
