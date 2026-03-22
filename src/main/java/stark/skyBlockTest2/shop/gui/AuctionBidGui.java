package stark.skyBlockTest2.shop.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
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

public class AuctionBidGui {

    private final AuctionManager         auctionManager;
    private final AuctionGui             auctionGui;
    private final Map<UUID, BidState>    states = new HashMap<>();

    public AuctionBidGui(AuctionManager auctionManager, AuctionGui auctionGui) {
        this.auctionManager = auctionManager;
        this.auctionGui     = auctionGui;
    }

    public void open(Player player, String listingId) {
        AuctionListing listing = auctionManager.getListing(listingId);
        if (listing == null || listing.isExpired()) {
            player.sendMessage("§cTa aukcja już nie istnieje!");
            return;
        }
        if (listing.getType() != AuctionListing.Type.BID) {
            player.sendMessage("§cTo nie jest aukcja licytacyjna!");
            return;
        }
        if (listing.getSellerUuid().equals(player.getUniqueId())) {
            player.sendMessage("§cNie możesz licytować własnego przedmiotu!");
            return;
        }
        if (listing.isLeading(player.getUniqueId())) {
            player.sendMessage("§aJuż prowadzisz w tej licytacji!");
            return;
        }

        BidState state = states.computeIfAbsent(player.getUniqueId(), k -> new BidState());
        state.listingId = listingId;
        // Ustaw startową ofertę na minimalną wymaganą
        state.bidAmount = auctionManager.getMinBid(listing);

        renderGui(player, state, listing);
    }

    public void openWithState(Player player) {
        BidState state = states.get(player.getUniqueId());
        if (state == null) return;
        AuctionListing listing = auctionManager.getListing(state.listingId);
        if (listing == null || listing.isExpired()) {
            player.sendMessage("§cAukcja wygasła!");
            auctionGui.open(player, 0);
            return;
        }
        renderGui(player, state, listing);
    }

    private void renderGui(Player player, BidState state, AuctionListing listing) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(), 27,
                Component.text("§9Złóż ofertę"));

        GuiBuilder builder = new GuiBuilder(inv);

        double minBid    = auctionManager.getMinBid(listing);
        double balance   = auctionManager.getCommissionPercent(); // tylko do info
        boolean canAfford = auctionManager.getListing(state.listingId) != null; // zawsze true tutaj

        // Podgląd aukcji
        builder.set(4, buildListingPreview(listing));

        // - przyciski
        builder.set(9, new ItemBuilder(Material.RED_DYE)
                .name("§c-1000")
                .setString("action", "BidAdjust")
                .setString("bid_listing_id", state.listingId)
                .setString("adjust", "-1000")
                .build());
        builder.set(10, new ItemBuilder(Material.RED_DYE)
                .name("§c-100")
                .setString("action", "BidAdjust")
                .setString("bid_listing_id", state.listingId)
                .setString("adjust", "-100")
                .build());
        builder.set(11, new ItemBuilder(Material.RED_DYE)
                .name("§c-10")
                .setString("action", "BidAdjust")
                .setString("bid_listing_id", state.listingId)
                .setString("adjust", "-10")
                .build());

        // Aktualna kwota oferty
        List<String> bidLore = new ArrayList<>();
        bidLore.add(" ");
        bidLore.add("§7Aktualna oferta: §e" + auctionManager.formatPrice(listing.getCurrentBid()));
        bidLore.add("§7Minimalna oferta: §a" + auctionManager.formatPrice(minBid));
        bidLore.add(" ");
        boolean valid = state.bidAmount >= minBid;
        bidLore.add(valid ? "§a✔ Oferta prawidłowa" : "§c✘ Oferta za niska!");

        builder.set(13, new ItemBuilder(valid ? Material.GOLD_NUGGET : Material.BARRIER)
                .name("§eTwoja oferta: §f" + auctionManager.formatPrice(state.bidAmount))
                .lore(bidLore.toArray(new String[0]))
                .build());

        // + przyciski
        builder.set(15, new ItemBuilder(Material.LIME_DYE)
                .name("§a+10")
                .setString("action", "BidAdjust")
                .setString("bid_listing_id", state.listingId)
                .setString("adjust", "+10")
                .build());
        builder.set(16, new ItemBuilder(Material.LIME_DYE)
                .name("§a+100")
                .setString("action", "BidAdjust")
                .setString("bid_listing_id", state.listingId)
                .setString("adjust", "+100")
                .build());
        builder.set(17, new ItemBuilder(Material.LIME_DYE)
                .name("§a+1000")
                .setString("action", "BidAdjust")
                .setString("bid_listing_id", state.listingId)
                .setString("adjust", "+1000")
                .build());

        // Potwierdź
        builder.set(22, new ItemBuilder(valid ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                .name(valid ? "§a§lZłóż ofertę!" : "§cZa niska kwota")
                .lore(
                        "§7Oferta: §e" + auctionManager.formatPrice(state.bidAmount),
                        "§7Pozostało: §f" + listing.getRemainingFormatted()
                )
                .setString("action", "BidConfirm")
                .setString("bid_listing_id", state.listingId)
                .build());

        // Powrót
        builder.set(18, new ItemBuilder(Material.ARROW)
                .name("§7Powrót")
                .setString("action", "OpenAuctions")
                .build());

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        player.openInventory(inv);
    }

    private org.bukkit.inventory.ItemStack buildListingPreview(AuctionListing listing) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Sprzedawca: §f" + listing.getSellerName());
        lore.add("§7Cena wywoławcza: §e" + auctionManager.formatPrice(listing.getPrice()));
        lore.add("§7Aktualna oferta: §e" + auctionManager.formatPrice(listing.getCurrentBid())
                + (listing.hasBid() ? "" : " §8(brak ofert)"));
        lore.add("§7Pozostało: §f" + listing.getRemainingFormatted());

        org.bukkit.inventory.ItemStack display = listing.getItem();
        org.bukkit.inventory.meta.ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> combined = new ArrayList<>();
            if (meta.hasLore()) { combined.addAll(meta.getLore()); combined.add(" "); }
            combined.addAll(lore);
            meta.setLore(combined);
            display.setItemMeta(meta);
        }
        return display;
    }

    // =========================================================================
    // Obsługa akcji z GuiListener
    // =========================================================================

    public void adjustBid(Player player, String listingId, String adjustStr) {
        BidState state = states.get(player.getUniqueId());
        if (state == null || !state.listingId.equals(listingId)) return;

        AuctionListing listing = auctionManager.getListing(listingId);
        if (listing == null) return;

        try {
            double delta = Double.parseDouble(adjustStr);
            double minBid = auctionManager.getMinBid(listing);
            state.bidAmount = Math.max(minBid, state.bidAmount + delta);
        } catch (NumberFormatException ignored) {}

        openWithState(player);
    }

    public void confirmBid(Player player, String listingId) {
        BidState state = states.get(player.getUniqueId());
        if (state == null || !state.listingId.equals(listingId)) {
            player.sendMessage("§cBłąd stanu oferty!");
            return;
        }

        String error = auctionManager.placeBid(player, listingId, state.bidAmount);
        if (error != null) {
            player.sendMessage(error);
            return;
        }

        states.remove(player.getUniqueId());
        auctionGui.open(player, 0);
    }

    private static class BidState {
        String listingId  = null;
        double bidAmount  = 0;
    }
}