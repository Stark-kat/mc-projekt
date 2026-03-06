package stark.skyBlockTest2.island;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import stark.skyBlockTest2.SkyBlockTest2;

import java.util.List;

public class IslandGenerator {

    /**
     * Czyści obszar wyspy chunk po chunku, rozłożone po 2 chunki na tick
     * żeby nie przeciążać serwera.
     * Po zakończeniu dodaje indeks wyspy do listy wolnych slotów.
     */
    /**
     * @param onComplete Runnable wykonywany po zwolnieniu obszaru (na głównym wątku).
     *                   Odpowiedzialny za zapis stanu indeksów do storage.
     */
    public void clearIsland(Island island, List<Integer> freeIndexes, Runnable onComplete) {
        int index = island.getIndex();
        World world = island.getCenter().getWorld();

        if (world == null) {
            freeIndexes.add(index);
            if (onComplete != null) onComplete.run();
            return;
        }

        int minCX = island.getMinChunkX();
        int maxCX = island.getMaxChunkX();
        int minCZ = island.getMinChunkZ();
        int maxCZ = island.getMaxChunkZ();

        // Usuwamy byty zanim zaczniemy kasować bloki
        removeEntitiesFromIsland(island);

        new BukkitRunnable() {
            int currentCX = minCX;

            @Override
            public void run() {
                // 2 kolumny chunków na tick — kompromis między szybkością a obciążeniem
                for (int i = 0; i < 2; i++) {
                    if (currentCX > maxCX) {
                        // Czyszczenie zakończone — zwalniamy indeks
                        freeIndexes.add(index);
                        if (onComplete != null) onComplete.run();
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

    /**
     * Wypełnia cały chunk powietrzem.
     * Używamy setType(AIR, false) — false wyłącza propagację fizyki bloków
     * (np. piasek/żwir nie spada, woda nie płynie), co znacznie przyspiesza czyszczenie.
     */
    private void clearChunk(Chunk chunk) {
        int minY = chunk.getWorld().getMinHeight();
        int maxY = chunk.getWorld().getMaxHeight();

        for (int y = minY; y < maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    // Pomijamy bloki które już są powietrzem — oszczędzamy wywołania
                    if (!chunk.getBlock(x, y, z).isEmpty()) {
                        chunk.getBlock(x, y, z).setType(org.bukkit.Material.AIR, false);
                    }
                }
            }
        }
    }

    /**
     * Usuwa wszystkie byty (oprócz graczy) z obszaru wyspy.
     * Sprawdza czy chunk jest załadowany PRZED próbą dostępu —
     * getChunkAt() bez tego ładuje chunk, co jest kosztowne i niepotrzebne.
     */
    public void removeEntitiesFromIsland(Island island) {
        World world = island.getCenter().getWorld();
        if (world == null) return;

        for (int cx = island.getMinChunkX(); cx <= island.getMaxChunkX(); cx++) {
            for (int cz = island.getMinChunkZ(); cz <= island.getMaxChunkZ(); cz++) {
                // Tylko załadowane chunki — nie ładujemy na siłę
                if (!world.isChunkLoaded(cx, cz)) continue;

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