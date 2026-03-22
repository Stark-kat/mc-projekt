package stark.skyBlockTest2.island;

import org.bukkit.Location;

import java.util.*;

public class Island {

    private UUID owner;
    private final List<UUID> members;
    private final Location center;
    private Location home;
    private int size;
    private final int index;
    private final Map<IslandAction, Boolean> visitorSettings = new HashMap<>();
    private final Set<UUID> bannedPlayers = new HashSet<>();
    private final Map<UUID, IslandRole> memberRoles = new HashMap<>();

    // ---- XP wyspy (wspólne dla całej wyspy, czysto prestiżowe) ----
    private long xp = 0;

    public Island(UUID owner, Location center, int size, int index, List<UUID> members) {
        this.owner = owner;
        this.members = (members != null) ? members : new ArrayList<>();
        this.center = center;
        this.home = center.clone().add(0.5, 1, 0.5);
        this.size = size;
        this.index = index;
    }

    // -------------------------------------------------------------------------
    // Granice wyspy (w chunkach)
    // -------------------------------------------------------------------------

    public int getMinChunkX() { return (center.getBlockX() >> 4) - size; }
    public int getMaxChunkX() { return (center.getBlockX() >> 4) + size; }
    public int getMinChunkZ() { return (center.getBlockZ() >> 4) - size; }
    public int getMaxChunkZ() { return (center.getBlockZ() >> 4) + size; }

    public boolean isInside(Location loc) {
        if (loc == null) return false;
        if (loc.getWorld() == null || center.getWorld() == null) return false;
        if (!loc.getWorld().equals(center.getWorld())) return false;

        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        return cx >= getMinChunkX() && cx <= getMaxChunkX()
                && cz >= getMinChunkZ() && cz <= getMaxChunkZ();
    }

    // -------------------------------------------------------------------------
    // Gettery / Settery
    // -------------------------------------------------------------------------

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }

    public List<UUID> getMembers() { return members; }

    public boolean isMember(UUID uuid) {
        return owner.equals(uuid) || members.contains(uuid);
    }

    public Location getCenter() { return center; }
    public Location getHome()   { return home; }
    public void setHome(Location home) { this.home = home; }

    public int getSize()  { return size; }
    public void setSize(int size) { this.size = size; }

    public int getIndex() { return index; }

    // ---- XP ----

    public long getXp() { return xp; }

    public void setXp(long xp) { this.xp = Math.max(0, xp); }

    public void addXp(long amount) {
        if (amount > 0) this.xp += amount;
    }

    // -------------------------------------------------------------------------
    // Ustawienia gości
    // -------------------------------------------------------------------------

    public boolean canVisitorDo(IslandAction action) {
        return visitorSettings.getOrDefault(action, false);
    }

    public void setVisitorSetting(IslandAction action, boolean allow) {
        visitorSettings.put(action, allow);
    }

    public Map<IslandAction, Boolean> getVisitorSettings() {
        return visitorSettings;
    }

    // -------------------------------------------------------------------------
    // System banów
    // -------------------------------------------------------------------------

    public boolean isBanned(UUID uuid)  { return bannedPlayers.contains(uuid); }
    public void banPlayer(UUID uuid)    { bannedPlayers.add(uuid); }
    public void unbanPlayer(UUID uuid)  { bannedPlayers.remove(uuid); }
    public Set<UUID> getBannedPlayers() { return bannedPlayers; }

    // -------------------------------------------------------------------------
    // System ról
    // -------------------------------------------------------------------------

    public IslandRole getRole(UUID uuid) {
        if (uuid.equals(owner)) return IslandRole.OWNER;
        if (memberRoles.containsKey(uuid)) return memberRoles.get(uuid);
        if (members.contains(uuid)) return IslandRole.MEMBER;
        return null;
    }

    public void setRole(UUID uuid, IslandRole role) { memberRoles.put(uuid, role); }
    public Map<UUID, IslandRole> getMemberRoles()   { return memberRoles; }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Island other)) return false;
        return this.index == other.index;
    }

    @Override
    public int hashCode() { return Integer.hashCode(index); }

    @Override
    public String toString() {
        return "Island{owner=" + owner + ", index=" + index + ", size=" + size + ", xp=" + xp + "}";
    }
}