package net.sivils.playerWorlds.plugins;

import net.sivils.playerWorlds.PlayerWorlds;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.HashMap;

public class Plugins {

  private static final HashMap<Plugins, String> plugins = new HashMap<>();

  public static void registerPlugin(Plugins plugin, String name) {
    plugins.put(plugin, name);
    if (plugin instanceof Listener listener) Bukkit.getPluginManager().registerEvents(listener, PlayerWorlds.getInstance());
  }

  public static Collection<String> getPluginNames() {
    return plugins.values();
  }

}
