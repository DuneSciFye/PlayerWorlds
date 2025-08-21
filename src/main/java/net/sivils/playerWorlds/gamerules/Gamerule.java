package net.sivils.playerWorlds.gamerules;

import net.sivils.playerWorlds.PlayerWorlds;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.HashMap;

public class Gamerule {

  private static final HashMap<String, Gamerule> gamerules = new HashMap<>();

  public static void registerGamerule(Gamerule gamerule, String name) {
    gamerules.put(name, gamerule);
    if (gamerule instanceof Listener listener) Bukkit.getPluginManager().registerEvents(listener, PlayerWorlds.getInstance());
    gamerule.setup();
  }

  public static Collection<String> getGameruleNames() {
    return gamerules.keySet();
  }

  public void setup() {}

  public void commandEnable(String worldUUID) {}
  public void commandDisable(String worldUUID) {}
}
