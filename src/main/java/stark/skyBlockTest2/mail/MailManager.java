package stark.skyBlockTest2.mail;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.database.DatabaseManager;
import stark.skyBlockTest2.economy.EconomyManager;

import java.util.*;

public class MailManager {

    private final EconomyManager economy;
    private final DatabaseManager db;

    // Cache skrzynek zalogowanych graczy
    private final Map<UUID, List<MailMessage>> inboxCache = new HashMap<>();

    private final int maxMessages;

    public MailManager(SkyBlockTest2 plugin, EconomyManager economy) {
        this.economy     = economy;
        this.db     = plugin.getDatabaseManager();
        this.maxMessages = plugin.getConfig().getInt("mail.max-messages", 50);
    }

    // =========================================================================
    // Wysyłanie
    // =========================================================================

    /**
     * Wysyła wiadomość. Zwraca null jeśli sukces, lub komunikat błędu.
     */
    public String send(UUID senderUuid, String senderName, UUID recipientUuid,
                       String subject, String message, ItemStack item, double money) {

        List<MailMessage> inbox = getInbox(recipientUuid);
        if (inbox.size() >= maxMessages)
            return "§cSkrzynka odbiorcy jest pełna! (" + maxMessages + " wiadomości)";

        String id = UUID.randomUUID().toString();
        MailMessage msg = new MailMessage(id, senderUuid, senderName, recipientUuid,
                subject, message, item, money, System.currentTimeMillis(), null);

        inbox.add(0, msg); // dodaj na początek (najnowsze pierwsze)
        db.saveMail(msg);

        // Powiadom gracza jeśli online
        Player recipient = Bukkit.getPlayer(recipientUuid);
        if (recipient != null) {
            recipient.sendMessage("§6§l✉ §r§7Masz nową wiadomość od §f" + senderName
                    + "§7! §8(/poczta)");
        }

        return null;
    }

    /**
     * Skrót dla wiadomości systemowych (aukcje, serwer).
     */
    public void sendSystem(UUID recipientUuid, String subject, String message,
                           ItemStack item, double money) {
        send(null, "§8[System]", recipientUuid, subject, message, item, money);
    }

    /**
     * Odpowiedź na wiadomość.
     */
    public String reply(Player sender, String replyToId, UUID recipientUuid,
                        String message, ItemStack item, double money) {
        List<MailMessage> senderInbox = getInbox(sender.getUniqueId());
        if (senderInbox.size() >= maxMessages)
            return "§cTwoja skrzynka jest pełna!";

        List<MailMessage> recipientInbox = getInbox(recipientUuid);
        if (recipientInbox.size() >= maxMessages)
            return "§cSkrzynka odbiorcy jest pełna!";

        // Ustal temat z prefiksem Re:
        String originalSubject = findMessage(recipientUuid, replyToId)
                .map(MailMessage::getSubject)
                .orElse("Wiadomość");
        String subject = originalSubject.startsWith("Re: ")
                ? originalSubject : "Re: " + originalSubject;

        String id = UUID.randomUUID().toString();
        MailMessage msg = new MailMessage(id, sender.getUniqueId(), sender.getName(),
                recipientUuid, subject, message, item, money,
                System.currentTimeMillis(), replyToId);

        recipientInbox.add(0, msg);
        db.saveMail(msg);

        Player recipient = Bukkit.getPlayer(recipientUuid);
        if (recipient != null) {
            recipient.sendMessage("§6§l✉ §r§7Odpowiedź od §f" + sender.getName()
                    + "§7! §8(/poczta)");
        }

        return null;
    }

    // =========================================================================
    // Odbiór nagród
    // =========================================================================

    /**
     * Odbiera item i/lub pieniądze z wiadomości. Zwraca null jeśli sukces.
     */
    public String claimRewards(Player player, String messageId) {
        Optional<MailMessage> opt = findMessage(player.getUniqueId(), messageId);
        if (opt.isEmpty()) return "§cNie znaleziono wiadomości!";

        MailMessage msg = opt.get();
        if (!msg.hasRewards()) return "§cBrak nagród do odebrania!";

        if (msg.hasItem()) {
            player.getInventory().addItem(msg.getItem())
                    .forEach((slot, overflow) ->
                            player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        }
        if (msg.hasMoney()) {
            economy.deposit(player, msg.getMoney());
        }

        msg.markClaimed();
        db.saveMail(msg);

        return null;
    }

    // =========================================================================
    // Zarządzanie skrzynką
    // =========================================================================

    public void markRead(UUID playerUuid, String messageId) {
        findMessage(playerUuid, messageId).ifPresent(msg -> {
            if (!msg.isRead()) {
                msg.markRead();
                db.saveMail(msg);
            }
        });
    }

    public String deleteMessage(Player player, String messageId) {
        List<MailMessage> inbox = getInbox(player.getUniqueId());
        Optional<MailMessage> opt = inbox.stream()
                .filter(m -> m.getId().equals(messageId)).findFirst();

        if (opt.isEmpty()) return "§cNie znaleziono wiadomości!";
        MailMessage msg = opt.get();

        // Jeśli są nieodebrane nagrody — zwróć przed usunięciem
        if (msg.hasRewards()) {
            String err = claimRewards(player, messageId);
            if (err != null) return err;
        }

        inbox.remove(msg);
        db.deleteMail(player.getUniqueId(), messageId);
        return null;
    }

    // =========================================================================
    // Cache
    // =========================================================================

    /** Ładuje skrzynkę gracza do cache (wywołaj przy logowaniu). */
    public void loadInbox(UUID uuid) {
        inboxCache.put(uuid, db.loadMail(uuid));
    }

    /** Usuwa skrzynkę z cache (wywołaj przy wylogowaniu). */
    public void unloadInbox(UUID uuid) {
        inboxCache.remove(uuid);
    }

    public List<MailMessage> getInbox(UUID uuid) {
        return inboxCache.computeIfAbsent(uuid, db::loadMail);
    }

    public int getUnreadCount(UUID uuid) {
        return (int) getInbox(uuid).stream().filter(m -> !m.isRead()).count();
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    public Optional<MailMessage> findMessage(UUID playerUuid, String messageId) {
        return getInbox(playerUuid).stream()
                .filter(m -> m.getId().equals(messageId))
                .findFirst();
    }

    public DatabaseManager getDb() { return db; }
    public int         getMaxMessages() { return maxMessages; }

    public String formatMoney(double amount) {
        return stark.skyBlockTest2.util.PriceFormat.format(amount);
    }
}