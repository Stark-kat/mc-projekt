package stark.skyBlockTest2.util;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import stark.skyBlockTest2.SkyBlockTest2;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Obsługuje wpisywanie wartości przez graczy na czacie.
 * Gracz dostaje prompt, wpisuje wartość, callback jest wywoływany na głównym wątku.
 * Wpisanie "anuluj" przerywa input.
 */
public class ChatInputManager implements Listener {

    private static class PendingInput {
        final Consumer<String> callback;
        final Runnable         onCancel;

        PendingInput(Consumer<String> callback, Runnable onCancel) {
            this.callback = callback;
            this.onCancel = onCancel;
        }
    }

    private final SkyBlockTest2             plugin;
    private final Map<UUID, PendingInput>   pending = new HashMap<>();

    public ChatInputManager(SkyBlockTest2 plugin) {
        this.plugin = plugin;
    }

    /**
     * Prosi gracza o wpisanie wartości na czacie.
     *
     * @param player   gracz
     * @param prompt   wiadomość wyświetlana przed inputem
     * @param callback wywołany na głównym wątku z wpisaną wartością
     * @param onCancel wywołany gdy gracz wpisze "anuluj" (może być null)
     */
    public void request(Player player, String prompt, Consumer<String> callback, Runnable onCancel) {
        pending.put(player.getUniqueId(), new PendingInput(callback, onCancel));
        player.sendMessage(prompt);
        player.sendMessage("§8Wpisz §canuluj §8aby przerwać.");
    }

    public boolean isWaiting(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void cancel(Player player) {
        PendingInput p = pending.remove(player.getUniqueId());
        if (p != null && p.onCancel != null) p.onCancel.run();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (!pending.containsKey(uuid)) return;

        e.setCancelled(true); // nie pokazuj na czacie
        String input = e.getMessage().trim();
        PendingInput p = pending.remove(uuid);

        // Przenieś na główny wątek
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("anuluj")) {
                if (p.onCancel != null) p.onCancel.run();
            } else {
                p.callback.accept(input);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }
}