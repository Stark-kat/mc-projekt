package stark.skyBlockTest2.rank;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum Rank {

    PLAYER("Gracz", null, NamedTextColor.WHITE, 0),
    VIP("VIP", "§6[VIP]§r", NamedTextColor.GOLD, 1),
    ADMIN("Admin", "§c[Admin]§r", NamedTextColor.RED, 2);

    private final String displayName;
    private final String chatPrefix;   // null = brak prefiksu
    private final TextColor nameColor;
    private final int weight;          // wyższy = ważniejsza ranga (przydatne przy rozbudowie)

    Rank(String displayName, String chatPrefix, TextColor nameColor, int weight) {
        this.displayName = displayName;
        this.chatPrefix = chatPrefix;
        this.nameColor = nameColor;
        this.weight = weight;
    }

    public String getDisplayName() { return displayName; }
    public String getChatPrefix()  { return chatPrefix; }
    public TextColor getNameColor(){ return nameColor; }
    public int getWeight()         { return weight; }

    /** Buduje pełny format wiadomości na czacie */
    public String formatChatMessage(String playerName, String message) {
        if (chatPrefix == null) {
            return playerName + " §7» §f" + message;
        }
        return chatPrefix + " " + playerName + " §7» §f" + message;
    }

    /** Zwraca rangę po nazwie (case-insensitive), domyślnie PLAYER */
    public static Rank fromString(String name) {
        for (Rank r : values()) {
            if (r.name().equalsIgnoreCase(name)) return r;
        }
        return PLAYER;
    }
}