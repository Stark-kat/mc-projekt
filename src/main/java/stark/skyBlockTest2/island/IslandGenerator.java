package stark.skyBlockTest2.island;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import stark.skyBlockTest2.SkyBlockTest2;

import java.util.List;

public class IslandGenerator {

    /*public void generateIsland(Island island) {

        Location center = island.getCenter();
        World world = center.getWorld();

        int size = island.getSize();
        int startX = center.getBlockX();
        int startY = center.getBlockY();
        int startZ = center.getBlockZ();

        // Platforma
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {

                for (int y = -4; y < 0; y++) {

                    Block block = world.getBlockAt(startX + x, startY + y, startZ + z);

                    if (y == -1) {
                        block.setType(Material.GRASS_BLOCK);
                    } else {
                        block.setType(Material.DIRT);
                    }
                }
            }
        }

        world.getBlockAt(startX, startY - 1, startZ).setType(Material.BEDROCK);

        world.generateTree(
                new Location(world, startX -2, startY, startZ -2),
                org.bukkit.TreeType.TREE
        );
    }*/

    public void clearIsland(Island island, List<Integer> freeIndexes, IslandStorage storage) {
        int index = island.getIndex();
        World world = island.getCenter().getWorld();

        // Pobieramy zakres chunków z obiektu Island (zmienionego wcześniej)
        int minCX = island.getMinChunkX();
        int maxCX = island.getMaxChunkX();
        int minCZ = island.getMinChunkZ();
        int maxCZ = island.getMaxChunkZ();

        removeEntitiesFromIsland(island);

        new BukkitRunnable() {
            int currentCX = minCX;

            @Override
            public void run() {
                // Czyścimy 2 kolumny chunków na tick, aby nie przeciążyć procesora
                for (int i = 0; i < 2; i++) {
                    if (currentCX > maxCX) {
                        freeIndexes.add(index);
                        storage.setFreeIndexes(freeIndexes);
                        this.cancel();
                        return;
                    }

                    for (int cz = minCZ; cz <= maxCZ; cz++) {
                        Chunk chunk = world.getChunkAt(currentCX, cz);
                        clearChunk(chunk);
                    }
                    currentCX++;
                }
            }
        }.runTaskTimer(SkyBlockTest2.getInstance(), 0L, 1L);
    }

    private void clearChunk(Chunk chunk) {
        // Najszybsza metoda czyszczenia chunka bez użycia NMS:
        // Przechodzimy przez sekcje chunka (16x16x16)
        int minY = chunk.getWorld().getMinHeight();
        int maxY = chunk.getWorld().getMaxHeight();

        for (int y = minY; y < maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.getBlock(x, y, z).setType(org.bukkit.Material.AIR, false);
                }
            }
        }
    }

    public void removeEntitiesFromIsland(Island island) {
        World world = island.getCenter().getWorld();

        for (int cx = island.getMinChunkX(); cx <= island.getMaxChunkX(); cx++) {
            for (int cz = island.getMinChunkZ(); cz <= island.getMaxChunkZ(); cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                for (Entity entity : chunk.getEntities()) {
                    if (!(entity instanceof Player)) {
                        entity.remove();
                    }
                }
            }
        }
    }
}
