package me.vaape.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.shininet.bukkit.playerheads.events.PlayerDropHeadEvent;

public class BeheadListener implements Listener {

    public BeheadListener(Events passedPlugin) {
        CrateDrops.plugin = passedPlugin;
    }

    @EventHandler
    public void onBehead(PlayerDropHeadEvent event) {
        if (Fishing.inFish(event.getEntity().getLocation()) || GuildWars.inCastle(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }
}
