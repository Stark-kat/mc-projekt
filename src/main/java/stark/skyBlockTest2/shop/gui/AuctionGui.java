package stark.skyBlockTest2.shop.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.menu.MenuHolder;
import stark.skyBlockTest2.shop.auction.AuctionListing;
import stark.skyBlockTest2.shop.auction.AuctionManager;

import java.util.*;
import java.util.stream.Collectors;

public class AuctionGui {

    // =========================================================================
    // Enums
    // =========================================================================

    public enum Tab { ALL, MY_LISTINGS, BIDDING }

    public enum TypeFilter {
        ALL("§fWszystko", Material.PAPER),
        FIXED("§aKup teraz", Material.EMERALD),
        BID("§9Licytacja", Material.GOLD_INGOT);

        final String label;
        final Material icon;
        TypeFilter(String label, Material icon) { this.label = label; this.icon = icon; }
    }

    public enum PriceSort {
        NONE("§7Brak", Material.GRAY_DYE),
        ASC("§a↑ Najtańsze", Material.LIME_DYE),
        DESC("§c↓ Najdroższe", Material.RED_DYE);

        final String label;
        final Material icon;
        PriceSort(String label, Material icon) { this.label = label; this.icon = icon; }
    }

    public enum TimeSort {
        NONE("§7Brak", Material.GRAY_DYE),
        ASC("§e↑ Kończące się", Material.YELLOW_DYE),
        DESC("§b↓ Najdłużej trwające", Material.CYAN_DYE);

        final String label;
        final Material icon;
        TimeSort(String label, Material icon) { this.label = label; this.icon = icon; }
    }

    // =========================================================================
    // Stan filtrów per gracz
    // =========================================================================

    private static class ViewState {
        Tab        tab        = Tab.ALL;
        TypeFilter typeFilter = TypeFilter.ALL;
        PriceSort  priceSort  = PriceSort.NONE;
        TimeSort   timeSort   = TimeSort.NONE;
        int        page       = 0;
    }

    private final Map<UUID, ViewState> states = new HashMap<>();

    // =========================================================================
    // Sloty
    // =========================================================================

    // Rząd 0 (0-8):   filtry
    // Rząd 1-4 (9-44): zawartość — ale sloty środkowe z pominięciem ramki
    // Rząd 5 (45-53): nawigacja + zakładki

    private static final int PAGE_SIZE = 21; // 3 rzędy x 7 slotów (omijamy boki)
    private static final int[] CONTENT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34
    };

    // Sloty filtrów w rzędzie 0
    private static final int SLOT_TYPE_FILTER = 2;
    private static final int SLOT_PRICE_SORT  = 5;
    private static final int SLOT_TIME_SORT   = 7;

    private final SkyBlockTest2  plugin;
    private final AuctionManager auctionManager;

    public AuctionGui(SkyBlockTest2 plugin, AuctionManager auctionManager) {
        this.plugin         = plugin;
        this.auctionManager = auctionManager;
    }

    // =========================================================================
    // Publiczne API
    // =========================================================================

    public void open(Player player, int page)           { openTab(player, Tab.ALL, page); }
    public void openMyListings(Player player, int page) { openTab(player, Tab.MY_LISTINGS, page); }
    public void openBidding(Player player, int page)    { openTab(player, Tab.BIDDING, page); }

    public void openTab(Player player, Tab tab, int page) {
        ViewState state = states.computeIfAbsent(player.getUniqueId(), k -> new ViewState());
        state.tab  = tab;
        state.page = page;
        render(player, state);
    }

    /** Cykl filtra typu — kliknięcie przechodzi do następnego */
    public void cycleTypeFilter(Player player) {
        ViewState state = getState(player);
        TypeFilter[] vals = TypeFilter.values();
        state.typeFilter = vals[(state.typeFilter.ordinal() + 1) % vals.length];
        state.page = 0;
        render(player, state);
    }

    /** Cykl sortowania ceny — resetuje sortowanie czasu */
    public void cyclePriceSort(Player player) {
        ViewState state = getState(player);
        PriceSort[] vals = PriceSort.values();
        state.priceSort = vals[(state.priceSort.ordinal() + 1) % vals.length];
        if (state.priceSort != PriceSort.NONE) state.timeSort = TimeSort.NONE; // reset
        state.page = 0;
        render(player, state);
    }

    /** Cykl sortowania czasu — resetuje sortowanie ceny */
    public void cycleTimeSort(Player player) {
        ViewState state = getState(player);
        TimeSort[] vals = TimeSort.values();
        state.timeSort = vals[(state.timeSort.ordinal() + 1) % vals.length];
        if (state.timeSort != TimeSort.NONE) state.priceSort = PriceSort.NONE; // reset
        state.page = 0;
        render(player, state);
    }

    // =========================================================================
    // Renderowanie
    // =========================================================================

    private void render(Player player, ViewState state) {
        List<AuctionListing> listings = getListings(player, state);

        int totalPages = Math.max(1, (int) Math.ceil((double) listings.size() / PAGE_SIZE));
        state.page = Math.max(0, Math.min(state.page, totalPages - 1));

        String title = switch (state.tab) {
            case ALL         -> "§6Dom Aukcyjny";
            case MY_LISTINGS -> "§6Moje Aukcje";
            case BIDDING     -> "§9Licytuję";
        };
        if (totalPages > 1) title += " §8(" + (state.page + 1) + "/" + totalPages + ")";

        Inventory inv = Bukkit.createInventory(new MenuHolder(), 54, Component.text(title));
        GuiBuilder builder = new GuiBuilder(inv);

        // ---- Rząd 0: filtry ----
        builder.set(SLOT_TYPE_FILTER, buildTypeFilterItem(state));
        builder.set(SLOT_PRICE_SORT,  buildPriceSortItem(state));
        builder.set(SLOT_TIME_SORT,   buildTimeSortItem(state));

        // ---- Zawartość ----
        int start = state.page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, listings.size());
        for (int i = start; i < end; i++) {
            builder.set(CONTENT_SLOTS[i - start], buildListingItem(listings.get(i), player));
        }

        // ---- Paginacja ----
        if (state.page > 0) {
            builder.set(36, new ItemBuilder(Material.ARROW)
                    .name("§7« Poprzednia strona")
                    .setString("action", "AuctionPage")
                    .setString("auction_tab", state.tab.name())
                    .setString("page", String.valueOf(state.page - 1))
                    .build());
        }
        if (state.page < totalPages - 1) {
            builder.set(44, new ItemBuilder(Material.ARROW)
                    .name("§7Następna strona »")
                    .setString("action", "AuctionPage")
                    .setString("auction_tab", state.tab.name())
                    .setString("page", String.valueOf(state.page + 1))
                    .build());
        }

        // ---- Rząd 5: zakładki + nawigacja ----
        builder.set(45, new ItemBuilder(state.tab == Tab.ALL ? Material.GOLD_INGOT : Material.GOLD_NUGGET)
                .name(state.tab == Tab.ALL ? "§6§lWszystkie aukcje" : "§7Wszystkie aukcje")
                .glow(state.tab == Tab.ALL)
                .setString("action", "AuctionTab")
                .setString("auction_tab", Tab.ALL.name())
                .build());

        builder.set(46, new ItemBuilder(state.tab == Tab.MY_LISTINGS ? Material.CHEST : Material.BARREL)
                .name(state.tab == Tab.MY_LISTINGS ? "§e§lMoje aukcje" : "§7Moje aukcje")
                .glow(state.tab == Tab.MY_LISTINGS)
                .setString("action", "AuctionTab")
                .setString("auction_tab", Tab.MY_LISTINGS.name())
                .build());

        builder.set(47, new ItemBuilder(state.tab == Tab.BIDDING ? Material.DIAMOND : Material.QUARTZ)
                .name(state.tab == Tab.BIDDING ? "§b§lLicytuję" : "§7Licytuję")
                .lore("§8Aukcje w których bierzesz udział")
                .glow(state.tab == Tab.BIDDING)
                .setString("action", "AuctionTab")
                .setString("auction_tab", Tab.BIDDING.name())
                .build());

        builder.set(49, new ItemBuilder(Material.BARRIER)
                .name("§7Powrót do sklepu")
                .setString("action", "OpenShopHub")
                .build());

        builder.set(52, new ItemBuilder(Material.EMERALD)
                .name("§a§lWystaw przedmiot")
                .lore("§7Trzymaj przedmiot w ręce.")
                .setString("action", "OpenAuctionCreate")
                .build());

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        player.openInventory(inv);
    }

    // =========================================================================
    // Filtry / sortowanie
    // =========================================================================

    private List<AuctionListing> getListings(Player player, ViewState state) {
        UUID uuid = player.getUniqueId();

        List<AuctionListing> list = switch (state.tab) {
            case ALL         -> auctionManager.getActiveListings();
            case MY_LISTINGS -> auctionManager.getPlayerListings(uuid);
            case BIDDING     -> auctionManager.getBiddingListings(uuid);
        };

        // Filtr typu
        if (state.typeFilter == TypeFilter.FIXED) {
            list = list.stream()
                    .filter(l -> l.getType() == AuctionListing.Type.FIXED)
                    .collect(Collectors.toList());
        } else if (state.typeFilter == TypeFilter.BID) {
            list = list.stream()
                    .filter(l -> l.getType() == AuctionListing.Type.BID)
                    .collect(Collectors.toList());
        }

        // Sortowanie — cena lub czas (wzajemnie się wykluczają)
        if (state.priceSort == PriceSort.ASC) {
            list = list.stream()
                    .sorted(Comparator.comparingDouble(l ->
                            l.getType() == AuctionListing.Type.BID ? l.getCurrentBid() : l.getPrice()))
                    .collect(Collectors.toList());
        } else if (state.priceSort == PriceSort.DESC) {
            list = list.stream()
                    .sorted(Comparator.comparingDouble((AuctionListing l) ->
                                    l.getType() == AuctionListing.Type.BID ? l.getCurrentBid() : l.getPrice())
                            .reversed())
                    .collect(Collectors.toList());
        } else if (state.timeSort == TimeSort.ASC) {
            list = list.stream()
                    .sorted(Comparator.comparingLong(AuctionListing::getRemainingMs))
                    .collect(Collectors.toList());
        } else if (state.timeSort == TimeSort.DESC) {
            list = list.stream()
                    .sorted(Comparator.comparingLong(AuctionListing::getRemainingMs).reversed())
                    .collect(Collectors.toList());
        }

        return list;
    }

    // =========================================================================
    // Przyciski filtrów
    // =========================================================================

    private ItemStack buildTypeFilterItem(ViewState state) {
        TypeFilter cur  = state.typeFilter;
        TypeFilter next = TypeFilter.values()[(cur.ordinal() + 1) % TypeFilter.values().length];

        return new ItemBuilder(cur.icon)
                .name("§7Typ: " + cur.label)
                .lore(
                        "§8Filtruje aukcje po typie.",
                        " ",
                        buildFilterLine(TypeFilter.ALL,   cur),
                        buildFilterLine(TypeFilter.FIXED, cur),
                        buildFilterLine(TypeFilter.BID,   cur),
                        " ",
                        "§7Kliknij → " + next.label
                )
                .glow(cur != TypeFilter.ALL)
                .setString("action", "AuctionCycleType")
                .build();
    }

    private ItemStack buildPriceSortItem(ViewState state) {
        PriceSort cur  = state.priceSort;
        PriceSort next = PriceSort.values()[(cur.ordinal() + 1) % PriceSort.values().length];
        boolean active = cur != PriceSort.NONE;

        return new ItemBuilder(cur.icon)
                .name("§7Cena: " + cur.label)
                .lore(
                        "§8Sortuje po cenie.",
                        "§8Resetuje sortowanie czasu.",
                        " ",
                        buildSortLine(PriceSort.NONE, cur),
                        buildSortLine(PriceSort.ASC,  cur),
                        buildSortLine(PriceSort.DESC, cur),
                        " ",
                        "§7Kliknij → " + next.label
                )
                .glow(active)
                .setString("action", "AuctionCyclePrice")
                .build();
    }

    private ItemStack buildTimeSortItem(ViewState state) {
        TimeSort cur  = state.timeSort;
        TimeSort next = TimeSort.values()[(cur.ordinal() + 1) % TimeSort.values().length];
        boolean active = cur != TimeSort.NONE;

        return new ItemBuilder(cur.icon)
                .name("§7Czas: " + cur.label)
                .lore(
                        "§8Sortuje po czasie do końca.",
                        "§8Resetuje sortowanie ceny.",
                        " ",
                        buildSortLine(TimeSort.NONE, cur),
                        buildSortLine(TimeSort.ASC,  cur),
                        buildSortLine(TimeSort.DESC, cur),
                        " ",
                        "§7Kliknij → " + next.label
                )
                .glow(active)
                .setString("action", "AuctionCycleTime")
                .build();
    }

    private String buildFilterLine(TypeFilter option, TypeFilter current) {
        return (option == current ? "§a▶ " : "§8  ") + option.label;
    }
    private String buildSortLine(PriceSort option, PriceSort current) {
        return (option == current ? "§a▶ " : "§8  ") + option.label;
    }
    private String buildSortLine(TimeSort option, TimeSort current) {
        return (option == current ? "§a▶ " : "§8  ") + option.label;
    }

    // =========================================================================
    // Budowanie itemu aukcji
    // =========================================================================

    private ItemStack buildListingItem(AuctionListing listing, Player viewer) {
        UUID viewerUuid = viewer.getUniqueId();
        boolean isOwner = listing.getSellerUuid().equals(viewerUuid);
        boolean isBid   = listing.getType() == AuctionListing.Type.BID;
        boolean leading = isBid && listing.isLeading(viewerUuid);
        boolean outbid  = isBid && listing.wasOutbid(viewerUuid);

        List<String> lore = new ArrayList<>();
        lore.add("§7Sprzedawca: §f" + listing.getSellerName());
        lore.add("§7Typ: " + (isBid ? "§9Licytacja" : "§aStała cena"));
        lore.add(" ");

        if (isBid) {
            lore.add("§7Cena wywoławcza: §e" + auctionManager.formatPrice(listing.getPrice()));
            lore.add("§7Aktualna oferta:  §e" + auctionManager.formatPrice(listing.getCurrentBid())
                    + (listing.hasBid() ? "" : " §8(brak ofert)"));
            if (!isOwner) {
                lore.add("§7Min. przebicie:   §a" + auctionManager.formatPrice(auctionManager.getMinBid(listing)));
            }
            if (leading) lore.add("§a§lProwadzisz! ✔");
            if (outbid)  lore.add("§c§lPrzebito Twoją ofertę!");
        } else {
            lore.add("§7Cena: §a" + auctionManager.formatPrice(listing.getPrice()));
        }

        lore.add("§7Pozostało: §f" + listing.getRemainingFormatted());
        lore.add(" ");

        if (isOwner)    lore.add("§cPPM §7— Zdejmij aukcję");
        else if (isBid) lore.add("§eKliknij §7— Złóż ofertę");
        else            lore.add("§eKliknij §7— Kup teraz");

        ItemStack result = listing.getItem();
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            List<String> combined = new ArrayList<>();
            if (meta.hasLore()) { combined.addAll(meta.getLore()); combined.add(" "); }
            combined.addAll(lore);
            meta.setLore(combined);

            if (outbid) {
                meta.addEnchant(Enchantment.FORTUNE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            String actionValue = isOwner ? "AuctionCancel" : (isBid ? "AuctionOpenBid" : "AuctionBuyNow");
            meta.getPersistentDataContainer()
                    .set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, actionValue);
            meta.getPersistentDataContainer()
                    .set(new NamespacedKey(plugin, "auction_id"), PersistentDataType.STRING, listing.getId());
            result.setItemMeta(meta);
        }

        return result;
    }

    // =========================================================================
    // Pomocnicze
    // =========================================================================

    private ViewState getState(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), k -> new ViewState());
    }
}