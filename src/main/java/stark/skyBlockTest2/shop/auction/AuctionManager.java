package stark.skyBlockTest2.shop.auction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.database.DatabaseManager;
import stark.skyBlockTest2.economy.EconomyManager;
import stark.skyBlockTest2.mail.MailManager;
import stark.skyBlockTest2.quest.QuestManager;
import stark.skyBlockTest2.quest.QuestTrigger;

import java.util.*;
import java.util.stream.Collectors;

public class AuctionManager {

    private final SkyBlockTest2  plugin;
    private final EconomyManager economy;
    private final DatabaseManager db;
    private       MailManager    mailManager; // ustawiany po inicjalizacji
    private       QuestManager   questManager; // ustawiany po inicjalizacji

    private final Map<String, AuctionListing> listings = new LinkedHashMap<>();

    private double commissionPercent;
    private int    maxListingsPerPlayer;
    private double minBidIncrementPercent;

    public static final int[] DURATION_HOURS = {24, 48, 72};

    public AuctionManager(SkyBlockTest2 plugin, EconomyManager economy) {
        this.plugin   = plugin;
        this.economy  = economy;
        this.db = plugin.getDatabaseManager();

        commissionPercent      = plugin.getConfig().getDouble("auction.commission-percent", 5.0);
        maxListingsPerPlayer   = plugin.getConfig().getInt("auction.max-listings-per-player", 5);
        minBidIncrementPercent = plugin.getConfig().getDouble("auction.min-bid-increment-percent", 5.0);

        for (AuctionListing l : db.loadAllAuctions()) listings.put(l.getId(), l);

        Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpired, 20L * 60, 20L * 60);
    }

    public void reload() {
        commissionPercent      = plugin.getConfig().getDouble("auction.commission-percent", 5.0);
        maxListingsPerPlayer   = plugin.getConfig().getInt("auction.max-listings-per-player", 5);
        minBidIncrementPercent = plugin.getConfig().getDouble("auction.min-bid-increment-percent", 5.0);
    }

    /** Wywołaj po inicjalizacji MailManager w onEnable */
    public void setMailManager(MailManager mailManager) {
        this.mailManager = mailManager;
    }

    /** Wywołaj po inicjalizacji QuestManager w onEnable */
    public void setQuestManager(QuestManager questManager) {
        this.questManager = questManager;
    }

    // =========================================================================
    // Wystawianie aukcji
    // =========================================================================

    public String createListing(Player seller, ItemStack item,
                                AuctionListing.Type type, double price, int durationHours) {
        if (item == null || item.getType().isAir())
            return "§cNie możesz wystawić pustego przedmiotu!";
        if (price <= 0)
            return "§cCena musi być większa od 0!";

        long count = listings.values().stream()
                .filter(l -> l.getSellerUuid().equals(seller.getUniqueId()) && !l.isExpired())
                .count();
        if (count >= maxListingsPerPlayer)
            return "§cMasz już §e" + maxListingsPerPlayer + " §caktywnych aukcji!";

        long expiresAt = System.currentTimeMillis() + ((long) durationHours * 60 * 60 * 1000);
        String id = UUID.randomUUID().toString();
        AuctionListing listing = new AuctionListing(
                id, seller.getUniqueId(), seller.getName(), item, type, price, expiresAt);

        listings.put(id, listing);
        db.saveAuction(listing);
        removeItemFromPlayer(seller, item);

        seller.sendMessage("§a§lAukcja wystawiona! §r§7Czas: §e" + durationHours + "h");
        return null;
    }

    // =========================================================================
    // Kupowanie (FIXED)
    // =========================================================================

    public String buyNow(Player buyer, String listingId) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null || listing.isExpired())
            return "§cTa aukcja już nie istnieje!";
        if (listing.getType() != AuctionListing.Type.FIXED)
            return "§cTo jest aukcja licytacyjna!";
        if (listing.getSellerUuid().equals(buyer.getUniqueId()))
            return "§cNie możesz kupić własnego przedmiotu!";

        double price = listing.getPrice();
        if (!economy.has(buyer, price))
            return "§cNie masz §e" + formatPrice(price) + "§c!";

        economy.withdraw(buyer, price);
        double sellerGets = price * (1.0 - commissionPercent / 100.0);

        // Przedmiot do kupującego
        buyer.getInventory().addItem(listing.getItem())
                .forEach((slot, overflow) ->
                        buyer.getWorld().dropItemNaturally(buyer.getLocation(), overflow));
        buyer.sendMessage("§aKupiono §f" + getItemName(listing.getItem())
                + " §7za §e" + formatPrice(price) + "§7!");

        // Kasa do sprzedającego — przez pocztę
        sendMoneyViaMail(listing.getSellerUuid(),
                "Sprzedano: " + getItemName(listing.getItem()),
                "Gracz §f" + buyer.getName() + " §7kupił Twój przedmiot z aukcji.",
                null, sellerGets,
                "§8Prowizja: " + (int) commissionPercent + "%");

        listing.markExpired();
        listings.remove(listingId);
        db.deleteAuction(listingId);

        if (questManager != null) {
            questManager.addProgress(listing.getSellerUuid(), QuestTrigger.AUCTION_SOLD, "TRANSACTION", 1);
            questManager.addProgress(listing.getSellerUuid(), QuestTrigger.AUCTION_SOLD,
                    "VALUE", (int) Math.min(price, 1_000_000_000.0));
        }
        return null;
    }

    // =========================================================================
    // Licytacja (BID)
    // =========================================================================

    public String placeBid(Player bidder, String listingId, double amount) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null || listing.isExpired())
            return "§cTa aukcja już nie istnieje!";
        if (listing.getType() != AuctionListing.Type.BID)
            return "§cTo jest aukcja z stałą ceną!";
        if (listing.getSellerUuid().equals(bidder.getUniqueId()))
            return "§cNie możesz licytować własnego przedmiotu!";
        if (listing.isLeading(bidder.getUniqueId()))
            return "§cJuż prowadzisz w tej licytacji!";

        double minBid = getMinBid(listing);
        if (amount < minBid)
            return "§cMinimalna oferta to §e" + formatPrice(minBid)
                    + " §8(+" + (int) minBidIncrementPercent + "%)";

        if (!economy.has(bidder, amount))
            return "§cNie masz §e" + formatPrice(amount) + "§c!";

        economy.withdraw(bidder, amount);

        // Zwróć kasę poprzedniemu liderowi
        double previousBid = listing.getCurrentBid();
        UUID previousBidder = listing.placeBid(bidder.getUniqueId(), amount);

        if (previousBidder != null) {
            // Zwrot przez pocztę — gracz może być offline
            sendMoneyViaMail(previousBidder,
                    "Przebito Twoją ofertę: " + getItemName(listing.getItem()),
                    "Gracz §f" + bidder.getName() + " §7przebił Twoją ofertę.\n"
                            + "Zwrócono §e" + formatPrice(previousBid) + "§7.",
                    null, previousBid, null);

            // Powiadom online
            Player outbid = Bukkit.getPlayer(previousBidder);
            if (outbid != null) outbid.sendMessage("§c§lPrzebito Twoją ofertę! §r§7("
                    + getItemName(listing.getItem()) + ")");
        }

        db.saveAuction(listing);
        bidder.sendMessage("§aZłożono ofertę §e" + formatPrice(amount)
                + " §7na §f" + getItemName(listing.getItem()) + "§7!");
        return null;
    }

    public double getMinBid(AuctionListing listing) {
        double base = listing.hasBid() ? listing.getCurrentBid() : listing.getPrice();
        return Math.ceil(base * (1.0 + minBidIncrementPercent / 100.0));
    }

    // =========================================================================
    // Zdejmowanie aukcji
    // =========================================================================

    public String cancelListing(Player seller, String listingId) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null)
            return "§cNie znaleziono aukcji!";
        if (!listing.getSellerUuid().equals(seller.getUniqueId()))
            return "§cTo nie jest Twoja aukcja!";

        // Zwróć kasę licytującemu
        if (listing.getType() == AuctionListing.Type.BID && listing.getHighBidder() != null) {
            sendMoneyViaMail(listing.getHighBidder(),
                    "Aukcja anulowana: " + getItemName(listing.getItem()),
                    "Sprzedający §f" + listing.getSellerName() + " §7zdjął aukcję.\n"
                            + "Zwrócono Twoją ofertę.",
                    null, listing.getCurrentBid(), null);
        }

        // Przedmiot z powrotem do sprzedającego
        seller.getInventory().addItem(listing.getItem())
                .forEach((slot, overflow) ->
                        seller.getWorld().dropItemNaturally(seller.getLocation(), overflow));

        listing.markExpired();
        listings.remove(listingId);
        db.deleteAuction(listingId);
        seller.sendMessage("§7Aukcja §f" + getItemName(listing.getItem()) + " §7zdjęta.");
        return null;
    }

    // =========================================================================
    // Wygasanie
    // =========================================================================

    private void checkExpired() {
        List<String> toRemove = new ArrayList<>();
        for (AuctionListing listing : listings.values()) {
            if (!listing.isExpired()) continue;
            toRemove.add(listing.getId());
            resolveExpired(listing);
        }
        toRemove.forEach(id -> { listings.remove(id); db.deleteAuction(id); });
    }

    private void resolveExpired(AuctionListing listing) {
        String itemName = getItemName(listing.getItem());

        if (listing.getType() == AuctionListing.Type.FIXED || listing.getHighBidder() == null) {
            // Nikt nie kupił — zwróć przedmiot sprzedającemu
            sendItemViaMail(listing.getSellerUuid(),
                    "Aukcja wygasła: " + itemName,
                    "Nikt nie kupił Twojego przedmiotu z aukcji.",
                    listing.getItem());
            notifyOnline(listing.getSellerUuid(),
                    "§7Aukcja §f" + itemName + " §7wygasła — przedmiot zwrócony do poczty.");
        } else {
            // Aukcja BID z ofertą — rozlicz
            double price      = listing.getCurrentBid();
            double sellerGets = price * (1.0 - commissionPercent / 100.0);

            // Przedmiot do wygrywającego
            sendItemViaMail(listing.getHighBidder(),
                    "Wygrałeś aukcję: " + itemName,
                    "Gratulacje! Wygrałeś licytację za §e" + formatPrice(price) + "§7.",
                    listing.getItem());
            notifyOnline(listing.getHighBidder(),
                    "§a§lWygrałeś aukcję! §r§7(" + itemName + ") — sprawdź pocztę.");

            // Kasa do sprzedającego
            sendMoneyViaMail(listing.getSellerUuid(),
                    "Aukcja zakończona: " + itemName,
                    "Aukcja zakończyła się. Najwyższa oferta: §e" + formatPrice(price) + "§7.",
                    null, sellerGets,
                    "§8Prowizja: " + (int) commissionPercent + "%");
            notifyOnline(listing.getSellerUuid(),
                    "§a§lAukcja zakończona! §r§7(" + itemName + ") — sprawdź pocztę.");

            if (questManager != null) {
                questManager.addProgress(listing.getSellerUuid(), QuestTrigger.AUCTION_SOLD, "TRANSACTION", 1);
                questManager.addProgress(listing.getSellerUuid(), QuestTrigger.AUCTION_SOLD,
                        "VALUE", (int) Math.min(price, 1_000_000_000.0));
                questManager.addProgress(listing.getHighBidder(), QuestTrigger.AUCTION_WON, "", 1);
            }
        }
    }

    // =========================================================================
    // Poczta — helper
    // =========================================================================

    private void sendItemViaMail(UUID recipient, String subject, String message, ItemStack item) {
        if (mailManager != null) {
            mailManager.sendSystem(recipient, subject, message, item, 0);
        } else {
            // Fallback — gracz online
            Player p = Bukkit.getPlayer(recipient);
            if (p != null) p.getInventory().addItem(item);
        }
    }

    private void sendMoneyViaMail(UUID recipient, String subject, String bodyLine,
                                  ItemStack item, double money, String extraLine) {
        if (mailManager != null) {
            String fullMessage = bodyLine + (extraLine != null ? "\n" + extraLine : "");
            mailManager.sendSystem(recipient, subject, fullMessage, item, money);
        } else {
            economy.depositOffline(recipient, money);
        }
    }

    private void notifyOnline(UUID uuid, String msg) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) p.sendMessage(msg);
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    private void removeItemFromPlayer(Player player, ItemStack item) {
        player.getInventory().removeItem(item);
    }

    public String getItemName(ItemStack item) {
        if (item == null) return "?";
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        String name = item.getType().name().replace("_", " ").toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public String formatPrice(double price) {
        return stark.skyBlockTest2.util.PriceFormat.format(price);
    }

    public List<AuctionListing> getActiveListings() {
        return listings.values().stream().filter(l -> !l.isExpired()).collect(Collectors.toList());
    }

    public List<AuctionListing> getPlayerListings(UUID uuid) {
        return listings.values().stream()
                .filter(l -> l.getSellerUuid().equals(uuid) && !l.isExpired())
                .collect(Collectors.toList());
    }

    public List<AuctionListing> getBiddingListings(UUID uuid) {
        return listings.values().stream()
                .filter(l -> !l.isExpired())
                .filter(l -> l.getType() == AuctionListing.Type.BID)
                .filter(l -> l.hasBidFrom(uuid))
                .collect(Collectors.toList());
    }

    public AuctionListing getListing(String id)      { return listings.get(id); }
    public double getCommissionPercent()              { return commissionPercent; }
    public int    getMaxListingsPerPlayer()           { return maxListingsPerPlayer; }
    public double getMinBidIncrementPercent()         { return minBidIncrementPercent; }
}