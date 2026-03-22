package stark.skyBlockTest2.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.crate.CrateRarity;
import stark.skyBlockTest2.crate.CrateReward;
import stark.skyBlockTest2.island.*;
import stark.skyBlockTest2.mail.MailMessage;
import stark.skyBlockTest2.quest.IslandQuestData;
import stark.skyBlockTest2.settings.PlayerSettings;
import stark.skyBlockTest2.shop.auction.AuctionListing;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DatabaseManager {

    private final SkyBlockTest2 plugin;
    private Connection connection;
    private final Gson gson = new Gson();

    // Jeden wątek dla wszystkich operacji zapisu — brak race condition
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SkyBlock-DB");
        t.setDaemon(true);
        return t;
    });

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    public DatabaseManager(SkyBlockTest2 plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "skyblock.db");
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // WAL mode — lepsza wydajność przy równoczesnych odczytach
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }

            createTables();
            plugin.getLogger().info("[DB] Połączono z bazą danych SQLite.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd połączenia z bazą danych!", e);
        }
    }

    public void disconnect() {
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("[DB] Zamknięto połączenie z bazą danych.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd zamykania bazy danych!", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // Wyspy
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS islands (
                    owner_uuid   TEXT NOT NULL,
                    type         TEXT NOT NULL,
                    world        TEXT NOT NULL,
                    x REAL, y REAL, z REAL,
                    home_x REAL, home_y REAL, home_z REAL,
                    home_yaw REAL, home_pitch REAL,
                    size         INTEGER DEFAULT 0,
                    xp           INTEGER DEFAULT 0,
                    island_index INTEGER,
                    PRIMARY KEY (owner_uuid, type)
                )
            """);

            // Członkowie
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS island_members (
                    owner_uuid  TEXT NOT NULL,
                    member_uuid TEXT NOT NULL,
                    role        TEXT DEFAULT 'MEMBER',
                    PRIMARY KEY (owner_uuid, member_uuid)
                )
            """);

            // Ustawienia gości (per akcja)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS island_settings (
                    owner_uuid TEXT NOT NULL,
                    action     TEXT NOT NULL,
                    value      INTEGER DEFAULT 0,
                    PRIMARY KEY (owner_uuid, action)
                )
            """);

            // Zbanowani gracze
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS island_banned (
                    owner_uuid  TEXT NOT NULL,
                    banned_uuid TEXT NOT NULL,
                    PRIMARY KEY (owner_uuid, banned_uuid)
                )
            """);

            // Indeksy spirali per typ
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS island_meta (
                    type          TEXT PRIMARY KEY,
                    current_index INTEGER DEFAULT 0,
                    free_indexes  TEXT DEFAULT ''
                )
            """);

            // Postęp questów wyspy
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS quest_progress (
                    island_owner         TEXT PRIMARY KEY,
                    last_daily_reset     INTEGER DEFAULT 0,
                    last_weekly_reset    INTEGER DEFAULT 0,
                    completed_daily      TEXT DEFAULT '[]',
                    completed_weekly     TEXT DEFAULT '[]',
                    daily_progress       TEXT DEFAULT '{}',
                    weekly_progress      TEXT DEFAULT '{}',
                    achievement_progress TEXT DEFAULT '{}',
                    achievement_tiers    TEXT DEFAULT '{}'
                )
            """);

            // Aukcje
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auction_listings (
                    id           TEXT PRIMARY KEY,
                    seller_uuid  TEXT NOT NULL,
                    seller_name  TEXT NOT NULL,
                    item_data    TEXT NOT NULL,
                    type         TEXT NOT NULL,
                    price        REAL NOT NULL,
                    current_bid  REAL NOT NULL,
                    expires_at   INTEGER NOT NULL,
                    high_bidder  TEXT,
                    all_bidders  TEXT DEFAULT '[]'
                )
            """);

            // Poczta
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mail_messages (
                    id             TEXT NOT NULL,
                    recipient_uuid TEXT NOT NULL,
                    sender_uuid    TEXT,
                    sender_name    TEXT NOT NULL,
                    subject        TEXT,
                    message        TEXT,
                    item_data      TEXT,
                    money          REAL DEFAULT 0,
                    sent_at        INTEGER NOT NULL,
                    read           INTEGER DEFAULT 0,
                    claimed        INTEGER DEFAULT 0,
                    reply_to_id    TEXT,
                    PRIMARY KEY (id, recipient_uuid)
                )
            """);

            // Nagrody ze skrzynek oczekujące na oddanie (np. po restarcie serwera)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pending_crate_rewards (
                    player_uuid TEXT PRIMARY KEY,
                    item_data   TEXT NOT NULL,
                    rarity      TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_settings (
                    uuid                   TEXT PRIMARY KEY,
                    border_visible         INTEGER DEFAULT 1,
                    particles_enabled      INTEGER DEFAULT 1,
                    accept_tpa             INTEGER DEFAULT 1,
                    accept_msg             INTEGER DEFAULT 1,
                    sidebar_enabled        INTEGER DEFAULT 1,
                    sidebar_island_level   INTEGER DEFAULT 1,
                    sidebar_island_xp      INTEGER DEFAULT 1,
                    sidebar_balance        INTEGER DEFAULT 1,
                    sidebar_daily_quests   INTEGER DEFAULT 1,
                    sidebar_weekly_quests  INTEGER DEFAULT 1,
                    sidebar_island_members INTEGER DEFAULT 1,
                    xp_bossbar             INTEGER DEFAULT 1
                )
            """);

            // Migracja istniejących baz — ignorujemy błąd jeśli kolumna już istnieje
            for (String col : new String[]{
                    "sidebar_enabled       INTEGER DEFAULT 1",
                    "sidebar_island_level  INTEGER DEFAULT 1",
                    "sidebar_island_xp     INTEGER DEFAULT 1",
                    "sidebar_balance       INTEGER DEFAULT 1",
                    "sidebar_daily_quests   INTEGER DEFAULT 1",
                    "sidebar_weekly_quests  INTEGER DEFAULT 1",
                    "sidebar_island_members INTEGER DEFAULT 1",
                    "xp_bossbar             INTEGER DEFAULT 1",
                    "direct_drop            INTEGER DEFAULT 0"
            }) {
                try { stmt.execute("ALTER TABLE player_settings ADD COLUMN " + col); }
                catch (java.sql.SQLException ignored) {}
            }

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_balances (
                    uuid    TEXT PRIMARY KEY,
                    balance REAL NOT NULL DEFAULT 0.0
                )
            """);
        }
    }

    // =========================================================================
    // EKONOMIA
    // =========================================================================

    public double getBalance(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT balance FROM player_balances WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[DB] Błąd odczytu salda " + uuid, e);
        }
        return 0.0;
    }

    public void saveBalance(UUID uuid, double amount) {
        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO player_balances (uuid, balance) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setDouble(2, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[DB] Błąd zapisu salda " + uuid, e);
            }
        });
    }

    // =========================================================================
    // USTAWIENIA GRACZA
    // =========================================================================

    public PlayerSettings loadPlayerSettings(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT border_visible, particles_enabled, accept_tpa, accept_msg,
                       sidebar_enabled, sidebar_island_level, sidebar_island_xp,
                       sidebar_balance, sidebar_daily_quests, sidebar_weekly_quests,
                       sidebar_island_members, xp_bossbar, direct_drop
                FROM player_settings WHERE uuid = ?""")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayerSettings s = new PlayerSettings();
                    s.setBorderVisible(rs.getInt("border_visible") == 1);
                    s.setParticlesEnabled(rs.getInt("particles_enabled") == 1);
                    s.setAcceptTpa(rs.getInt("accept_tpa") == 1);
                    s.setAcceptMsg(rs.getInt("accept_msg") == 1);
                    s.setSidebarEnabled(rs.getInt("sidebar_enabled") == 1);
                    s.setSidebarIslandLevel(rs.getInt("sidebar_island_level") == 1);
                    s.setSidebarIslandXp(rs.getInt("sidebar_island_xp") == 1);
                    s.setSidebarBalance(rs.getInt("sidebar_balance") == 1);
                    s.setSidebarDailyQuests(rs.getInt("sidebar_daily_quests") == 1);
                    s.setSidebarWeeklyQuests(rs.getInt("sidebar_weekly_quests") == 1);
                    s.setSidebarIslandMembers(rs.getInt("sidebar_island_members") == 1);
                    s.setXpBossbarEnabled(rs.getInt("xp_bossbar") == 1);
                    s.setDirectDrop(rs.getInt("direct_drop") == 1);
                    return s;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[DB] Błąd ładowania ustawień gracza " + uuid, e);
        }
        return new PlayerSettings();
    }

    public void savePlayerSettings(UUID uuid, PlayerSettings settings) {
        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT OR REPLACE INTO player_settings
                    (uuid, border_visible, particles_enabled, accept_tpa, accept_msg,
                     sidebar_enabled, sidebar_island_level, sidebar_island_xp,
                     sidebar_balance, sidebar_daily_quests, sidebar_weekly_quests,
                     sidebar_island_members, xp_bossbar, direct_drop)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2,  settings.isBorderVisible()        ? 1 : 0);
                ps.setInt(3,  settings.isParticlesEnabled()     ? 1 : 0);
                ps.setInt(4,  settings.isAcceptTpa()            ? 1 : 0);
                ps.setInt(5,  settings.isAcceptMsg()            ? 1 : 0);
                ps.setInt(6,  settings.isSidebarEnabled()       ? 1 : 0);
                ps.setInt(7,  settings.isSidebarIslandLevel()   ? 1 : 0);
                ps.setInt(8,  settings.isSidebarIslandXp()      ? 1 : 0);
                ps.setInt(9,  settings.isSidebarBalance()       ? 1 : 0);
                ps.setInt(10, settings.isSidebarDailyQuests()   ? 1 : 0);
                ps.setInt(11, settings.isSidebarWeeklyQuests()  ? 1 : 0);
                ps.setInt(12, settings.isSidebarIslandMembers() ? 1 : 0);
                ps.setInt(13, settings.isXpBossbarEnabled()     ? 1 : 0);
                ps.setInt(14, settings.isDirectDrop()           ? 1 : 0);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[DB] Błąd zapisu ustawień gracza " + uuid, e);
            }
        });
    }

    // =========================================================================
    // WYSPY — odpowiednik IslandStorage
    // =========================================================================

    public void saveIsland(Island island, IslandType type) {
        executor.submit(() -> {
            try {
                String ownerStr = island.getOwner().toString();

                // Upsert wyspy
                try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO islands
                        (owner_uuid, type, world, x, y, z,
                         home_x, home_y, home_z, home_yaw, home_pitch,
                         size, xp, island_index)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(owner_uuid, type) DO UPDATE SET
                        world=excluded.world,
                        x=excluded.x, y=excluded.y, z=excluded.z,
                        home_x=excluded.home_x, home_y=excluded.home_y, home_z=excluded.home_z,
                        home_yaw=excluded.home_yaw, home_pitch=excluded.home_pitch,
                        size=excluded.size, xp=excluded.xp,
                        island_index=excluded.island_index
                """)) {
                    ps.setString(1, ownerStr);
                    ps.setString(2, type.name());
                    ps.setString(3, island.getCenter().getWorld().getName());
                    ps.setDouble(4, island.getCenter().getX());
                    ps.setDouble(5, island.getCenter().getY());
                    ps.setDouble(6, island.getCenter().getZ());
                    ps.setDouble(7, island.getHome().getX());
                    ps.setDouble(8, island.getHome().getY());
                    ps.setDouble(9, island.getHome().getZ());
                    ps.setFloat(10, island.getHome().getYaw());
                    ps.setFloat(11, island.getHome().getPitch());
                    ps.setInt(12, island.getSize());
                    ps.setLong(13, island.getXp());
                    ps.setInt(14, island.getIndex());
                    ps.executeUpdate();
                }

                // Ustawienia gości i relacje — tylko OVERWORLD
                if (type == IslandType.OVERWORLD) {
                    // Ustawienia
                    try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO island_settings (owner_uuid, action, value)
                        VALUES (?,?,?)
                        ON CONFLICT(owner_uuid, action) DO UPDATE SET value=excluded.value
                    """)) {
                        for (IslandAction action : IslandAction.values()) {
                            ps.setString(1, ownerStr);
                            ps.setString(2, action.name());
                            ps.setInt(3, island.canVisitorDo(action) ? 1 : 0);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    // Członkowie — usuń stare, wstaw nowe
                    try (PreparedStatement del = connection.prepareStatement(
                            "DELETE FROM island_members WHERE owner_uuid=?")) {
                        del.setString(1, ownerStr);
                        del.executeUpdate();
                    }
                    if (!island.getMembers().isEmpty()) {
                        try (PreparedStatement ps = connection.prepareStatement(
                                "INSERT INTO island_members (owner_uuid, member_uuid, role) VALUES (?,?,?)")) {
                            for (UUID memberUuid : island.getMembers()) {
                                IslandRole role = island.getRole(memberUuid);
                                ps.setString(1, ownerStr);
                                ps.setString(2, memberUuid.toString());
                                ps.setString(3, role != null ? role.name() : IslandRole.MEMBER.name());
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }

                    // Bany — usuń stare, wstaw nowe
                    try (PreparedStatement del = connection.prepareStatement(
                            "DELETE FROM island_banned WHERE owner_uuid=?")) {
                        del.setString(1, ownerStr);
                        del.executeUpdate();
                    }
                    if (!island.getBannedPlayers().isEmpty()) {
                        try (PreparedStatement ps = connection.prepareStatement(
                                "INSERT INTO island_banned (owner_uuid, banned_uuid) VALUES (?,?)")) {
                            for (UUID banned : island.getBannedPlayers()) {
                                ps.setString(1, ownerStr);
                                ps.setString(2, banned.toString());
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] Błąd zapisu wyspy " + island.getOwner(), e);
            }
        });
    }

    public void deleteIsland(UUID ownerUuid, IslandType type) {
        executor.submit(() -> {
            try {
                String uuidStr = ownerUuid.toString();
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM islands WHERE owner_uuid=? AND type=?")) {
                    ps.setString(1, uuidStr);
                    ps.setString(2, type.name());
                    ps.executeUpdate();
                }
                if (type == IslandType.OVERWORLD) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM island_members WHERE owner_uuid=?")) {
                        ps.setString(1, uuidStr);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM island_settings WHERE owner_uuid=?")) {
                        ps.setString(1, uuidStr);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM island_banned WHERE owner_uuid=?")) {
                        ps.setString(1, uuidStr);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] Błąd usuwania wyspy " + ownerUuid, e);
            }
        });
    }

    /** Ładowanie wysp przy starcie — synchronicznie. */
    public Map<UUID, Island> loadIslands(IslandType type) {
        Map<UUID, Island> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM islands WHERE type=?")) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("[DB] Brak świata '" + worldName
                                + "' dla wyspy " + ownerUuid + " — pomijam.");
                        continue;
                    }

                    Location center = new Location(world,
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                    Location home = new Location(world,
                            rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"),
                            rs.getFloat("home_yaw"), rs.getFloat("home_pitch"));

                    // Załaduj członków
                    List<UUID> members = loadMembers(ownerUuid);

                    Island island = new Island(ownerUuid, center,
                            rs.getInt("size"), rs.getInt("island_index"), members);
                    island.setHome(home);
                    island.setXp(rs.getLong("xp"));

                    if (type == IslandType.OVERWORLD) {
                        loadSettings(island, ownerUuid);
                        loadBanned(island, ownerUuid);
                        loadRoles(island, ownerUuid);
                    }

                    result.put(ownerUuid, island);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[DB] Błąd ładowania wyspy", e);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd ładowania wysp typu " + type, e);
        }
        return result;
    }

    private List<UUID> loadMembers(UUID ownerUuid) throws SQLException {
        List<UUID> members = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT member_uuid FROM island_members WHERE owner_uuid=?")) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try { members.add(UUID.fromString(rs.getString("member_uuid"))); }
                    catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return members;
    }

    private void loadSettings(Island island, UUID ownerUuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT action, value FROM island_settings WHERE owner_uuid=?")) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        IslandAction action = IslandAction.valueOf(rs.getString("action"));
                        island.setVisitorSetting(action, rs.getInt("value") == 1);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    private void loadBanned(Island island, UUID ownerUuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT banned_uuid FROM island_banned WHERE owner_uuid=?")) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try { island.banPlayer(UUID.fromString(rs.getString("banned_uuid"))); }
                    catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    private void loadRoles(Island island, UUID ownerUuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT member_uuid, role FROM island_members WHERE owner_uuid=?")) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID memberUuid = UUID.fromString(rs.getString("member_uuid"));
                        IslandRole role = IslandRole.valueOf(rs.getString("role"));
                        if (role != IslandRole.MEMBER) {
                            island.setRole(memberUuid, role);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Indeksy spirali
    // -------------------------------------------------------------------------

    public void saveIndexState(IslandType type, int currentIndex, List<Integer> freeIndexes) {
        String freeJson = gson.toJson(freeIndexes);
        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO island_meta (type, current_index, free_indexes)
                VALUES (?,?,?)
                ON CONFLICT(type) DO UPDATE SET
                    current_index=excluded.current_index,
                    free_indexes=excluded.free_indexes
            """)) {
                ps.setString(1, type.name());
                ps.setInt(2, currentIndex);
                ps.setString(3, freeJson);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] Błąd zapisu indeksów dla " + type, e);
            }
        });
    }

    public int getCurrentIndex(IslandType type) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT current_index FROM island_meta WHERE type=?")) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("current_index");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd odczytu current_index dla " + type, e);
        }
        return 0;
    }

    public List<Integer> getFreeIndexes(IslandType type) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT free_indexes FROM island_meta WHERE type=?")) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String json = rs.getString("free_indexes");
                if (json != null && !json.isBlank()) {
                    Type listType = new TypeToken<List<Integer>>() {}.getType();
                    return gson.fromJson(json, listType);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd odczytu free_indexes dla " + type, e);
        }
        return new ArrayList<>();
    }

    // =========================================================================
    // QUESTY — odpowiednik QuestStorage
    // =========================================================================

    public void saveQuestData(UUID islandOwner, IslandQuestData data) {
        Type mapType = new TypeToken<Map<String, Integer>>() {}.getType();
        String completedDailyJson  = gson.toJson(new ArrayList<>(data.getCompletedDailySet()));
        String completedWeeklyJson = gson.toJson(new ArrayList<>(data.getCompletedWeeklySet()));
        String dailyProgressJson   = gson.toJson(data.getDailyProgressMap(), mapType);
        String weeklyProgressJson  = gson.toJson(data.getWeeklyProgressMap(), mapType);
        String achProgressJson     = gson.toJson(data.getAchievementProgressMap(), mapType);
        String achTiersJson        = gson.toJson(data.getAchievementTiersMap(), mapType);

        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO quest_progress
                    (island_owner, last_daily_reset, last_weekly_reset,
                     completed_daily, completed_weekly,
                     daily_progress, weekly_progress,
                     achievement_progress, achievement_tiers)
                VALUES (?,?,?,?,?,?,?,?,?)
                ON CONFLICT(island_owner) DO UPDATE SET
                    last_daily_reset=excluded.last_daily_reset,
                    last_weekly_reset=excluded.last_weekly_reset,
                    completed_daily=excluded.completed_daily,
                    completed_weekly=excluded.completed_weekly,
                    daily_progress=excluded.daily_progress,
                    weekly_progress=excluded.weekly_progress,
                    achievement_progress=excluded.achievement_progress,
                    achievement_tiers=excluded.achievement_tiers
            """)) {
                ps.setString(1, islandOwner.toString());
                ps.setLong(2, data.getLastDailyReset());
                ps.setLong(3, data.getLastWeeklyReset());
                ps.setString(4, completedDailyJson);
                ps.setString(5, completedWeeklyJson);
                ps.setString(6, dailyProgressJson);
                ps.setString(7, weeklyProgressJson);
                ps.setString(8, achProgressJson);
                ps.setString(9, achTiersJson);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] Błąd zapisu questów " + islandOwner, e);
            }
        });
    }

    public IslandQuestData loadQuestData(UUID islandOwner) {
        IslandQuestData data = new IslandQuestData();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM quest_progress WHERE island_owner=?")) {
            ps.setString(1, islandOwner.toString());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return data;

            Type listType = new TypeToken<List<String>>() {}.getType();
            Type mapType  = new TypeToken<Map<String, Integer>>() {}.getType();

            data.setLastDailyReset(rs.getLong("last_daily_reset"));
            data.setLastWeeklyReset(rs.getLong("last_weekly_reset"));

            List<String> completedDaily = gson.fromJson(rs.getString("completed_daily"), listType);
            if (completedDaily != null) data.getCompletedDailySet().addAll(completedDaily);

            List<String> completedWeekly = gson.fromJson(rs.getString("completed_weekly"), listType);
            if (completedWeekly != null) data.getCompletedWeeklySet().addAll(completedWeekly);

            Map<String, Integer> dp = gson.fromJson(rs.getString("daily_progress"), mapType);
            if (dp != null) data.getDailyProgressMap().putAll(dp);

            Map<String, Integer> wp = gson.fromJson(rs.getString("weekly_progress"), mapType);
            if (wp != null) data.getWeeklyProgressMap().putAll(wp);

            Map<String, Integer> ap = gson.fromJson(rs.getString("achievement_progress"), mapType);
            if (ap != null) data.getAchievementProgressMap().putAll(ap);

            Map<String, Integer> at = gson.fromJson(rs.getString("achievement_tiers"), mapType);
            if (at != null) data.getAchievementTiersMap().putAll(at);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd ładowania questów " + islandOwner, e);
        }
        return data;
    }

    public void loadAllQuestData(Map<UUID, IslandQuestData> target) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT island_owner FROM quest_progress")) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("island_owner"));
                    target.put(uuid, loadQuestData(uuid));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd ładowania wszystkich questów", e);
        }
        plugin.getLogger().info("[DB] Załadowano dane questów dla " + target.size() + " wysp.");
    }

    public void deleteQuestData(UUID islandOwner) {
        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM quest_progress WHERE island_owner=?")) {
                ps.setString(1, islandOwner.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] Błąd usuwania questów " + islandOwner, e);
            }
        });
    }

    // =========================================================================
    // AUKCJE — odpowiednik AuctionStorage
    // =========================================================================

    public void saveAuction(AuctionListing listing) {
        String itemBase64 = Base64.getEncoder().encodeToString(listing.getItem().serializeAsBytes());
        String allBidders = gson.toJson(listing.getAllBidders().stream()
                .map(UUID::toString).toList());

        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO auction_listings
                    (id, seller_uuid, seller_name, item_data, type,
                     price, current_bid, expires_at, high_bidder, all_bidders)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET
                    current_bid=excluded.current_bid,
                    high_bidder=excluded.high_bidder,
                    all_bidders=excluded.all_bidders,
                    expires_at=excluded.expires_at
            """)) {
                ps.setString(1, listing.getId());
                ps.setString(2, listing.getSellerUuid().toString());
                ps.setString(3, listing.getSellerName());
                ps.setString(4, itemBase64);
                ps.setString(5, listing.getType().name());
                ps.setDouble(6, listing.getPrice());
                ps.setDouble(7, listing.getCurrentBid());
                ps.setLong(8, listing.getExpiresAt());
                ps.setString(9, listing.getHighBidder() != null
                        ? listing.getHighBidder().toString() : null);
                ps.setString(10, allBidders);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] Błąd zapisu aukcji " + listing.getId(), e);
            }
        });
    }

    public void deleteAuction(String auctionId) {
        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM auction_listings WHERE id=?")) {
                ps.setString(1, auctionId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] Błąd usuwania aukcji " + auctionId, e);
            }
        });
    }

    public List<AuctionListing> loadAllAuctions() {
        List<AuctionListing> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM auction_listings")) {
            while (rs.next()) {
                try {
                    String id         = rs.getString("id");
                    UUID sellerUuid   = UUID.fromString(rs.getString("seller_uuid"));
                    String sellerName = rs.getString("seller_name");
                    AuctionListing.Type type = AuctionListing.Type.valueOf(rs.getString("type"));
                    double price      = rs.getDouble("price");
                    double currentBid = rs.getDouble("current_bid");
                    long expiresAt    = rs.getLong("expires_at");

                    ItemStack item = ItemStack.deserializeBytes(
                            Base64.getDecoder().decode(rs.getString("item_data")));

                    AuctionListing listing = new AuctionListing(
                            id, sellerUuid, sellerName, item, type, price, expiresAt);

                    if (type == AuctionListing.Type.BID && currentBid > price) {
                        String highBidderStr = rs.getString("high_bidder");
                        if (highBidderStr != null) {
                            listing.placeBid(UUID.fromString(highBidderStr), currentBid);
                        }
                    }

                    Type listType = new TypeToken<List<String>>() {}.getType();
                    List<String> bidders = gson.fromJson(rs.getString("all_bidders"), listType);
                    if (bidders != null) {
                        for (String uuidStr : bidders) {
                            try { listing.addPreviousBidder(UUID.fromString(uuidStr)); }
                            catch (IllegalArgumentException ignored) {}
                        }
                    }

                    result.add(listing);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[DB] Błąd ładowania aukcji", e);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd ładowania aukcji", e);
        }
        plugin.getLogger().info("[DB] Załadowano " + result.size() + " aukcji.");
        return result;
    }

    // =========================================================================
    // POCZTA — odpowiednik MailStorage
    // =========================================================================

    public void saveMail(MailMessage msg) {
        String itemData = msg.hasItem()
                ? Base64.getEncoder().encodeToString(msg.getItem().serializeAsBytes())
                : null;

        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO mail_messages
                    (id, recipient_uuid, sender_uuid, sender_name,
                     subject, message, item_data, money,
                     sent_at, read, claimed, reply_to_id)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id, recipient_uuid) DO UPDATE SET
                    read=excluded.read,
                    claimed=excluded.claimed
            """)) {
                ps.setString(1, msg.getId());
                ps.setString(2, msg.getRecipientUuid().toString());
                ps.setString(3, msg.getSenderUuid() != null ? msg.getSenderUuid().toString() : null);
                ps.setString(4, msg.getSenderName());
                ps.setString(5, msg.getSubject());
                ps.setString(6, msg.getMessage());
                ps.setString(7, itemData);
                ps.setDouble(8, msg.getMoney());
                ps.setLong(9, msg.getSentAt());
                ps.setInt(10, msg.isRead() ? 1 : 0);
                ps.setInt(11, msg.isClaimed() ? 1 : 0);
                ps.setString(12, msg.getReplyToId());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] Błąd zapisu wiadomości " + msg.getId(), e);
            }
        });
    }

    public void deleteMail(UUID playerUuid, String messageId) {
        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM mail_messages WHERE id=? AND recipient_uuid=?")) {
                ps.setString(1, messageId);
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] Błąd usuwania wiadomości " + messageId, e);
            }
        });
    }

    public List<MailMessage> loadMail(UUID playerUuid) {
        List<MailMessage> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM mail_messages WHERE recipient_uuid=? ORDER BY sent_at DESC")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    String senderUuidStr = rs.getString("sender_uuid");
                    UUID senderUuid = senderUuidStr != null ? UUID.fromString(senderUuidStr) : null;

                    String itemData = rs.getString("item_data");
                    ItemStack item = itemData != null
                            ? ItemStack.deserializeBytes(Base64.getDecoder().decode(itemData))
                            : null;

                    MailMessage msg = new MailMessage(
                            rs.getString("id"),
                            senderUuid,
                            rs.getString("sender_name"),
                            playerUuid,
                            rs.getString("subject"),
                            rs.getString("message"),
                            item,
                            rs.getDouble("money"),
                            rs.getLong("sent_at"),
                            rs.getString("reply_to_id")
                    );
                    if (rs.getInt("read") == 1)    msg.markRead();
                    if (rs.getInt("claimed") == 1) msg.markClaimed();

                    result.add(msg);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[DB] Błąd ładowania wiadomości", e);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd ładowania poczty " + playerUuid, e);
        }
        return result;
    }

    // =========================================================================
    // OCZEKUJĄCE NAGRODY ZE SKRZYNEK
    // Zapisujemy przy starcie animacji, kasujemy po dostarczeniu nagrody.
    // Przy logowaniu gracza sprawdzamy czy ma coś w tej tabeli.
    // =========================================================================

    /**
     * Zapisuje nagrodę oczekującą na dostarczenie.
     * Wywoływane synchronicznie zaraz przed startem animacji.
     */
    public void savePendingReward(UUID playerUuid, CrateReward reward) {
        String itemData = Base64.getEncoder().encodeToString(reward.getItem().serializeAsBytes());
        String rarity   = reward.getRarity().name();
        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO pending_crate_rewards (player_uuid, item_data, rarity)
                VALUES (?,?,?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    item_data=excluded.item_data,
                    rarity=excluded.rarity
            """)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, itemData);
                ps.setString(3, rarity);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[DB] Błąd zapisu pending reward dla " + playerUuid, e);
            }
        });
    }

    /**
     * Usuwa oczekującą nagrodę po jej dostarczeniu.
     */
    public void deletePendingReward(UUID playerUuid) {
        executor.submit(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM pending_crate_rewards WHERE player_uuid=?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[DB] Błąd usuwania pending reward dla " + playerUuid, e);
            }
        });
    }

    /**
     * Ładuje wszystkie oczekujące nagrody przy starcie serwera.
     * Synchroniczne — wywoływane tylko raz podczas onEnable.
     */
    public Map<UUID, CrateReward> loadAllPendingRewards() {
        Map<UUID, CrateReward> result = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM pending_crate_rewards")) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    ItemStack item = ItemStack.deserializeBytes(
                            Base64.getDecoder().decode(rs.getString("item_data")));
                    CrateRarity rarity = CrateRarity.fromString(rs.getString("rarity"));
                    // weight i displayName nie są potrzebne do dostarczenia nagrody
                    result.put(uuid, new CrateReward(item, rarity, 0, null));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "[DB] Błąd ładowania pending reward", e);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[DB] Błąd ładowania pending rewards", e);
        }
        if (!result.isEmpty()) {
            plugin.getLogger().info("[DB] Znaleziono " + result.size()
                    + " nieoddanych nagród ze skrzynek.");
        }
        return result;
    }

    public CrateReward loadPendingReward(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM pending_crate_rewards WHERE player_uuid=?")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            ItemStack item = ItemStack.deserializeBytes(
                    Base64.getDecoder().decode(rs.getString("item_data")));
            CrateRarity rarity = CrateRarity.fromString(rs.getString("rarity"));
            return new CrateReward(item, rarity, 0, null);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "[DB] Błąd ładowania pending reward dla " + playerUuid, e);
            return null;
        }
    }

    public Connection getConnection() {
        return connection;
    }
}