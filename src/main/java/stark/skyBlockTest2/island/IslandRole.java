package stark.skyBlockTest2.island;

/**
 * Rola gracza na wyspie.
 * Hierarchia: OWNER > CO_LEADER > MEMBER
 *
 * Na razie CO_LEADER ma uprawnienia do banowania graczy.
 * Docelowo rola będzie miała własny zestaw uprawnień do dostosowania.
 */
public enum IslandRole {

    OWNER("§6Właściciel"),
    CO_LEADER("§eCo-Leader"),
    MEMBER("§7Członek");

    private final String displayName;

    IslandRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Czy ta rola ma uprawnienie do banowania graczy? */
    public boolean canBan() {
        return this == OWNER || this == CO_LEADER;
    }

    /** Czy ta rola ma uprawnienie do zapraszania graczy? */
    public boolean canInvite() {
        return this == OWNER || this == CO_LEADER;
    }

    /** Czy ta rola ma uprawnienie do kickowania graczy? */
    public boolean canKick() {
        return this == OWNER || this == CO_LEADER;
    }
}