package stark.skyBlockTest2.mail.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.menu.MenuHolder;
import stark.skyBlockTest2.mail.MailManager;
import stark.skyBlockTest2.mail.MailMessage;

import java.util.Optional;

/**
 * Małe GUI (27 slotów) wyświetlane po zamknięciu książki z wiadomością.
 * Zawiera przyciski: Odbierz / Odpowiedz / Usuń / Powrót.
 */
public class MailActionGui {

    private final MailManager mailManager;

    public MailActionGui(MailManager mailManager) {
        this.mailManager = mailManager;
    }

    public void open(Player player, String messageId) {
        Optional<MailMessage> opt = mailManager.findMessage(player.getUniqueId(), messageId);
        if (opt.isEmpty()) { player.sendMessage("§cWiadomość nie istnieje."); return; }

        MailMessage msg = opt.get();

        Inventory inv = Bukkit.createInventory(new MenuHolder(), 27,
                Component.text("§6✉ " + truncate(msg.getSubject(), 28)));
        GuiBuilder builder = new GuiBuilder(inv);

        // Odbierz nagrody — widoczny tylko jeśli są
        if (msg.hasRewards()) {
            String rewardLabel = buildRewardLabel(msg);
            builder.set(10, new ItemBuilder(Material.CHEST)
                    .name("§a§lOdbierz nagrody")
                    .lore(rewardLabel, " ", "§eKliknij §7— Odbierz")
                    .glow(true)
                    .setString("action", "MailClaim")
                    .setString("mail_id", messageId)
                    .build());
        }

        // Czytaj ponownie
        builder.set(11, new ItemBuilder(Material.WRITTEN_BOOK)
                .name("§7Czytaj ponownie")
                .setString("action", "MailReadBook")
                .setString("mail_id", messageId)
                .build());

        // Odpowiedz — tylko dla wiadomości od gracza
        if (!msg.isSystem()) {
            builder.set(13, new ItemBuilder(Material.FEATHER)
                    .name("§a§lOdpowiedz")
                    .lore("§7Do: §f" + msg.getSenderName())
                    .setString("action", "MailReply")
                    .setString("mail_id", messageId)
                    .setString("reply_to_uuid", msg.getSenderUuid().toString())
                    .build());
        }

        // Usuń
        builder.set(15, new ItemBuilder(Material.TNT)
                .name("§c§lUsuń wiadomość")
                .lore(msg.hasRewards() && !msg.isClaimed()
                        ? "§7Nagrody zostaną automatycznie odebrane."
                        : "§7Tej operacji nie można cofnąć.")
                .setString("action", "MailDelete")
                .setString("mail_id", messageId)
                .build());

        // Powrót do skrzynki
        builder.set(16, new ItemBuilder(Material.ARROW)
                .name("§7Powrót do skrzynki")
                .setString("action", "MailBack")
                .build());

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        player.openInventory(inv);
    }

    private String buildRewardLabel(MailMessage msg) {
        StringBuilder sb = new StringBuilder();
        if (msg.hasItem())  sb.append("§7Przedmiot: §f")
                .append(msg.getItem().getType().name().toLowerCase().replace("_", " "));
        if (msg.hasItem() && msg.hasMoney()) sb.append("\n");
        if (msg.hasMoney()) sb.append("§7Pieniądze: §e")
                .append(mailManager.formatMoney(msg.getMoney()));
        return sb.toString();
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}