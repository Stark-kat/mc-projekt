package stark.skyBlockTest2.border;

import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;


public class BorderManager {

    private final IslandManager islandManager;
    private final World islandWorld;

    public BorderManager(IslandManager islandManager, World islandWorld) {
        this.islandManager = islandManager;
        this.islandWorld = islandWorld;
    }

    public void updateBorder(Player player) {

        if (!player.getWorld().equals(islandWorld)) {
            removeBorder(player);
            return;
        }

        Island island = islandManager.getIslandAt(player.getLocation());
        if (island == null) {
            removeBorder(player);
            return;
        }

        WorldBorder border = org.bukkit.Bukkit.createWorldBorder();

        double centerX = (island.getCenter().getBlockX() >> 4 << 4) + 8.0;
        double centerZ = (island.getCenter().getBlockZ() >> 4 << 4) + 8.0;
        border.setCenter(centerX, centerZ);

        double borderSize = (island.getSize() * 2.0 + 1.0) * 16.0;

        border.setSize(borderSize);
        border.setWarningDistance(0);

        player.setWorldBorder(border);
    }

    public void removeBorder(Player player) {
        player.setWorldBorder(null);
    }
}
