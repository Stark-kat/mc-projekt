package stark.skyBlockTest2.rank;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Warstwa danych rang — korzysta z tej samej bazy SQLite co reszta pluginu.
 * Dodaje tabelę player_ranks do istniejącego pliku database.db.
 */
public class RankDatabase {

    private final Connection connection;
    private final Logger log;

    public RankDatabase(JavaPlugin plugin) throws SQLException {
        // Ta sama baza co IslandDatabase — zmień ścieżkę jeśli masz inną nazwę pliku
        String path = plugin.getDataFolder().getAbsolutePath() + "/database.db";
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        this.log = plugin.getLogger();
        createTable();
    }

    // -------------------------------------------------------------------------
    // Schema
    // -------------------------------------------------------------------------

    private void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS player_ranks (
                    uuid       TEXT    NOT NULL PRIMARY KEY,
                    rank       TEXT    NOT NULL DEFAULT 'PLAYER',
                    granted_by TEXT,
                    granted_at INTEGER NOT NULL,
                    expires_at INTEGER             -- NULL = bezterminowo
                );
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // -------------------------------------------------------------------------
    // Odczyt
    // -------------------------------------------------------------------------

    /** Zwraca wpis rangi lub null jeśli gracz nie ma rekordu (= domyślny PLAYER). */
    public RankEntry getRank(UUID uuid) {
        String sql = "SELECT rank, granted_by, granted_at, expires_at FROM player_ranks WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Rank rank       = Rank.fromString(rs.getString("rank"));
                String grantedBy = rs.getString("granted_by");
                long grantedAt   = rs.getLong("granted_at");
                long expiresAt   = rs.getLong("expires_at");
                boolean hasExpiry = !rs.wasNull();
                return new RankEntry(uuid, rank, grantedBy, grantedAt, hasExpiry ? expiresAt : null);
            }
        } catch (SQLException e) {
            log.severe("[RankDatabase] Błąd getRank: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Zapis / aktualizacja
    // -------------------------------------------------------------------------

    /**
     * Ustawia rangę gracza.
     * @param expiresAt  timestamp w ms — null oznacza bezterminowo
     */
    public void setRank(UUID uuid, Rank rank, String grantedBy, Long expiresAt) {
        String sql = """
                INSERT INTO player_ranks (uuid, rank, granted_by, granted_at, expires_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    rank       = excluded.rank,
                    granted_by = excluded.granted_by,
                    granted_at = excluded.granted_at,
                    expires_at = excluded.expires_at;
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, rank.name());
            ps.setString(3, grantedBy);
            ps.setLong(4, System.currentTimeMillis());
            if (expiresAt != null) ps.setLong(5, expiresAt);
            else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("[RankDatabase] Błąd setRank: " + e.getMessage());
        }
    }

    /** Usuwa rangę gracza (wraca do domyślnego PLAYER). */
    public void removeRank(UUID uuid) {
        String sql = "DELETE FROM player_ranks WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("[RankDatabase] Błąd removeRank: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Pomocnicze DTO
    // -------------------------------------------------------------------------

    public record RankEntry(
            UUID uuid,
            Rank rank,
            String grantedBy,
            long grantedAt,
            Long expiresAt   // null = bezterminowo
    ) {
        public boolean isExpired() {
            return expiresAt != null && System.currentTimeMillis() > expiresAt;
        }
    }
}