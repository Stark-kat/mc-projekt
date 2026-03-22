package stark.skyBlockTest2.shop.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.menu.MenuHolder;
import stark.skyBlockTest2.shop.auction.AuctionListing;
import stark.skyBlockTest2.shop.auction.AuctionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionCreateGui {

    private final AuctionManager         auctionManager;
    private final Map<UUID, CreateState> states = new HashMap<>();

    public AuctionCreateGui(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    public void open(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType().isAir()) {
            player.sendMessage("§cTrzymaj przedmiot w ręce który chcesz wystawić!");
            return;
        }

        CreateState state = states.computeIfAbsent(player.getUniqueId(), k -> new CreateState());
        state.item = handItem.clone();

        renderGui(player, state);
    }

    public void openWithState(Player player) {
        CreateState state = states.get(player.getUniqueId());
        if (state == null || state.item == null) { open(player); return; }
        renderGui(player, state);
    }

    private void renderGui(Player player, CreateState state) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(), 54,
                Component.text("§6Wystaw na aukcję"));

        GuiBuilder builder = new GuiBuilder(inv);

        // Podgląd przedmiotu (tylko wyświetlenie)
        builder.set(13, state.item.clone());

        // Wybór typu
        boolean isFixed = state.type == AuctionListing.Type.FIXED;

        builder.set(28, new ItemBuilder(Material.EMERALD)
                .name(isFixed ? "§a§l✔ Stała cena" : "§7Stała cena")
                .lore("§7Gracz kupuje od razu po Twojej cenie.")
                .glow(isFixed)
                .setString("action", "AuctionSetType")
                .setString("auction_type", "FIXED")
                .build());

        builder.set(30, new ItemBuilder(Material.GOLD_INGOT)
                .name(!isFixed ? "§6§l✔ Licytacja" : "§7Licytacja")
                .lore("§7Gracze licytują — wygrywa najwyższa oferta.")
                .glow(!isFixed)
                .setString("action", "AuctionSetType")
                .setString("auction_type", "BID")
                .build());

        // Czas trwania
        for (int i = 0; i < AuctionManager.DURATION_HOURS.length; i++) {
            int hours   = AuctionManager.DURATION_HOURS[i];
            boolean sel = state.durationHours == hours;
            builder.set(32 + i, new ItemBuilder(sel ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name(sel ? "§a§l✔ " + hours + "h" : "§7" + hours + "h")
                    .glow(sel)
                    .setString("action", "AuctionSetDuration")
                    .setString("auction_duration", String.valueOf(hours))
                    .build());
        }

        // Regulacja ceny
        builder.set(37, new ItemBuilder(Material.RED_DYE)
                .name("§c- Cena")
                .lore("§7LPM §8-100  §7| §7PPM §8-10  §7| §7Shift §8-1")
                .setString("action", "AuctionAdjustPrice")
                .setString("adjust", "minus")
                .build());

        double receives = state.price * (1.0 - auctionManager.getCommissionPercent() / 100.0);
        builder.set(40, new ItemBuilder(Material.NAME_TAG)
                .name("§eCena: §f" + auctionManager.formatPrice(state.price))
                .lore(
                        "§8Prowizja serwera: §c" + (int) auctionManager.getCommissionPercent() + "%",
                        "§8Otrzymasz: §f" + auctionManager.formatPrice(receives)
                )
                .setString("action", "AuctionTypePrice")
                .build());

        builder.set(43, new ItemBuilder(Material.LIME_DYE)
                .name("§a+ Cena")
                .lore("§7LPM §8+100  §7| §7PPM §8+10  §7| §7Shift §8+1")
                .setString("action", "AuctionAdjustPrice")
                .setString("adjust", "plus")
                .build());

        // Wystaw
        builder.set(49, new ItemBuilder(Material.EMERALD_BLOCK)
                .name("§a§lWystaw aukcję!")
                .lore(buildConfirmLore(state).toArray(new String[0]))
                .setString("action", "AuctionConfirm")
                .build());

        // Anuluj
        builder.set(45, new ItemBuilder(Material.BARRIER)
                .name("§7Anuluj")
                .setString("action", "OpenAuctions")
                .build());

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        player.openInventory(inv);
    }

    // =========================================================================
    // Obsługa akcji z GuiListener
    // =========================================================================

    public void setType(Player player, AuctionListing.Type type) {
        getState(player).type = type;
        openWithState(player);
    }

    public void setDuration(Player player, int hours) {
        getState(player).durationHours = hours;
        openWithState(player);
    }

    public void adjustPrice(Player player, String direction, boolean shift, boolean rightClick) {
        CreateState state = getState(player);
        double delta;
        if (shift)           delta = 1;
        else if (rightClick) delta = 10;
        else                 delta = 100;

        if (direction.equals("minus")) delta = -delta;
        state.price = Math.max(1, state.price + delta);
        openWithState(player);
    }

    public void confirm(Player player) {
        CreateState state = states.get(player.getUniqueId());
        if (state == null || state.item == null) {
            player.sendMessage("§cBłąd: brak danych aukcji!");
            return;
        }
        if (state.price <= 0) {
            player.sendMessage("§cCena musi być większa od 0!");
            return;
        }

        String error = auctionManager.createListing(
                player, state.item, state.type, state.price, state.durationHours);

        if (error != null) {
            player.sendMessage(error);
            return;
        }

        states.remove(player.getUniqueId());
        player.closeInventory();
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    private List<String> buildConfirmLore(CreateState state) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Przedmiot: §f" + getItemName(state.item));
        lore.add("§7Typ: §f" + (state.type == AuctionListing.Type.FIXED ? "Stała cena" : "Licytacja"));
        lore.add("§7Cena: §e" + auctionManager.formatPrice(state.price));
        lore.add("§7Czas: §f" + state.durationHours + "h");
        lore.add("§7Prowizja: §c" + (int) auctionManager.getCommissionPercent() + "%");
        lore.add("§7Otrzymasz: §a" + auctionManager.formatPrice(
                state.price * (1.0 - auctionManager.getCommissionPercent() / 100.0)));
        return lore;
    }

    private CreateState getState(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), k -> new CreateState());
    }

    private String getItemName(ItemStack item) {
        if (item == null) return "?";
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        String name = item.getType().name().replace("_", " ").toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static class CreateState {
        ItemStack           item          = null;
        AuctionListing.Type type          = AuctionListing.Type.FIXED;
        double              price         = 100;
        int                 durationHours = 24;
    }
}