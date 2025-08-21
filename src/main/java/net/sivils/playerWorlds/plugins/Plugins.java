package net.sivils.playerWorlds.plugins;

import net.sivils.playerWorlds.PlayerWorlds;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.HashMap;

public abstract class Plugins {

  private static final HashMap<String, Plugins> plugins = new HashMap<>();

  public static void registerPlugin(Plugins plugin, String name) {
    plugins.put(name, plugin);
    if (plugin instanceof Listener listener) Bukkit.getPluginManager().registerEvents(listener, PlayerWorlds.getInstance());
    plugin.setup();
  }

  public static Collection<String> getPluginNames() {
    return plugins.keySet();
  }

  public static Collection<Plugins> getPlugins() {
    return plugins.values();
  }

  public static Plugins getPlugin(String pluginName) {
    return plugins.get(pluginName);
  }

  public void setup() {}

  public void commandEnable(String worldUUID) {}
  public void commandDisable(String worldUUID) {}
}
