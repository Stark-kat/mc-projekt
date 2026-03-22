package stark.skyBlockTest2.island;

/**
 * Akcje, które gość może wykonać na wyspie.
 * Wyciągnięte z IslandProtectionListener — model danych nie powinien
 * zależeć od warstwy listenerów.
 * Akcje z editable=false nie pojawiają się w GUI ustawień —
 * są zawsze wyłączone (np. PVP) lub zawsze włączone.
 */
public enum IslandAction {

    // --- Ogólne ---
    BREAK_BLOCKS("Niszczenie",                      false, ActionCategory.GENERAL),
    PLACE_BLOCKS("Budowanie",                       false, ActionCategory.GENERAL),
    USE_BUCKETS("Używanie wiader",                  false, ActionCategory.GENERAL),
    FIRE_SPREAD("Podpalanie",                       false, ActionCategory.GENERAL),
    CROP_TRAMPLE("Niszczenie upraw przez skok",     false, ActionCategory.GENERAL),
    TELEPORT_VISIT("Odwiedziny",                    true,  ActionCategory.GENERAL),
    PICKUP_ITEMS("Podnoszenie przedmiotów",         true,  ActionCategory.GENERAL),
    DROP_ITEMS("Wyrzucanie przedmiotów",            true,  ActionCategory.GENERAL),
    USE_PORTALS("Używanie portali",                 true,  ActionCategory.GENERAL),

    // --- Bloki użytkowe ---
    OPEN_CONTAINERS("Otwieranie skrzyń",            false, ActionCategory.UTILITY),
    USE_CRAFTING("Stół rzemieślniczy",              true,  ActionCategory.UTILITY),
    USE_ANVIL("Używanie kowadeł",                   true,  ActionCategory.UTILITY),
    USE_ENCHANTING("Stół do zaklęć",                true,  ActionCategory.UTILITY),
    USE_BREWING("Stół alchemiczny",                 true,  ActionCategory.UTILITY),
    INTERACT_DECORATIONS("Dekoracje",               false, ActionCategory.UTILITY),
    INTERACT_UTILITY("Mównica",                     false, ActionCategory.UTILITY),
    ITEM_FRAME_INTERACT("Ramki",                    false, ActionCategory.UTILITY),
    ARMOR_STAND_INTERACT("Interakcja ze stojakiem", false, ActionCategory.UTILITY),

    // --- Mechanizmy ---
    USE_DOORS("Drzwi i furtki",                     true,  ActionCategory.REDSTONE),
    USE_BUTTONS("Dźwignie i przyciski",             true,  ActionCategory.REDSTONE),
    USE_PRESSURE_PLATES("Płytki naciskowe",         true,  ActionCategory.REDSTONE),

    // --- Istoty ---
    INTERACT_ANIMALS("Karmienie i hodowanie",       true,  ActionCategory.MOBS),
    MILK_COWS("Dojenie krów",                       true,  ActionCategory.MOBS),
    SHEAR_SHEEP("Strzyżenie owiec",                 true,  ActionCategory.MOBS),
    KILL_ANIMALS("Zabijanie istot",                 false, ActionCategory.MOBS),
    VILLAGER_TRADE("Handel z Villagerami",          true,  ActionCategory.MOBS);

    private final String displayName;
    private final boolean editable;
    private final ActionCategory category;

    IslandAction(String displayName, boolean editable, ActionCategory category) {
        this.displayName = displayName;
        this.editable    = editable;
        this.category    = category;
    }

    public String getDisplayName()      { return displayName; }
    public boolean isEditable()         { return editable; }
    public ActionCategory getCategory() { return category; }
}