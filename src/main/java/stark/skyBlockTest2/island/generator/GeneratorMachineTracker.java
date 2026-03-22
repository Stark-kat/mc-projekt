package stark.skyBlockTest2.island.generator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import stark.skyBlockTest2.island.Island;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

public class GeneratorMachineTracker {

    private final JavaPlugin plugin;
    private final GeneratorManager generatorManager;

    // location maszyny -> aktywny task
    private final Map<Location, BukkitTask> tasks = new HashMap<>();

    public GeneratorMachineTracker(JavaPlugin plugin, GeneratorManager generatorManager) {
        this.plugin           = plugin;
        this.generatorManager = generatorManager;
    }

    public void register(Location loc, GeneratorType type, GeneratorDefinition definition,
                         Supplier<Integer> levelSupplier) {
        if (tasks.containsKey(loc)) return; // już zarejestrowana

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> tick(loc, type, definition, levelSupplier),
                definition.getIntervalTicks(),
                definition.getIntervalTicks()
        );
        tasks.put(loc, task);
    }

    public void unregister(Location loc) {
        BukkitTask task = tasks.remove(loc);
        if (task != null) task.cancel();
    }

    public boolean isRegistered(Location loc) {
        return tasks.containsKey(loc);
    }

    public int getMachineCount() {
        return tasks.size();
    }

    public void unregisterAll() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }

    /** Zatrzymuje i usuwa wszystkie maszyny należące do podanej wyspy. */
    public void unregisterAllInIsland(Island island) {
        Iterator<Map.Entry<Location, BukkitTask>> it = tasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, BukkitTask> entry = it.next();
            if (island.isInside(entry.getKey())) {
                entry.getValue().cancel();
                it.remove();
            }
        }
    }

    private void tick(Location machineLoc, GeneratorType type,
                      GeneratorDefinition definition, Supplier<Integer> levelSupplier) {
        Block machine = machineLoc.getBlock();
        // Sprawdź czy maszyna nadal istnieje
        if (machine.getType() != definition.getMachineBlock()) {
            unregister(machineLoc);
            return;
        }

        int level = levelSupplier.get();
        GeneratorLevel generatorLevel = definition.getLevel(level);
        Material toPlace = generatorLevel.roll();

        // Szukamy wolnego miejsca bezpośrednio nad generatorem
        int radius = definition.getRadius();
        for (int dy = 1; dy <= radius; dy++) {
            Block candidate = machineLoc.getBlock().getRelative(0, dy, 0);
            if (candidate.getType() == Material.AIR) {
                if (!toPlace.isBlock()) return; // pomiń — nie da się postawić
                candidate.setType(toPlace);
                return;
            }
        }
        // Brak miejsca nad generatorem — nic nie robimy
    }
}