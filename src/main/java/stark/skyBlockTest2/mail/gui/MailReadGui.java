package stark.skyBlockTest2.mail.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.mail.MailManager;
import stark.skyBlockTest2.mail.MailMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Buduje i otwiera WRITTEN_BOOK z treścią wiadomości.
 *
 * Przepływ:
 *   MailGui (klik) → MailActionGui.open() [oznacza jako przeczytane]
 *   MailActionGui (przycisk "Czytaj") → MailReadGui.openBook()
 *
 * openBook() NIE próbuje wykryć zamknięcia książki — to pakiet kliencki
 * bez server-side inventory. Po przeczytaniu gracz naciska Esc/E i wraca
 * do normalnego HUD. Akcje są dostępne przez /poczta → kliknięcie wiadomości.
 */
public class MailReadGui {

    private static final int CHARS_PER_LINE = 38;
    private static final int LINES_PER_PAGE = 13;

    private final SkyBlockTest2 plugin;
    private final MailManager   mailManager;

    public MailReadGui(SkyBlockTest2 plugin, MailManager mailManager) {
        this.plugin      = plugin;
        this.mailManager = mailManager;
    }

    /** Otwiera książkę z treścią wiadomości. Wywołaj z MailActionGui. */
    public void openBook(Player player, String messageId) {
        Optional<MailMessage> opt = mailManager.findMessage(player.getUniqueId(), messageId);
        if (opt.isEmpty()) {
            player.sendMessage("§cNie znaleziono wiadomości.");
            return;
        }
        MailMessage msg = opt.get();
        player.openBook(buildBook(msg));
        player.sendMessage("§8Zamknij książkę (Esc) i wpisz §e/poczta §8aby wrócić do opcji wiadomości.");
    }

    // =========================================================================
    // Budowanie książki
    // =========================================================================

    private ItemStack buildBook(MailMessage msg) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta  = (BookMeta) book.getItemMeta();
        if (meta == null) return book;

        meta.setTitle(msg.getSubject());
        meta.setAuthor(msg.getSenderName());

        List<Component> pages = new ArrayList<>();

        // Strona 1 — nagłówek
        String header =
                "§8§m──────────────\n" +
                        "§6§l" + truncate(msg.getSubject(), 22) + "\n" +
                        "§8§m──────────────\n" +
                        "§8Od: §7" + msg.getSenderName() + "\n" +
                        "§8Data: §7" + msg.getFormattedDate() + "\n";

        if (msg.hasItem() && !msg.isClaimed())  header += "\n§2[Zawiera przedmiot]";
        if (msg.hasMoney() && !msg.isClaimed()) header += "\n§6[Zawiera " + mailManager.formatMoney(msg.getMoney()) + "]";
        if (msg.hasRewards())                   header += "\n\n§c§lODBIERZ W MENU!";

        pages.add(legacy(header));

        // Strony z treścią
        List<String> lines    = wrapText(msg.getMessage());
        List<String> pageLines = new ArrayList<>();
        for (String line : lines) {
            pageLines.add(line);
            if (pageLines.size() >= LINES_PER_PAGE) {
                pages.add(legacy(String.join("\n", pageLines)));
                pageLines.clear();
            }
        }
        if (!pageLines.isEmpty()) pages.add(legacy(String.join("\n", pageLines)));

        meta.pages(pages);
        book.setItemMeta(meta);
        return book;
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    private Component legacy(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private List<String> wrapText(String text) {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (line.length() + word.length() + 1 > CHARS_PER_LINE && !line.isEmpty()) {
                    result.add(line.toString().trim());
                    line = new StringBuilder();
                }
                line.append(word).append(" ");
            }
            if (!line.isEmpty()) result.add(line.toString().trim());
        }
        return result;
    }
}