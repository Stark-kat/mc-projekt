package stark.skyBlockTest2.mail.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.menu.MenuHolder;
import stark.skyBlockTest2.mail.MailManager;
import stark.skyBlockTest2.util.ChatInputManager;

import java.util.*;

/**
 * GUI tworzenia wiadomości.
 *
 * Pola temat/odbiorca — chat input (krótkie, jednoliniowe).
 * Pole treści — writable book: gracz pisze w interfejsie książki,
 *               po kliknięciu "Done" BookEditListener zapisuje tekst i wraca do GUI.
 */
public class MailComposeGui {

    // =========================================================================
    // Stan compose per gracz
    // =========================================================================

    public static class ComposeState {
        public String    recipientName = null;
        public UUID      recipientUuid = null;
        public String    subject       = null;
        public String    message       = null;  // null = nie wpisano
        public ItemStack item          = null;
        public double    money         = 0;
        public String    replyToId     = null;
    }

    private final Map<UUID, ComposeState> states = new HashMap<>();

    private final SkyBlockTest2    plugin;
    private final MailManager      mailManager;
    private final ChatInputManager chatInput;
    private final MailGui          mailGui;

    public MailComposeGui(SkyBlockTest2 plugin, MailManager mailManager,
                          ChatInputManager chatInput, MailGui mailGui) {
        this.plugin      = plugin;
        this.mailManager = mailManager;
        this.chatInput   = chatInput;
        this.mailGui     = mailGui;
    }

    // =========================================================================
    // Otwieranie
    // =========================================================================

    public void open(Player player) {
        states.put(player.getUniqueId(), new ComposeState());
        render(player);
    }

    public void openReply(Player player, String replyToId, UUID recipientUuid, String recipientName) {
        ComposeState state = new ComposeState();
        state.recipientUuid = recipientUuid;
        state.recipientName = recipientName;
        state.replyToId     = replyToId;
        states.put(player.getUniqueId(), state);
        render(player);
    }

    // =========================================================================
    // Akcje z GUI
    // =========================================================================

    public void promptRecipient(Player player) {
        player.closeInventory();
        chatInput.request(player,
                "§6§lPoczta §8» §7Podaj nazwę gracza:",
                input -> {
                    Player target = Bukkit.getPlayerExact(input);
                    if (target == null) {
                        @SuppressWarnings("deprecation")
                        org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(input);
                        if (!off.hasPlayedBefore()) {
                            player.sendMessage("§cNie znaleziono gracza §f" + input + "§c!");
                            render(player);
                            return;
                        }
                        getState(player).recipientUuid = off.getUniqueId();
                        getState(player).recipientName = off.getName() != null ? off.getName() : input;
                    } else {
                        getState(player).recipientUuid = target.getUniqueId();
                        getState(player).recipientName = target.getName();
                    }
                    render(player);
                },
                () -> render(player));
    }

    public void promptSubject(Player player) {
        player.closeInventory();
        chatInput.request(player,
                "§6§lPoczta §8» §7Wpisz temat (max 40 znaków):",
                input -> {
                    getState(player).subject = input.length() > 40 ? input.substring(0, 40) : input;
                    render(player);
                },
                () -> render(player));
    }

    public void promptMessage(Player player) {
        player.closeInventory();
        chatInput.request(player,
                "§6§lPoczta §8» §7Wpisz treść wiadomości:",
                input -> {
                    getState(player).message = input;
                    render(player);
                },
                () -> render(player));
    }

    public void promptMoney(Player player) {
        player.closeInventory();
        chatInput.request(player,
                "§6§lPoczta §8» §7Podaj kwotę do wysłania (§e0§7 = brak):",
                input -> {
                    try {
                        double amount = Double.parseDouble(input);
                        if (amount < 0) {
                            player.sendMessage("§cKwota nie może być ujemna!");
                            render(player);
                            return;
                        }
                        // Zwróć poprzednią kasę
                        ComposeState state = getState(player);
                        if (state.money > 0) plugin.getEconomyManager().deposit(player, state.money);

                        if (amount > 0 && !plugin.getEconomyManager().has(player, amount)) {
                            player.sendMessage("§cNie masz §e" + mailManager.formatMoney(amount) + "§c!");
                            render(player);
                            return;
                        }
                        if (amount > 0) plugin.getEconomyManager().withdraw(player, amount);
                        state.money = amount;
                        render(player);
                    } catch (NumberFormatException ex) {
                        player.sendMessage("§cPodaj poprawną liczbę!");
                        render(player);
                    }
                },
                () -> render(player));
    }

    public void toggleItem(Player player) {
        ComposeState state = getState(player);
        if (state.item != null) {
            player.getInventory().addItem(state.item)
                    .forEach((slot, overflow) ->
                            player.getWorld().dropItemNaturally(player.getLocation(), overflow));
            state.item = null;
        } else {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                player.sendMessage("§cTrzymaj przedmiot który chcesz dodać!");
                render(player);
                return;
            }
            state.item = hand.clone();
            hand.setAmount(0);
        }
        render(player);
    }

    public void send(Player player) {
        ComposeState state = getState(player);

        if (state.recipientUuid == null)         { player.sendMessage("§cWybierz odbiorcę!"); return; }
        if (state.recipientUuid.equals(player.getUniqueId())) { player.sendMessage("§cNie możesz pisać do siebie!"); return; }
        if (state.subject == null || state.subject.isBlank()) { player.sendMessage("§cWpisz temat!"); return; }
        if (state.message == null || state.message.isBlank()) { player.sendMessage("§cWpisz treść!"); return; }

        String err;
        if (state.replyToId != null) {
            err = mailManager.reply(player, state.replyToId, state.recipientUuid,
                    state.message, state.item, state.money);
        } else {
            err = mailManager.send(player.getUniqueId(), player.getName(), state.recipientUuid,
                    state.subject, state.message, state.item, state.money);
        }

        if (err != null) {
            // Zwróć kasę/item przy błędzie
            if (state.money > 0) plugin.getEconomyManager().deposit(player, state.money);
            if (state.item != null) player.getInventory().addItem(state.item);
            player.sendMessage(err);
            return;
        }

        states.remove(player.getUniqueId());
        player.sendMessage("§a§l✉ §r§7Wiadomość wysłana do §f" + state.recipientName + "§7!");
        player.closeInventory();
        mailGui.open(player, 0);
    }

    public void cancel(Player player) {
        ComposeState state = states.remove(player.getUniqueId());
        if (state != null) {
            if (state.item != null)
                player.getInventory().addItem(state.item)
                        .forEach((slot, o) -> player.getWorld().dropItemNaturally(player.getLocation(), o));
            if (state.money > 0)
                plugin.getEconomyManager().deposit(player, state.money);
        }
        player.closeInventory();
        mailGui.open(player, 0);
    }

    // =========================================================================
    // Render GUI
    // =========================================================================

    private void render(Player player) {
        ComposeState state = getState(player);

        String title = state.replyToId != null ? "§6✉ Odpowiedź" : "§6✉ Nowa wiadomość";
        Inventory inv = Bukkit.createInventory(new MenuHolder(), 54, Component.text(title));
        GuiBuilder builder = new GuiBuilder(inv);

        boolean hasRecipient = state.recipientUuid != null;
        boolean hasSubject   = state.subject != null;
        boolean hasMessage   = state.message != null && !state.message.isBlank();

        // Odbiorca — slot 10
        builder.set(10, new ItemBuilder(hasRecipient ? Material.LIME_DYE : Material.RED_DYE)
                .name(hasRecipient ? "§7Do: §f" + state.recipientName : "§c§lBrak odbiorcy")
                .lore(state.replyToId != null
                        ? new String[]{"§8(Odpowiedź — odbiorca zablokowany)"}
                        : new String[]{"§eKliknij §7— " + (hasRecipient ? "Zmień" : "Wybierz")})
                .glow(hasRecipient)
                .setString("action", state.replyToId != null ? "MailComposeNoop" : "MailComposeRecipient")
                .build());

        // Temat — slot 12
        builder.set(12, new ItemBuilder(hasSubject ? Material.LIME_DYE : Material.RED_DYE)
                .name(hasSubject ? "§7Temat: §f" + state.subject : "§c§lBrak tematu")
                .lore("§eKliknij §7— " + (hasSubject ? "Zmień" : "Wpisz"))
                .glow(hasSubject)
                .setString("action", "MailComposeSubject")
                .build());

        // Treść — slot 14 — otwiera book and quill
        builder.set(14, new ItemBuilder(hasMessage ? Material.WRITTEN_BOOK : Material.WRITABLE_BOOK)
                .name(hasMessage ? "§7Treść: §a§lWpisana ✔" : "§c§lBrak treści")
                .lore(
                        hasMessage ? "§8" + preview(state.message) : "§7Napisz treść w książce.",
                        "§eKliknij §7— " + (hasMessage ? "Edytuj" : "Napisz")
                )
                .glow(hasMessage)
                .setString("action", "MailComposeMessage")
                .build());

        // Przedmiot — slot 20
        buildItemSlot(builder, state, 20);

        // Pieniądze — slot 22
        builder.set(22, new ItemBuilder(state.money > 0 ? Material.GOLD_INGOT : Material.GOLD_NUGGET)
                .name(state.money > 0
                        ? "§7Pieniądze: §e" + mailManager.formatMoney(state.money)
                        : "§7Pieniądze: §8Brak")
                .lore("§eKliknij §7— " + (state.money > 0 ? "Zmień" : "Dodaj"))
                .setString("action", "MailComposeMoney")
                .build());

        // Wyślij — slot 25
        boolean canSend = hasRecipient && hasSubject && hasMessage;
        builder.set(25, new ItemBuilder(canSend ? Material.EMERALD : Material.REDSTONE)
                .name(canSend ? "§a§l✉ Wyślij" : "§8Wyślij §7(uzupełnij brakujące pola)")
                .glow(canSend)
                .setString("action", canSend ? "MailComposeSend" : "MailComposeNoop")
                .build());

        // Anuluj — slot 49
        builder.set(49, new ItemBuilder(Material.BARRIER)
                .name("§c§lAnuluj")
                .lore("§7Odzyska przedmiot i pieniądze.")
                .setString("action", "MailComposeCancel")
                .build());

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        player.openInventory(inv);
    }

    // =========================================================================
    // Slot z itemem — PDC bezpośrednio (brak ItemBuilder(ItemStack))
    // =========================================================================

    private void buildItemSlot(GuiBuilder builder, ComposeState state, int slot) {
        ItemStack display = state.item != null ? state.item.clone() : new ItemStack(Material.CHEST);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(state.item != null
                    ? "§7Przedmiot: §f" + state.item.getType().name().toLowerCase().replace("_", " ")
                    : "§7Przedmiot: §8Brak");
            meta.setLore(state.item != null
                    ? List.of("§cKliknij §7— Usuń i odzyskaj")
                    : List.of("§7Trzymaj item w ręce.", "§eKliknij §7— Dodaj z ręki"));
            meta.getPersistentDataContainer()
                    .set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "MailComposeItem");
            display.setItemMeta(meta);
        }
        builder.set(slot, display);
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    public ComposeState getState(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), k -> new ComposeState());
    }

    private String preview(String text) {
        if (text == null) return "";
        String flat = text.replace("\n", " ");
        return flat.length() > 32 ? flat.substring(0, 29) + "..." : flat;
    }
}