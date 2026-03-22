package stark.skyBlockTest2.rank;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Główny punkt dostępu do systemu rang.
 * Cache trzyma rangi zalogowanych graczy — baza jest odpytywana tylko przy logowaniu.
 *
 * Użycie z innych części pluginu:
 *   Rank rank = plugin.getRankManager().getRank(player);
 *   if (rank == Rank.VIP) { ... }
 */
public class RankManager {

    private final RankDatabase database;
    private final Map<UUID, Rank> cache = new ConcurrentHashMap<>();

    public RankManager(JavaPlugin plugin) throws SQLException {
        this.database = new RankDatabase(plugin);
    }

    // -------------------------------------------------------------------------
    // Odczyt rangi
    // -------------------------------------------------------------------------

    /**
     * Zwraca aktualną rangę gracza.
     * Dla zalogowanych graczy korzysta z cache (szybkie).
     */
    public Rank getRank(Player player) {
        return cache.getOrDefault(player.getUniqueId(), Rank.PLAYER);
    }

    /** Wersja do użycia gdy gracz jest offline (np. w komendach admina). */
    public Rank getRank(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        RankDatabase.RankEntry entry = database.getRank(uuid);
        return entry != null ? entry.rank() : Rank.PLAYER;
    }

    // -------------------------------------------------------------------------
    // Ustawianie rangi
    // -------------------------------------------------------------------------

    /**
     * Ustawia rangę bezterminowo.
     * @param grantedBy  nick/UUID osoby przyznającej (np. "CONSOLE" lub nick admina)
     */
    public void setRank(UUID uuid, Rank rank, String grantedBy) {
        database.setRank(uuid, rank, grantedBy, null);
        cache.put(uuid, rank);
    }

    /**
     * Ustawia rangę na określoną liczbę dni.
     * @param days  liczba dni — po tym czasie ranga wygasa do PLAYER
     */
    public void setRankForDays(UUID uuid, Rank rank, String grantedBy, int days) {
        long expiresAt = System.currentTimeMillis() + (long) days * 24 * 60 * 60 * 1000;
        database.setRank(uuid, rank, grantedBy, expiresAt);
        cache.put(uuid, rank);
    }

    /**
     * Usuwa rangę gracza (wraca do PLAYER).
     */
    public void removeRank(UUID uuid) {
        database.removeRank(uuid);
        cache.put(uuid, Rank.PLAYER);
    }

    // -------------------------------------------------------------------------
    // Lifecycle — wywoływane przez RankListener
    // -------------------------------------------------------------------------

    /** Wywołaj przy logowaniu gracza. Ładuje rangę do cache, sprawdza wygaśnięcie. */
    public void onPlayerJoin(Player player) {
        RankDatabase.RankEntry entry = database.getRank(player.getUniqueId());

        if (entry == null) {
            // Nowy gracz — brak rekordu = domyślny PLAYER
            cache.put(player.getUniqueId(), Rank.PLAYER);
            return;
        }

        if (entry.isExpired()) {
            // Ranga wygasła — usuń z bazy i ustaw PLAYER
            database.removeRank(player.getUniqueId());
            cache.put(player.getUniqueId(), Rank.PLAYER);
            player.sendMessage("§7Twoja ranga §6" + entry.rank().getDisplayName() + "§7 wygasła.");
        } else {
            cache.put(player.getUniqueId(), entry.rank());
        }
    }

    /** Wywołaj przy wylogowaniu gracza. Zwalnia cache. */
    public void onPlayerQuit(UUID uuid) {
        cache.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Info o wpisie (do komendy /rank info)
    // -------------------------------------------------------------------------

    public RankDatabase.RankEntry getRankEntry(UUID uuid) {
        return database.getRank(uuid);
    }
}