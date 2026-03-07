package stark.skyBlockTest2.teleport;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class TeleportListener implements Listener {

    private final TeleportManager teleportManager;

    public TeleportListener(TeleportManager teleportManager) {
        this.teleportManager = teleportManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        teleportManager.cancelTeleport(e.getPlayer(), "§7Opuściłeś serwer");
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            teleportManager.cancelTeleport(p, "§7Otrzymałeś obrażenia");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPlayedBefore()) {
            teleportManager.teleportWithoutDelay(player);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (teleportManager.hasSpawn()) {
            e.getPlayer().setRespawnLocation(teleportManager.getSpawnLocation());
        }
    }

    @EventHandler
    public void onVoidFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
                teleportManager.teleportWithoutDelay(player);
                player.sendMessage("§cSpadłeś do pustki! Teleportowano na spawn");
        }
    }
}


