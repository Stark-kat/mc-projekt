package stark.skyBlockTest2.island;

import org.bukkit.Location;
import stark.skyBlockTest2.island.listener.IslandProtectionListener;

import java.util.*;

public class Island {

    private UUID owner;
    private final List<UUID> members;
    private final Location center;
    private Location home;
    private int size;
    private final int index;
    private Map<IslandProtectionListener.IslandAction, Boolean> visitorSettings = new HashMap<>();

    public Island(UUID owner, Location center, int size, int index, List<UUID> members) {
        this.owner = owner;
        this.members = (members != null) ? members : new ArrayList<>();
        this.center = center;
        this.home = center.clone().add(0.5, 1, 0.5);
        this.size = size;
        this.index = index;
    }

    public int getMinChunkX() { return (center.getBlockX() >> 4) - size; }
    public int getMaxChunkX() { return (center.getBlockX() >> 4) + size; }
    public int getMinChunkZ() { return (center.getBlockZ() >> 4) - size; }
    public int getMaxChunkZ() { return (center.getBlockZ() >> 4) + size; }

    public boolean isInside(Location loc) {
        if (loc == null || !loc.getWorld().equals(center.getWorld())) return false;
        int cx = loc.getBlockX() >> 4; // Szybkie dzielenie przez 16
        int cz = loc.getBlockZ() >> 4;
        return cx >= getMinChunkX() && cx <= getMaxChunkX() &&
                cz >= getMinChunkZ() && cz <= getMaxChunkZ();
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public boolean isMember(UUID uuid) {
        return owner.equals(uuid) || members.contains(uuid);
    }

    public Location getCenter() {
        return center;
    }

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getIndex() {
        return index;
    }

    public boolean canVisitorDo(IslandProtectionListener.IslandAction action) {
        return visitorSettings.getOrDefault(action, false); // Domyślnie gość nie może nic
    }

    public void setVisitorSetting(IslandProtectionListener.IslandAction action, boolean allow) {
        visitorSettings.put(action, allow);
    }

    public Map<IslandProtectionListener.IslandAction, Boolean> getVisitorSettings() {
        return visitorSettings;
    }
}

