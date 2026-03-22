package stark.skyBlockTest2.mail.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.menu.MenuHolder;
import stark.skyBlockTest2.mail.MailManager;
import stark.skyBlockTest2.mail.MailMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MailGui {

    private static final int   PAGE_SIZE     = 36;
    private static final int[] CONTENT_SLOTS;

    static {
        CONTENT_SLOTS = new int[PAGE_SIZE];
        for (int i = 0; i < PAGE_SIZE; i++) CONTENT_SLOTS[i] = i;
    }

    private final MailManager mailManager;

    public MailGui(MailManager mailManager) {
        this.mailManager = mailManager;
    }

    public void open(Player player, int page) {
        UUID uuid = player.getUniqueId();
        List<MailMessage> inbox = mailManager.getInbox(uuid);

        int totalPages = Math.max(1, (int) Math.ceil((double) inbox.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int unread = mailManager.getUnreadCount(uuid);
        String title = "§6✉ Poczta"
                + (unread > 0 ? " §c(" + unread + " nieprzeczytanych)" : "")
                + (totalPages > 1 ? " §8(" + (page + 1) + "/" + totalPages + ")" : "");

        Inventory inv = Bukkit.createInventory(new MenuHolder(), 54, Component.text(title));
        GuiBuilder builder = new GuiBuilder(inv);

        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, inbox.size());

        for (int i = start; i < end; i++) {
            builder.set(CONTENT_SLOTS[i - start], buildMessageItem(inbox.get(i)));
        }

        // Rząd 5 — nawigacja
        if (page > 0) {
            builder.set(45, new ItemBuilder(Material.ARROW)
                    .name("§7« Poprzednia strona")
                    .setString("action", "MailPage")
                    .setString("page", String.valueOf(page - 1))
                    .build());
        }
        if (page < totalPages - 1) {
            builder.set(53, new ItemBuilder(Material.ARROW)
                    .name("§7Następna strona »")
                    .setString("action", "MailPage")
                    .setString("page", String.valueOf(page + 1))
                    .build());
        }

        builder.set(49, new ItemBuilder(Material.FEATHER)
                .name("§a§lNowa wiadomość")
                .lore("§7Napisz do innego gracza.")
                .setString("action", "MailCompose")
                .build());

        builder.set(48, new ItemBuilder(Material.ARROW)
                .name("§7Cofnij")
                .setString("action", "MenuGui")
                .build());

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        player.openInventory(inv);
    }

    // =========================================================================
    // Item wiadomości na liście
    // =========================================================================

    private ItemStack buildMessageItem(MailMessage msg) {
        boolean hasRewards = msg.hasRewards();
        boolean unread     = !msg.isRead();

        Material icon;
        if (hasRewards && msg.hasItem() && msg.hasMoney()) icon = Material.BUNDLE;
        else if (hasRewards && msg.hasItem())              icon = Material.CHEST_MINECART;
        else if (hasRewards && msg.hasMoney())             icon = Material.GOLD_NUGGET;
        else if (unread)                                   icon = Material.PAPER;
        else                                               icon = Material.MAP;

        List<String> lore = new ArrayList<>();
        lore.add("§7Od: §f" + msg.getSenderName());
        lore.add("§7Data: §8" + msg.getFormattedDate());
        lore.add(" ");

        // Podgląd treści
        String preview = msg.getMessage();
        if (preview.length() > 40) preview = preview.substring(0, 37) + "...";
        lore.add("§7" + preview);

        if (hasRewards) {
            lore.add(" ");
            lore.add("§e§l⚠ Zawiera nagrody do odebrania!");
            if (msg.hasItem())  lore.add("§7• Przedmiot: §f" + msg.getItem().getType().name()
                    .toLowerCase().replace("_", " "));
            if (msg.hasMoney()) lore.add("§7• Pieniądze: §e" + mailManager.formatMoney(msg.getMoney()));
        }

        lore.add(" ");
        lore.add("§eKliknij §7— Czytaj");

        String nameColor = unread ? "§f§l" : "§7";

        return new ItemBuilder(icon)
                .name(nameColor + msg.getSubject())
                .lore(lore.toArray(new String[0]))
                .glow(hasRewards)
                .setString("action", "MailRead")
                .setString("mail_id", msg.getId())
                .build();
    }
}