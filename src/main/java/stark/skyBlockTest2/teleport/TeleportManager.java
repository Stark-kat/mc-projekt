package stark.skyBlockTest2.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import stark.skyBlockTest2.rank.RankManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;

public class TeleportManager {

    private final JavaPlugin plugin;
    private final RankManager rankManager;
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();
    private Location spawnLocation;

    public TeleportManager(JavaPlugin plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
        loadSpawn();
    }

    // 🔹 CACHE SPAWNA
    private void loadSpawn() {
        if (!plugin.getConfig().contains("spawn.world")) return;

        World world = Bukkit.getWorld(plugin.getConfig().getString("spawn.world"));
        if (world == null) return;

        spawnLocation = new Location(
                world,
                plugin.getConfig().getDouble("spawn.x"),
                plugin.getConfig().getDouble("spawn.y"),
                plugin.getConfig().getDouble("spawn.z"),
                (float) plugin.getConfig().getDouble("spawn.yaw"),
                (float) plugin.getConfig().getDouble("spawn.pitch")
        );
    }

    public void reloadSpawn() {
        loadSpawn();
    }

    public boolean hasSpawn() {
        return spawnLocation != null;
    }

    public Location getSpawnLocation() {
        return spawnLocation == null ? null : spawnLocation.clone();
    }

    // 🔹 CANCEL (ruch / damage / quit / ponowny spawn)
    public void cancelTeleport(Player p, String reason) {
        BukkitRunnable task = tasks.remove(p.getUniqueId());
        if (task != null) {
            task.cancel();
            p.sendTitle("§cTeleport canceled", reason, 0, 40, 10);
        }
    }
    // Anulowanie przy server stop
    public void cancelAll() {
        tasks.values().forEach(BukkitRunnable::cancel);
        tasks.clear();
    }
    public void teleportWithoutDelay(Player p) {
        if (!hasSpawn()) {
            return;
        }
        p.teleport(spawnLocation);
    }

    // 🔹 START TELEPORTU (spawn) — opóźnienie z config na podstawie rangi
    public void teleportWithDelay(Player p) {
        teleportWithDelay(p, spawnLocation, getDelay(p));
    }

    // 🔹 START TELEPORTU z dowolnym celem — opóźnienie z config na podstawie rangi
    public void teleportWithDelay(Player p, Location destination) {
        teleportWithDelay(p, destination, getDelay(p));
    }

    private int getDelay(Player p) {
        String rank = rankManager.getRank(p).name();
        return plugin.getConfig().getInt("teleport.delay-by-rank." + rank, 5);
    }

    // 🔹 START TELEPORTU z dowolnym celem i jawnym czasem odliczania
    public void teleportWithDelay(Player p, Location destination, int delaySeconds) {

        cancelTeleport(p, "§7Restart teleportu");

        Location start = p.getLocation().clone();

        BukkitRunnable task = new BukkitRunnable() {

            int time = delaySeconds;

            @Override
            public void run() {

                if (p.getLocation().distanceSquared(start) > 0.01) {
                    cancelTeleport(p, "§7Poruszyłeś się");
                    return;
                }

                if (time == 0) {
                    p.teleport(destination);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    p.sendTitle("§aTeleport!", "", 0, 40, 10);
                    tasks.remove(p.getUniqueId());
                    cancel();
                    return;
                }

                p.sendTitle("" + time, "§7Nie ruszaj się!", 0, 20, 0);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                time--;
            }
        };

        tasks.put(p.getUniqueId(), task);
        task.runTaskTimer(plugin, 0L, 20L);
    }
}