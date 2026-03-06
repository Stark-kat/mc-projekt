package stark.skyBlockTest2.border;

import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.IslandType;

public class BorderManager {

    private final IslandManager islandManager;

    public BorderManager(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    private boolean isIslandWorld(World world) {
        if (world == null) return false;
        for (IslandType type : IslandType.values()) {
            if (type.worldName.equals(world.getName())) return true;
        }
        return false;
    }

    public void updateBorder(Player player) {
        if (!isIslandWorld(player.getWorld())) {
            removeBorder(player);
            return;
        }

        Island island = islandManager.getIslandAt(player.getLocation());
        if (island == null) {
            removeBorder(player);
            return;
        }

        WorldBorder border = org.bukkit.Bukkit.createWorldBorder();

        // Środek wyspy jest już ustawiony jako chunkX*16 + 8 w IslandManager —
        // możemy go użyć bezpośrednio zamiast przeliczać z powrotem przez chunk
        border.setCenter(island.getCenter().getX(), island.getCenter().getZ());

        // Rozmiar: (size*2 + 1) chunków * 16 bloków
        // np. size=0 → 1 chunk = 16 bloków, size=1 → 3 chunki = 48 bloków
        double borderSize = (island.getSize() * 2.0 + 1.0) * 16.0;
        border.setSize(borderSize);

        // Wyłączamy oba ostrzeżenia — bez czerwonego ekranu i bez żółtych linii
        border.setWarningDistance(0);

        player.setWorldBorder(border);
    }

    public void removeBorder(Player player) {
        player.setWorldBorder(null);
    }
}