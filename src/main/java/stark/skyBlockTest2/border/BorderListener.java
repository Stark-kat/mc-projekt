package stark.skyBlockTest2.border;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import stark.skyBlockTest2.SkyBlockTest2;

public class BorderListener implements Listener {

    private final BorderManager borderManager;
    private final SkyBlockTest2 plugin;

    public BorderListener(BorderManager borderManager, SkyBlockTest2 plugin) {
        this.borderManager = borderManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Opóźnienie o 2 ticki (0.1 sekundy) zazwyczaj wystarcza
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) { // Zawsze sprawdzaj, czy gracz nie wyszedł w międzyczasie
                borderManager.updateBorder(player);
            }
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        borderManager.removeBorder(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                borderManager.updateBorder(player);
            }
        }, 2L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        borderManager.updateBorder(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        borderManager.updateBorder(event.getPlayer());
    }
}
