package stark.skyBlockTest2.island.generator;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.quest.QuestManager;
import stark.skyBlockTest2.quest.QuestTrigger;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class GeneratorManager {

    private final JavaPlugin plugin;
    private final GeneratorConfig config;
    private final IslandManager islandManager;
    private final Economy economy;
    private final GeneratorMachineTracker machineTracker;
    private final Connection db;

    // Cache: islandId -> (GeneratorType -> level)
    private final Map<String, Map<GeneratorType, Integer>> cache = new HashMap<>();

    // Opcjonalny — ustawiany po inicjalizacji QuestManagera
    private QuestManager questManager;

    public GeneratorManager(JavaPlugin plugin, GeneratorConfig config,
                            IslandManager islandManager, Economy economy, Connection db) {
        this.plugin         = plugin;
        this.config         = config;
        this.islandManager  = islandManager;
        this.economy        = economy;
        this.db             = db;
        this.machineTracker = new GeneratorMachineTracker(plugin, this);
        initTable();
    }

    // -------------------------------------------------------------------------
    // DB
    // -------------------------------------------------------------------------

    private void initTable() {
        try (Statement stmt = db.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS island_generators (
                    island_id      TEXT NOT NULL,
                    generator_type TEXT NOT NULL,
                    level          INTEGER DEFAULT 1,
                    PRIMARY KEY (island_id, generator_type)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS generator_machines (
                    world          TEXT NOT NULL,
                    x              INTEGER NOT NULL,
                    y              INTEGER NOT NULL,
                    z              INTEGER NOT NULL,
                    generator_type TEXT NOT NULL,
                    PRIMARY KEY (world, x, y, z)
                )
            """);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Generator] Błąd tworzenia tabeli", e);
        }
    }

    public void saveMachine(Location loc, GeneratorType type) {
        try (PreparedStatement ps = db.prepareStatement("""
                INSERT OR REPLACE INTO generator_machines (world, x, y, z, generator_type)
                VALUES (?, ?, ?, ?, ?)
            """)) {
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.setString(5, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Generator] Błąd zapisu maszyny", e);
        }
    }

    public void deleteMachine(Location loc) {
        try (PreparedStatement ps = db.prepareStatement(
                "DELETE FROM generator_machines WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Generator] Błąd usuwania maszyny", e);
        }
    }

    /**
     * Ładuje wszystkie maszyny z bazy i rejestruje je w trackerze.
     * Wywołać po załadowaniu światów i wysp.
     */
    public void loadAllMachines() {
        try (Statement stmt = db.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT world, x, y, z, generator_type FROM generator_machines")) {

            List<Location> stale = new ArrayList<>();

            while (rs.next()) {
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                String typeName = rs.getString("generator_type");

                World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("[Generator] Świat '" + worldName + "' nie istnieje — pomijam maszynę.");
                    continue;
                }

                GeneratorType type;
                try {
                    type = GeneratorType.valueOf(typeName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[Generator] Nieznany typ maszyny '" + typeName + "' — pomijam.");
                    continue;
                }

                GeneratorDefinition def = config.getDefinition(type);
                if (def == null) continue;

                Location loc = new Location(world, x, y, z);

                // Sprawdź czy blok maszyny nadal istnieje
                if (loc.getBlock().getType() != def.getMachineBlock()) {
                    stale.add(loc);
                    plugin.getLogger().info("[Generator] Maszyna na " + x + "," + y + "," + z + " zniknęła — usuwam z bazy.");
                    continue;
                }

                Island island = islandManager.getIslandAt(loc);
                if (island == null) continue;

                String islandId = island.getOwner().toString();
                machineTracker.register(loc, type, def, () -> getLevel(islandId, type));
            }

            // Usuń nieaktualne wpisy
            for (Location loc : stale) {
                deleteMachine(loc);
            }

            plugin.getLogger().info("[Generator] Załadowano " + machineTracker.getMachineCount() + " maszyn(y) z bazy.");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Generator] Błąd ładowania maszyn", e);
        }
    }

    public void loadIsland(String islandId) {
        Map<GeneratorType, Integer> levels = new EnumMap<>(GeneratorType.class);
        for (GeneratorType type : GeneratorType.values()) levels.put(type, 1); // defaults

        try (PreparedStatement ps = db.prepareStatement(
                "SELECT generator_type, level FROM island_generators WHERE island_id = ?")) {
            ps.setString(1, islandId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    GeneratorType type = GeneratorType.valueOf(rs.getString("generator_type"));
                    levels.put(type, rs.getInt("level"));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Generator] Błąd ładowania wyspy " + islandId, e);
        }

        cache.put(islandId, levels);
    }

    private void saveLevel(String islandId, GeneratorType type, int level) {
        try (PreparedStatement ps = db.prepareStatement("""
                INSERT INTO island_generators (island_id, generator_type, level)
                VALUES (?, ?, ?)
                ON CONFLICT(island_id, generator_type) DO UPDATE SET level = excluded.level
            """)) {
            ps.setString(1, islandId);
            ps.setString(2, type.name());
            ps.setInt(3, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Generator] Błąd zapisu poziomu", e);
        }
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    public int getLevel(String islandId, GeneratorType type) {
        if (!cache.containsKey(islandId)) {
            loadIsland(islandId);
        }
        Map<GeneratorType, Integer> levels = cache.get(islandId);
        if (levels == null) return 1;
        return levels.getOrDefault(type, 1);
    }

    public GeneratorLevel getCurrentLevel(String islandId, GeneratorType type) {
        GeneratorDefinition def = config.getDefinition(type);
        if (def == null) return null;
        return def.getLevel(getLevel(islandId, type));
    }

    /**
     * Próba ulepszenia generatora. Zwraca true jeśli się udało.
     */
    public boolean upgrade(Player player, Island island, GeneratorType type) {
        GeneratorDefinition def = config.getDefinition(type);
        if (def == null) {
            player.sendMessage("§cBrak konfiguracji dla tego generatora.");
            return false;
        }

        String islandId  = island.getOwner().toString();
        int currentLevel = getLevel(islandId, type);

        if (currentLevel >= def.getMaxLevel()) {
            player.sendMessage("§cGenerator jest już na maksymalnym poziomie!");
            return false;
        }

        GeneratorLevel nextLevel = def.getLevel(currentLevel + 1);
        double cost = nextLevel.getCost();

        if (!economy.has(player, cost)) {
            player.sendMessage("§cNie masz wystarczająco środków! Potrzebujesz §e"
                    + stark.skyBlockTest2.util.PriceFormat.format(cost)
                    + "§c, masz §e" + stark.skyBlockTest2.util.PriceFormat.format(economy.getBalance(player)) + "§c.");
            return false;
        }

        economy.withdrawPlayer(player, cost);
        int newLevel = currentLevel + 1;
        cache.get(islandId).put(type, newLevel);
        saveLevel(islandId, type, newLevel);

        player.sendMessage(String.format("§aGenerator ulepszony do §e%s§a!", nextLevel.getName()));

        if (questManager != null) {
            questManager.addProgress(player, QuestTrigger.UPGRADE_GENERATOR, type.name(), 1);
        }

        return true;
    }

    /**
     * Usuwa wszystkie dane generatorów wyspy (przy kasowaniu wyspy).
     * Czyści cache, zatrzymuje maszyny i kasuje wpisy z bazy.
     */
    public void deleteIslandData(Island island) {
        String islandId = island.getOwner().toString();

        // Usuń z cache
        cache.remove(islandId);

        // Zatrzymaj aktywne taski maszyn dla tej wyspy
        machineTracker.unregisterAllInIsland(island);

        // Usuń poziomy generatorów z bazy
        try (PreparedStatement ps = db.prepareStatement(
                "DELETE FROM island_generators WHERE island_id = ?")) {
            ps.setString(1, islandId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Generator] Błąd usuwania danych wyspy " + islandId, e);
        }

        plugin.getLogger().info("[Generator] Usunięto dane generatorów wyspy " + islandId);
    }

    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void reload() {
        config.reload(plugin);
    }

    public GeneratorConfig getConfig()               { return config; }
    public GeneratorMachineTracker getMachineTracker() { return machineTracker; }

    public void shutdown() {
        machineTracker.unregisterAll();
    }
}