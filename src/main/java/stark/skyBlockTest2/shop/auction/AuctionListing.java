package stark.skyBlockTest2.shop.auction;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuctionListing {

    public enum Type { FIXED, BID }

    private final String    id;
    private final UUID      sellerUuid;
    private final String    sellerName;
    private final ItemStack item;
    private final Type      type;

    // Ceny
    private final double    price;       // cena wywoławcza / stała
    private double          currentBid;  // aktualna najwyższa oferta
    private UUID            highBidder;  // UUID aktualnego lidera

    // Wszyscy którzy kiedykolwiek licytowali (do zakładki "Licytuję")
    private final Set<UUID> allBidders = new HashSet<>();

    // Czas
    private final long      expiresAt;
    private boolean         expired;

    public AuctionListing(String id, UUID sellerUuid, String sellerName,
                          ItemStack item, Type type, double price, long expiresAt) {
        this.id          = id;
        this.sellerUuid  = sellerUuid;
        this.sellerName  = sellerName;
        this.item        = item.clone();
        this.type        = type;
        this.price       = price;
        this.currentBid  = price;
        this.expiresAt   = expiresAt;
        this.expired     = false;
    }

    // =========================================================================
    // Licytacja
    // =========================================================================

    /**
     * Składa ofertę. Zwraca UUID poprzedniego lidera (do zwrotu kasy) lub null.
     * Wywołaj TYLKO z AuctionManager po walidacji.
     */
    public UUID placeBid(UUID bidderUuid, double amount) {
        UUID previousBidder = this.highBidder;
        this.currentBid = amount;
        this.highBidder = bidderUuid;
        this.allBidders.add(bidderUuid);
        return previousBidder; // null jeśli nikt wcześniej nie licytował
    }

    // =========================================================================
    // Czas
    // =========================================================================

    public boolean isExpired() {
        if (expired) return true;
        return System.currentTimeMillis() >= expiresAt;
    }

    public void markExpired() { this.expired = true; }

    public long getRemainingMs() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    public String getRemainingFormatted() {
        long ms      = getRemainingMs();
        long hours   = ms / (1000 * 60 * 60);
        long minutes = (ms % (1000 * 60 * 60)) / (1000 * 60);
        if (hours > 0)   return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m";
        return "<1m";
    }

    // =========================================================================
    // Gettery
    // =========================================================================

    public String         getId()          { return id; }
    public UUID           getSellerUuid()  { return sellerUuid; }
    public String         getSellerName()  { return sellerName; }
    public ItemStack      getItem()        { return item.clone(); }
    public Type           getType()        { return type; }
    public double         getPrice()       { return price; }
    public double         getCurrentBid()  { return currentBid; }
    public UUID           getHighBidder()  { return highBidder; }
    public long           getExpiresAt()   { return expiresAt; }
    public boolean        hasBid()         { return highBidder != null; }
    public Set<UUID>      getAllBidders()   { return Collections.unmodifiableSet(allBidders); }

    /** Czy gracz aktualnie prowadzi w licytacji? */
    public boolean isLeading(UUID uuid)    { return uuid.equals(highBidder); }

    /** Czy gracz licytował ale został przebity? */
    public boolean wasOutbid(UUID uuid)    { return allBidders.contains(uuid) && !uuid.equals(highBidder); }

    /** Czy gracz w ogóle licytował? */
    public boolean hasBidFrom(UUID uuid)   { return allBidders.contains(uuid); }

    // Używane przez AuctionStorage do odtworzenia stanu
    public void addPreviousBidder(UUID uuid) { allBidders.add(uuid); }
}