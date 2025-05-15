package net.sivils.playerWorlds.hooks;

public class PlaceholderAPIHook {

    private static boolean enabled = false;

    public static void setEnabled() {
        enabled = true;
    }

    public static boolean isEnabled() {
        return enabled;
    }

}
