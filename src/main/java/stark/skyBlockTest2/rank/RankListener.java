package stark.skyBlockTest2.rank;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class RankListener implements Listener {

    private final RankManager rankManager;

    public RankListener(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    // -------------------------------------------------------------------------
    // Login / logout
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        rankManager.onPlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        rankManager.onPlayerQuit(event.getPlayer().getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Chat prefix
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Rank rank = rankManager.getRank(event.getPlayer());

        // Pobierz treść wiadomości jako plain text
        String message = LegacyComponentSerializer.legacySection()
                .serialize(event.message());

        // Zbuduj sformatowaną wiadomość
        String formatted = rank.formatChatMessage(
                event.getPlayer().getName(),
                message
        );

        // Ustaw nowy renderer — zastępuje domyślny format Papieru
        event.renderer((source, sourceDisplayName, msg, viewer) ->
                LegacyComponentSerializer.legacySection().deserialize(formatted)
        );
    }
}