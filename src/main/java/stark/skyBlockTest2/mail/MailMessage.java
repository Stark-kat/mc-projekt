package stark.skyBlockTest2.mail;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Pojedyncza wiadomość w skrzynce gracza.
 *
 * senderUuid == null  →  wiadomość systemowa (aukcje, serwer)
 * item == null        →  brak załącznika przedmiotowego
 * money == 0          →  brak załącznika pieniężnego
 * replyToId == null   →  nowa wiadomość (nie odpowiedź)
 */
public class MailMessage {

    private final String    id;
    private final UUID      senderUuid;   // null = system
    private final String    senderName;
    private final UUID      recipientUuid;
    private final String    subject;
    private final String    message;
    private final ItemStack item;         // nullable
    private final double    money;        // 0 = brak
    private final long      sentAt;
    private final String    replyToId;    // nullable
    private boolean         read;
    private boolean         claimed;      // czy item/money zostały odebrane

    public MailMessage(String id, UUID senderUuid, String senderName, UUID recipientUuid,
                       String subject, String message, ItemStack item, double money,
                       long sentAt, String replyToId) {
        this.id            = id;
        this.senderUuid    = senderUuid;
        this.senderName    = senderName;
        this.recipientUuid = recipientUuid;
        this.subject       = subject;
        this.message       = message;
        this.item          = item != null ? item.clone() : null;
        this.money         = money;
        this.sentAt        = sentAt;
        this.replyToId     = replyToId;
        this.read          = false;
        this.claimed       = false;
    }

    public boolean hasItem()      { return item != null; }
    public boolean hasMoney()     { return money > 0; }
    public boolean hasRewards()   { return (hasItem() || hasMoney()) && !claimed; }
    public boolean isSystem()     { return senderUuid == null; }

    // Gettery
    public String    getId()            { return id; }
    public UUID      getSenderUuid()    { return senderUuid; }
    public String    getSenderName()    { return senderName; }
    public UUID      getRecipientUuid() { return recipientUuid; }
    public String    getSubject()       { return subject; }
    public String    getMessage()       { return message; }
    public ItemStack getItem()          { return item != null ? item.clone() : null; }
    public double    getMoney()         { return money; }
    public long      getSentAt()        { return sentAt; }
    public String    getReplyToId()     { return replyToId; }
    public boolean   isRead()           { return read; }
    public boolean   isClaimed()        { return claimed; }

    public void markRead()    { this.read    = true; }
    public void markClaimed() { this.claimed = true; }

    /** Czytelna data "DD.MM HH:mm" */
    public String getFormattedDate() {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(sentAt);
        java.time.LocalDateTime dt = java.time.LocalDateTime.ofInstant(
                instant, java.time.ZoneId.systemDefault());
        return String.format("%02d.%02d %02d:%02d",
                dt.getDayOfMonth(), dt.getMonthValue(),
                dt.getHour(), dt.getMinute());
    }
}