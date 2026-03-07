package stark.skyBlockTest2.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.teleport.TeleportManager;
import stark.skyBlockTest2.border.BorderManager;
import stark.skyBlockTest2.economy.EconomyManager;

import java.util.*;
import java.util.EnumMap;

public class IslandManager {

    private final SkyBlockTest2 plugin;
    private final Map<UUID, Island> playerToIsland = new HashMap<>();
    private final Map<UUID, Island> islandsByOwner = new HashMap<>();
    private final Map<Long, Island> islandGrid = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final Set<UUID> pendingDeletes = new HashSet<>();
    private final IslandGenerator generator = new IslandGenerator();
    private final IslandStorage storage;
    private final TeleportManager teleportManager;
    private BorderManager borderManager;
    private EconomyManager economyManager;

    private final int[] islandSizes = {0, 1, 2, 3, 4};
    private static final int SPACING_CHUNKS = 10;

    private int currentIndex;
    private final List<Integer> freeIndexes = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Wyspy dodatkowych typów (NETHER, END, ...)
    // -------------------------------------------------------------------------
    private final Map<IslandType, Map<UUID, Island>> secondaryIslands  = new EnumMap<>(IslandType.class);
    private final Map<IslandType, Map<Long, Island>> secondaryGrids    = new EnumMap<>(IslandType.class);
    private final Map<IslandType, Integer>           secondaryIndexes  = new EnumMap<>(IslandType.class);
    private final Map<IslandType, List<Integer>>     secondaryFreeIdxs = new EnumMap<>(IslandType.class);

    public void setBorderManager(BorderManager borderManager) {
        this.borderManager = borderManager;
    }

    public void setEconomyManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    public IslandManager(SkyBlockTest2 plugin, TeleportManager teleportManager) {
        this.plugin = plugin;
        this.storage = new IslandStorage(plugin);
        this.teleportManager = teleportManager;

        // OVERWORLD
        this.currentIndex = storage.getCurrentIndex(IslandType.OVERWORLD);
        this.freeIndexes.addAll(storage.getFreeIndexes(IslandType.OVERWORLD));

        Map<UUID, Island> loaded = storage.loadIslands(IslandType.OVERWORLD);
        islandsByOwner.putAll(loaded);
        for (Island island : loaded.values()) {
            registerIslandInMemory(island);
        }

        // Typy dodatkowe (NETHER itp.)
        for (IslandType type : IslandType.values()) {
            if (type == IslandType.OVERWORLD) continue;
            secondaryIslands.put(type, new HashMap<>());
            secondaryGrids.put(type, new HashMap<>());
            secondaryIndexes.put(type, storage.getCurrentIndex(type));
            secondaryFreeIdxs.put(type, new ArrayList<>(storage.getFreeIndexes(type)));

            Map<UUID, Island> sec = storage.loadIslands(type);
            secondaryIslands.get(type).putAll(sec);
            for (Island island : sec.values()) {
                secondaryGrids.get(type).put(getGridKey(island.getCenter()), island);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Pomocnicze
    // -------------------------------------------------------------------------

    public boolean hasIsland(UUID uuid) {
        return playerToIsland.containsKey(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return islandsByOwner.containsKey(uuid);
    }

    public Island getIsland(UUID uuid) {
        return playerToIsland.get(uuid);
    }

    public IslandStorage getStorage() {
        return storage;
    }

    private void registerIslandInMemory(Island island) {
        playerToIsland.put(island.getOwner(), island);
        for (UUID memberUuid : island.getMembers()) {
            playerToIsland.put(memberUuid, island);
        }
        islandGrid.put(getGridKey(island.getCenter()), island);
    }

    private long getGridKey(Location loc) {
        long gridX = Math.round((double) (loc.getBlockX() >> 4) / SPACING_CHUNKS);
        long gridZ = Math.round((double) (loc.getBlockZ() >> 4) / SPACING_CHUNKS);
        return (gridX << 32) | (gridZ & 0xFFFFFFFFL);
    }

    // -------------------------------------------------------------------------
    // Tworzenie wyspy
    // -------------------------------------------------------------------------

    public void createIsland(Player player) {
        if (hasIsland(player.getUniqueId())) {
            player.sendMessage("§cMasz już wyspę!");
            return;
        }

        World world = plugin.getWorldManager().getWorld();
        int index;

        if (!freeIndexes.isEmpty()) {
            index = freeIndexes.removeFirst();
        } else {
            index = currentIndex;
            currentIndex++;
        }

        // Jeden zapis zamiast dwóch osobnych (setCurrentIndex + setFreeIndexes)
        storage.saveIndexState(IslandType.OVERWORLD, currentIndex, freeIndexes);

        int[] spiral = SpiralCalculator.getSpiralPosition(index);
        int x = (spiral[0] * SPACING_CHUNKS) << 4;
        int z = (spiral[1] * SPACING_CHUNKS) << 4;
        Location center = new Location(world, x + 8, 100, z + 8);

        Island island = new Island(player.getUniqueId(), center, 0, index, new ArrayList<>());
        island.setHome(center.clone().add(0.5, 0.1, 0.5));

        islandsByOwner.put(player.getUniqueId(), island);
        registerIslandInMemory(island);

        center.getChunk().load(true);
        storage.saveIsland(island, IslandType.OVERWORLD);

        player.sendMessage("§7Tworzenie wyspy, poczekaj chwilę...");
        plugin.getSchematicManager().pasteSchematic(center, "Default_Island", () -> {
            player.teleport(island.getHome());
            player.sendMessage("§aWyspa została utworzona!");
        });
    }

    // -------------------------------------------------------------------------
    // Dom wyspy
    // -------------------------------------------------------------------------

    public void setHome(Player player) {
        Island island = getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage("§cNie masz wyspy!");
            return;
        }
        if (!isOnOwnIsland(player, player.getLocation())) {
            player.sendMessage("§cMożesz ustawić dom tylko na terenie własnej wyspy!");
            return;
        }
        if (!isLocationSafe(player.getLocation())) {
            player.sendMessage("§cNie możesz tu ustawić domu — miejsce jest niebezpieczne!");
            return;
        }
        island.setHome(player.getLocation());
        storage.saveIsland(island, IslandType.OVERWORLD);
        player.sendMessage("§aPunkt domowy wyspy został ustawiony!");
    }

    public void teleportHome(Player player) {
        Island island = getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage("§cNie należysz do żadnej wyspy!");
            return;
        }
        if (!isLocationSafe(island.getHome())) {
            player.sendMessage("§cTwój dom wyspy jest uszkodzony! Napraw go komendą §e/is repairhome");
            return;
        }
        player.teleport(island.getHome());
        player.sendMessage("§aTeleportowano na dom wyspy.");
    }

    private boolean isLocationSafe(Location loc) {
        Block ground = loc.clone().subtract(0, 1, 0).getBlock();
        Block feet   = loc.getBlock();
        Block head   = loc.clone().add(0, 1, 0).getBlock();
        return ground.getType().isSolid()
                && feet.getType().isAir()
                && head.getType().isAir();
    }

    // -------------------------------------------------------------------------
    // Naprawa domu
    // -------------------------------------------------------------------------

    public void repairHomeToNearest(Island island) {
        Location currentHome = island.getHome();
        World world = currentHome.getWorld();
        if (world == null) {
            handleEmergencyHome(island, island.getCenter());
            return;
        }

        int minY    = world.getMinHeight();
        int maxY    = world.getMaxHeight(); // obsługuje 1.18+ (do 320)
        int maxR    = 10;
        int maxYOff = 20;

        for (int yOffset = 0; yOffset <= maxYOff; yOffset++) {
            int[] yLevels = (yOffset == 0)
                    ? new int[]{currentHome.getBlockY()}
                    : new int[]{currentHome.getBlockY() + yOffset, currentHome.getBlockY() - yOffset};

            for (int y : yLevels) {
                if (y < minY || y > maxY) continue;

                for (int r = 0; r <= maxR; r++) {
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (Math.abs(dx) != r && Math.abs(dz) != r) continue; // tylko brzeg kwadratu

                            Location check = new Location(world,
                                    currentHome.getBlockX() + dx,
                                    y,
                                    currentHome.getBlockZ() + dz);

                            if (island.isInside(check) && isLocationSafe(check)) {
                                setAndSaveHome(island, check,
                                        currentHome.getYaw(), currentHome.getPitch());
                                return;
                            }
                        }
                    }
                }
            }
        }

        handleEmergencyHome(island, island.getCenter());
    }

    private void setAndSaveHome(Island island, Location loc, float yaw, float pitch) {
        Location newHome = loc.clone().add(0.5, 0.1, 0.5);
        newHome.setYaw(yaw);
        newHome.setPitch(pitch);
        island.setHome(newHome);
        storage.saveIsland(island, IslandType.OVERWORLD);
    }

    private void handleEmergencyHome(Island island, Location center) {
        Location emergencyLoc = center.clone();
        emergencyLoc.setY(100);

        Block blockBelow = emergencyLoc.clone().subtract(0, 1, 0).getBlock();
        if (blockBelow.isEmpty() || blockBelow.isLiquid()) {
            blockBelow.setType(org.bukkit.Material.GLASS);
        }

        island.setHome(emergencyLoc.add(0.5, 0.1, 0.5));
        storage.saveIsland(island, IslandType.OVERWORLD);
    }

    // -------------------------------------------------------------------------
    // Usuwanie wyspy
    // -------------------------------------------------------------------------

    public void requestDeleteIsland(Player player) {
        if (!isOwner(player.getUniqueId())) {
            player.sendMessage("§cTylko właściciel może usunąć wyspę!");
            return;
        }

        UUID uuid = player.getUniqueId();
        pendingDeletes.add(uuid);
        stark.skyBlockTest2.util.ChatUtil.sendDeleteConfirmMessage(player);

        // Auto-usunięcie po 60 sekundach
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingDeletes.remove(uuid)) {
                if (player.isOnline()) player.sendMessage("§7Potwierdzenie usunięcia wyspy wygasło.");
            }
        }, 1200L);
    }

    public void cancelDeleteIsland(Player player) {
        if (pendingDeletes.remove(player.getUniqueId())) {
            player.sendMessage("§aAnulowano usuwanie wyspy.");
        } else {
            player.sendMessage("§cNie masz aktywnego wniosku o usunięcie wyspy.");
        }
    }

    public void deleteIsland(Player player) {
        UUID ownerUUID = player.getUniqueId();

        if (!pendingDeletes.remove(ownerUUID)) {
            player.sendMessage("§cNajpierw wpisz §e/island delete §caby zainicjować usuwanie.");
            return;
        }

        Island island = islandsByOwner.remove(ownerUUID);

        if (island == null) {
            player.sendMessage("§cNie jesteś właścicielem żadnej wyspy!");
            return;
        }

        playerToIsland.remove(ownerUUID);
        for (UUID memberUUID : island.getMembers()) {
            playerToIsland.remove(memberUUID);
        }
        islandGrid.remove(getGridKey(island.getCenter()));

        teleportEveryoneFromIsland(island);
        generator.clearIsland(island, freeIndexes,
                () -> storage.saveIndexState(IslandType.OVERWORLD, currentIndex, freeIndexes));
        storage.deleteIsland(ownerUUID, IslandType.OVERWORLD);

        player.sendMessage("§cWyspa została usunięta!");
    }

    private void teleportEveryoneFromIsland(Island island) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (island.isInside(onlinePlayer.getLocation())) {
                teleportManager.teleportWithoutDelay(onlinePlayer);
                onlinePlayer.sendMessage("§eZostałeś przeteleportowany, ponieważ ta wyspa została usunięta.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // System banów
    // -------------------------------------------------------------------------

    public void banPlayer(Player executor, String targetName) {
        Island island = islandsByOwner.get(executor.getUniqueId());

        // Sprawdź czy executor ma wyspę i odpowiednią rolę
        if (island == null) {
            // Może być co-leader — szukamy jego wyspy
            island = playerToIsland.get(executor.getUniqueId());
        }
        if (island == null) {
            executor.sendMessage("§cNie należysz do żadnej wyspy!");
            return;
        }

        IslandRole role = island.getRole(executor.getUniqueId());
        if (role == null || !role.canBan()) {
            executor.sendMessage("§cTylko właściciel i Co-Leader mogą banować graczy!");
            return;
        }

        // Znajdź cel
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null) {
            executor.sendMessage("§cNie znaleziono gracza §e" + targetName + "§c.");
            return;
        }

        UUID targetUUID = target.getUniqueId();

        if (island.isMember(targetUUID)) {
            executor.sendMessage("§cNie możesz zbanować członka wyspy! Najpierw go wykop.");
            return;
        }
        if (targetUUID.equals(island.getOwner())) {
            executor.sendMessage("§cNie możesz zbanować właściciela!");
            return;
        }
        if (island.isBanned(targetUUID)) {
            executor.sendMessage("§cTen gracz jest już zbanowany.");
            return;
        }

        island.banPlayer(targetUUID);
        storage.saveIsland(island, IslandType.OVERWORLD);

        executor.sendMessage("§aGracz §e" + targetName + " §azostał zbanowany na wyspie.");

        // Jeśli online — teleportuj natychmiast
        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null && island.isInside(targetPlayer.getLocation())) {
            teleportManager.teleportWithoutDelay(targetPlayer);
            targetPlayer.sendMessage("§cZostałeś zbanowany na wyspie gracza §e"
                    + Bukkit.getOfflinePlayer(island.getOwner()).getName() + "§c.");
        }
    }

    public void unbanPlayer(Player executor, UUID targetUUID) {
        Island island = islandsByOwner.get(executor.getUniqueId());
        if (island == null) island = playerToIsland.get(executor.getUniqueId());
        if (island == null) {
            executor.sendMessage("§cNie należysz do żadnej wyspy!");
            return;
        }

        IslandRole role = island.getRole(executor.getUniqueId());
        if (role == null || !role.canBan()) {
            executor.sendMessage("§cTylko właściciel i Co-Leader mogą odbanowywać graczy!");
            return;
        }

        if (!island.isBanned(targetUUID)) {
            executor.sendMessage("§cTen gracz nie jest zbanowany.");
            return;
        }

        island.unbanPlayer(targetUUID);
        storage.saveIsland(island, IslandType.OVERWORLD);

        String name = Bukkit.getOfflinePlayer(targetUUID).getName();
        executor.sendMessage("§aGracz §e" + (name != null ? name : targetUUID) + " §azostał odbanowany.");
    }

    // -------------------------------------------------------------------------
    // Zarządzanie rolami
    // -------------------------------------------------------------------------

    public void setCoLeader(Player owner, UUID targetUUID) {
        // Sprawdź czy wykonujący jest właścicielem
        Island island = islandsByOwner.get(owner.getUniqueId());
        if (island == null) {
            owner.sendMessage("§cTylko właściciel wyspy może nadawać rolę Co-Leader!");
            return;
        }

        if (!island.isMember(targetUUID)) {
            owner.sendMessage("§cTen gracz nie jest członkiem Twojej wyspy!");
            return;
        }

        // Nie można nadać Co-Leadera samemu sobie
        if (targetUUID.equals(owner.getUniqueId())) {
            owner.sendMessage("§cNie możesz nadać sobie tej roli!");
            return;
        }

        String name = Bukkit.getOfflinePlayer(targetUUID).getName();
        if (name == null) name = targetUUID.toString().substring(0, 8);

        if (island.getRole(targetUUID) == IslandRole.CO_LEADER) {
            // Odbieramy rolę
            island.setRole(targetUUID, IslandRole.MEMBER);
            storage.saveIsland(island, IslandType.OVERWORLD);
            owner.sendMessage("§7Gracz §e" + name + " §7stracił rolę §eCo-Leader§7.");
            Player tp = Bukkit.getPlayer(targetUUID);
            if (tp != null) tp.sendMessage("§7Twoja rola na wyspie została zmieniona na §7Członek§7.");
        } else {
            // Nadajemy rolę
            island.setRole(targetUUID, IslandRole.CO_LEADER);
            storage.saveIsland(island, IslandType.OVERWORLD);
            owner.sendMessage("§aGracz §e" + name + " §aotrzymał rolę §eCo-Leader§a!");
            Player tp = Bukkit.getPlayer(targetUUID);
            if (tp != null) tp.sendMessage("§aOtrzymałeś rolę §eCo-Leader §ana wyspie!");
        }
    }

    /**
     * Szukamy właściciela po nazwie tylko wśród załadowanych wysp — bez odpytywania Mojang API.
     * Dzięki temu nie blokujemy głównego wątku.
     */
    public void teleportToIsland(Player visitor, String targetOwnerName) {
        // Szukamy wyspy po nazwie właściciela wśród załadowanych danych (bez Bukkit.getOfflinePlayer)
        Island targetIsland = null;
        UUID targetOwnerUUID = null;

        for (Map.Entry<UUID, Island> entry : islandsByOwner.entrySet()) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            if (targetOwnerName.equalsIgnoreCase(op.getName())) {
                targetIsland = entry.getValue();
                targetOwnerUUID = entry.getKey();
                break;
            }
        }

        if (targetIsland == null) {
            visitor.sendMessage("§cNie znaleziono gracza o takiej nazwie lub nie posiada wyspy.");
            return;
        }

        // Właściciel lub członek — standardowy home
        if (targetIsland.isMember(visitor.getUniqueId())) {
            teleportHome(visitor);
            return;
        }

        // Sprawdź ban
        if (targetIsland.isBanned(visitor.getUniqueId())) {
            visitor.sendMessage("§cJesteś zbanowany na tej wyspie.");
            return;
        }

        // Wyspa prywatna
        if (!targetIsland.canVisitorDo(IslandAction.TELEPORT_VISIT)) {
            visitor.sendMessage("§cTa wyspa jest prywatna. Właściciel zablokował odwiedziny.");
            return;
        }

        if (!isLocationSafe(targetIsland.getHome())) {
            visitor.sendMessage("§cPunkt domowy tej wyspy jest niebezpieczny. Nie można się przeteleportować.");
            return;
        }

        visitor.teleport(targetIsland.getHome());
        visitor.sendMessage("§aTeleportowano na wyspę gracza §e" + targetOwnerName);

        Player ownerOnline = Bukkit.getPlayer(targetOwnerUUID);
        if (ownerOnline != null) {
            ownerOnline.sendMessage("§7Gracz §e" + visitor.getName() + " §7odwiedził Twoją wyspę.");
        }
    }

    // -------------------------------------------------------------------------
    // Lokalizacja
    // -------------------------------------------------------------------------

    public boolean isOnOwnIsland(Player player, Location location) {
        Island island = getIsland(player.getUniqueId());
        if (island == null) return false;
        return island.isInside(location);
    }

    public Island getIslandAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        String worldName = location.getWorld().getName();

        // OVERWORLD
        if (IslandType.OVERWORLD.worldName.equals(worldName)) {
            Island island = islandGrid.get(getGridKey(location));
            if (island != null && island.isInside(location)) return island;
            return null;
        }

        // Typy dodatkowe
        for (IslandType type : IslandType.values()) {
            if (type == IslandType.OVERWORLD) continue;
            if (type.worldName.equals(worldName)) {
                Map<Long, Island> grid = secondaryGrids.get(type);
                if (grid == null) return null;
                Island island = grid.get(getGridKey(location));
                if (island != null && island.isInside(location)) return island;
                return null;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Zaproszenia
    // -------------------------------------------------------------------------

    public void sendInvite(Player sender, Player target) {
        // Kolejność sprawdzeń: najpierw warunki niezależne od obiektu island (unikamy NPE)
        if (sender.equals(target)) {
            sender.sendMessage("§cNie możesz zaprosić samego siebie!");
            return;
        }

        if (!isOwner(sender.getUniqueId())) {
            sender.sendMessage("§cTylko lider wyspy może zapraszać graczy!");
            return;
        }

        Island island = islandsByOwner.get(sender.getUniqueId());
        if (island == null) {
            sender.sendMessage("§cNie masz wyspy!");
            return;
        }

        if (island.getMembers().size() >= 4) {
            sender.sendMessage("§cTwoja wyspa jest pełna! Maksymalna liczba członków to 4.");
            return;
        }

        if (hasIsland(target.getUniqueId())) {
            sender.sendMessage("§cTen gracz ma już wyspę!");
            return;
        }

        if (pendingInvites.containsKey(target.getUniqueId())) {
            sender.sendMessage("§cTen gracz ma już oczekujące zaproszenie!");
            return;
        }

        pendingInvites.put(target.getUniqueId(), sender.getUniqueId());
        stark.skyBlockTest2.util.ChatUtil.sendInviteMessage(target, sender.getName());
        sender.sendMessage("§aWysłano zaproszenie do gracza " + target.getName());

        // Auto-usunięcie po 60 sekundach
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.remove(target.getUniqueId()) != null) {
                if (target.isOnline()) target.sendMessage("§cZaproszenie od " + sender.getName() + " wygasło.");
                if (sender.isOnline()) sender.sendMessage("§cZaproszenie dla " + target.getName() + " wygasło.");
            }
        }, 1200L);
    }

    public void acceptInvite(Player player) {
        UUID playerUUID = player.getUniqueId();
        UUID ownerUUID  = pendingInvites.remove(playerUUID);

        if (ownerUUID == null) {
            player.sendMessage("§cNie masz żadnych oczekujących zaproszeń!");
            return;
        }

        Island island = islandsByOwner.get(ownerUUID);
        if (island == null) {
            player.sendMessage("§cWyspa już nie istnieje.");
            return;
        }

        island.getMembers().add(playerUUID);
        island.getMemberRoles().remove(playerUUID);  // upewnij się że zaczyna jako MEMBER
        playerToIsland.put(playerUUID, island);
        storage.saveIsland(island, IslandType.OVERWORLD);

        player.sendMessage("§aDołączyłeś do wyspy!");
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) owner.sendMessage("§e" + player.getName() + " §adołączył do Twojej wyspy!");
    }

    public void declineInvite(Player player) {
        if (pendingInvites.remove(player.getUniqueId()) != null) {
            player.sendMessage("§cOdrzuciłeś zaproszenie.");
        } else {
            player.sendMessage("§cNie masz zaproszeń do odrzucenia.");
        }
    }

    // -------------------------------------------------------------------------
    // Zarządzanie członkami
    // -------------------------------------------------------------------------

    public void kickMember(Player owner, UUID targetUUID) {
        if (!isOwner(owner.getUniqueId())) {
            owner.sendMessage("§cTylko lider może wyrzucać członków!");
            return;
        }

        Island island = islandsByOwner.get(owner.getUniqueId());
        if (island == null) return;

        if (!island.getMembers().contains(targetUUID)) {
            owner.sendMessage("§cTen gracz nie jest członkiem Twojej wyspy!");
            return;
        }

        island.getMembers().remove(targetUUID);
        island.getMemberRoles().remove(targetUUID);  // resetuj rolę przy kicku
        playerToIsland.remove(targetUUID);
        storage.saveIsland(island, IslandType.OVERWORLD);

        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
        owner.sendMessage("§aWyrzuciłeś gracza " + (targetName != null ? targetName : "Nieznany") + " z wyspy.");

        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null) {
            targetPlayer.sendMessage("§cZostałeś wyrzucony z wyspy!");
            // Teleportuj tylko jeśli stoi na tej wyspie
            if (island.isInside(targetPlayer.getLocation())) {
                teleportManager.teleportWithoutDelay(targetPlayer);
            }
        }
    }

    public void leaveIsland(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (!hasIsland(playerUUID)) {
            player.sendMessage("§cNie należysz do żadnej wyspy!");
            return;
        }

        if (isOwner(playerUUID)) {
            player.sendMessage("§cJesteś liderem! Aby zlikwidować wyspę, użyj §e/island delete§c.");
            return;
        }

        Island island = getIsland(playerUUID);
        island.getMembers().remove(playerUUID);
        island.getMemberRoles().remove(playerUUID);  // resetuj rolę przy opuszczeniu
        playerToIsland.remove(playerUUID);
        storage.saveIsland(island, IslandType.OVERWORLD);

        player.sendMessage("§aOpuściłeś wyspę.");
        // Teleportuj tylko jeśli stoi na tej wyspie
        if (island.isInside(player.getLocation())) {
            teleportManager.teleportWithoutDelay(player);
        }

        Player ownerPlayer = Bukkit.getPlayer(island.getOwner());
        if (ownerPlayer != null) {
            ownerPlayer.sendMessage("§eGracz " + player.getName() + " opuścił Twoją wyspę.");
        }
    }

    public void showMembers(Player player) {
        Island island = getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage("§cNie należysz do żadnej wyspy!");
            return;
        }

        player.sendMessage(" ");
        player.sendMessage("§8§m-------§r §6§lCzłonkowie Wyspy §8§m-------");

        org.bukkit.OfflinePlayer ownerOp = Bukkit.getOfflinePlayer(island.getOwner());
        String ownerStatus = ownerOp.isOnline() ? "§a●" : "§c●";
        player.sendMessage("§7Właściciel: " + ownerStatus + " §e" + ownerOp.getName());

        if (island.getMembers().isEmpty()) {
            player.sendMessage("§7Członkowie: §8Brak");
        } else {
            player.sendMessage("§7Członkowie:");
            for (UUID memberUUID : island.getMembers()) {
                org.bukkit.OfflinePlayer member = Bukkit.getOfflinePlayer(memberUUID);
                String status = member.isOnline() ? "§a●" : "§c●";
                player.sendMessage("  §8» " + status + " §7" + member.getName());
            }
        }

        player.sendMessage("§8§m-----------------------------");
        player.sendMessage(" ");
    }

    public void transferOwnership(Player currentOwner, UUID newOwnerUUID) {
        UUID oldOwnerUUID = currentOwner.getUniqueId();

        if (!isOwner(oldOwnerUUID)) {
            currentOwner.sendMessage("§cTylko lider może przekazać wyspę!");
            return;
        }

        Island island = islandsByOwner.get(oldOwnerUUID);
        if (island == null) return;

        if (!island.getMembers().contains(newOwnerUUID)) {
            currentOwner.sendMessage("§cMożesz przekazać lidera tylko członkowi swojej wyspy.");
            return;
        }

        // Stary właściciel staje się członkiem, nowy właścicielem
        island.getMembers().remove(newOwnerUUID);
        island.getMembers().add(oldOwnerUUID);
        island.setOwner(newOwnerUUID);

        islandsByOwner.remove(oldOwnerUUID);
        islandsByOwner.put(newOwnerUUID, island);

        playerToIsland.put(newOwnerUUID, island);
        playerToIsland.put(oldOwnerUUID, island);

        // Zapis po wszystkich zmianach w pamięci (nie przed, jak było wcześniej)
        storage.saveIsland(island, IslandType.OVERWORLD);

        String newOwnerName = Bukkit.getOfflinePlayer(newOwnerUUID).getName();
        currentOwner.sendMessage("§aPrzekazałeś lidera graczowi §e" + (newOwnerName != null ? newOwnerName : "Nieznany"));

        Player newOwnerPlayer = Bukkit.getPlayer(newOwnerUUID);
        if (newOwnerPlayer != null) {
            newOwnerPlayer.sendMessage("§aZostałeś nowym liderem wyspy!");
        }
    }

    // -------------------------------------------------------------------------
    // Ulepszanie wyspy
    // -------------------------------------------------------------------------

    public void upgradeIslandSize(Player player) {
        Island island = islandsByOwner.get(player.getUniqueId());
        if (island == null) {
            player.sendMessage("§cTylko właściciel może ulepszać wyspę!");
            return;
        }

        int currentSize = island.getSize();
        int nextSize    = -1;
        int nextLevel   = -1;

        for (int i = 0; i < islandSizes.length; i++) {
            if (islandSizes[i] > currentSize) {
                nextSize  = islandSizes[i];
                nextLevel = i + 1;
                break;
            }
        }

        if (nextSize == -1) {
            player.sendMessage("§cOsiągnąłeś już maksymalny rozmiar wyspy!");
            return;
        }

        // Sprawdzenie i pobranie opłaty
        if (economyManager != null && economyManager.isAvailable()) {
            double cost = getUpgradeCost(nextLevel);
            if (!economyManager.has(player, cost)) {
                player.sendMessage("§cNie masz wystarczających środków! Potrzebujesz §e"
                        + economyManager.format(cost) + "§c.");
                return;
            }
            economyManager.withdraw(player, cost);
            player.sendMessage("§7Pobrano §e" + economyManager.format(cost) + "§7.");
        }

        island.setSize(nextSize);
        storage.saveIsland(island, IslandType.OVERWORLD);
        updateBorderForEveryoneOnIsland(island);
        player.sendMessage("§aTwoja wyspa została powiększona do rozmiaru §e"
                + (nextSize * 2 + 1) + "x" + (nextSize * 2 + 1) + "§a!");
    }

    /**
     * Ulepsza wyspę do konkretnego poziomu — wywoływane z GUI.
     * Sprawdza czy to faktycznie następny poziom (nie można przeskakiwać).
     */
    public void upgradeIslandToLevel(Player player, int targetLevel) {
        Island island = islandsByOwner.get(player.getUniqueId());
        if (island == null) {
            player.sendMessage("§cTylko właściciel może ulepszać wyspę!");
            return;
        }

        // Obecny poziom gracza
        int currentLevel = 1;
        for (int i = 0; i < islandSizes.length; i++) {
            if (islandSizes[i] == island.getSize()) { currentLevel = i + 1; break; }
        }

        // Można kupić tylko następny poziom
        if (targetLevel != currentLevel + 1) {
            player.sendMessage("§cMożesz ulepszyć wyspę tylko o jeden poziom na raz!");
            return;
        }

        if (targetLevel < 1 || targetLevel > islandSizes.length) {
            player.sendMessage("§cNieprawidłowy poziom!");
            return;
        }

        // Sprawdzenie ekonomii
        if (economyManager != null && economyManager.isAvailable()) {
            double cost = getUpgradeCost(targetLevel);
            if (!economyManager.has(player, cost)) {
                player.sendMessage("§cNie masz wystarczających środków! Potrzebujesz §e"
                        + economyManager.format(cost) + "§c.");
                return;
            }
            economyManager.withdraw(player, cost);
            player.sendMessage("§7Pobrano §e" + economyManager.format(cost) + "§7.");
        }

        int newSize = islandSizes[targetLevel - 1];
        island.setSize(newSize);
        storage.saveIsland(island, IslandType.OVERWORLD);
        updateBorderForEveryoneOnIsland(island);
        player.sendMessage("§aTwoja wyspa została powiększona do poziomu §e" + targetLevel
                + " §a(" + (newSize * 2 + 1) + "x" + (newSize * 2 + 1) + ")§a!");
    }

    /**
     * Klucz: island.upgrade-costs.<level>
     */
    public double getUpgradeCost(int level) {
        return getUpgradeCost(IslandType.OVERWORLD, level);
    }

    public void setIslandLevel(Player player, int level) {
        if (level < 1 || level > 5) return;

        Island island = getIsland(player.getUniqueId());
        if (island == null) return;

        island.setSize(islandSizes[level - 1]);
        storage.saveIsland(island, IslandType.OVERWORLD);
        updateBorderForEveryoneOnIsland(island);
    }

    private void updateBorderForEveryoneOnIsland(Island island) {
        if (borderManager == null) return;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // Island.equals() działa teraz poprawnie (po index) — porównanie jest wiarygodne
            if (island.equals(getIslandAt(onlinePlayer.getLocation()))) {
                borderManager.updateBorder(onlinePlayer);
            }
        }
    }

    // -------------------------------------------------------------------------
    // API dla typów dodatkowych (NETHER, END, ...)
    // -------------------------------------------------------------------------

    public boolean hasIsland(UUID uuid, IslandType type) {
        if (type == IslandType.OVERWORLD) return hasIsland(uuid);
        Map<UUID, Island> map = secondaryIslands.get(type);
        return map != null && map.containsKey(uuid);
    }

    public Island getIsland(UUID uuid, IslandType type) {
        if (type == IslandType.OVERWORLD) return islandsByOwner.get(uuid);
        Map<UUID, Island> map = secondaryIslands.get(type);
        return map != null ? map.get(uuid) : null;
    }

    public int getCurrentLevel(UUID uuid, IslandType type) {
        Island island = getIsland(uuid, type);
        if (island == null) return 0;
        for (int i = 0; i < islandSizes.length; i++) {
            if (islandSizes[i] == island.getSize()) return i + 1;
        }
        return 1;
    }

    public double getPurchaseCost(IslandType type) {
        return plugin.getConfig().getDouble(type.configPath + ".purchase-cost", 10000.0);
    }

    public double getUpgradeCost(IslandType type, int level) {
        return plugin.getConfig().getDouble(type.configPath + ".upgrade-costs." + level, 0.0);
    }

    public void createIsland(Player player, IslandType type) {
        if (type == IslandType.OVERWORLD) { createIsland(player); return; }

        if (!hasIsland(player.getUniqueId())) {
            player.sendMessage("§cMusisz najpierw posiadać zwykłą wyspę!");
            return;
        }
        if (hasIsland(player.getUniqueId(), type)) {
            player.sendMessage("§cMasz już " + type.displayName + "!");
            return;
        }

        double cost = getPurchaseCost(type);
        if (economyManager != null && economyManager.isAvailable() && cost > 0) {
            if (!economyManager.has(player, cost)) {
                player.sendMessage("§cNie masz wystarczających środków! Potrzebujesz §e"
                        + economyManager.format(cost) + "§c.");
                return;
            }
            economyManager.withdraw(player, cost);
            player.sendMessage("§7Pobrano §e" + economyManager.format(cost) + "§7.");
        }

        World world = plugin.getWorldManager().getWorld(type);
        if (world == null) {
            player.sendMessage("§cŚwiat " + type.displayName + " nie jest dostępny!");
            return;
        }

        List<Integer> free = secondaryFreeIdxs.get(type);
        int index;
        if (!free.isEmpty()) {
            index = free.removeFirst();
        } else {
            index = secondaryIndexes.get(type);
            secondaryIndexes.put(type, index + 1);
        }
        storage.saveIndexState(type, secondaryIndexes.get(type), free);

        int[] spiral = SpiralCalculator.getSpiralPosition(index);
        int x = (spiral[0] * SPACING_CHUNKS) << 4;
        int z = (spiral[1] * SPACING_CHUNKS) << 4;
        Location center = new Location(world, x + 8, 100, z + 8);

        Island island = new Island(player.getUniqueId(), center, 0, index, new ArrayList<>());
        island.setHome(center.clone().add(0.5, 0.1, 0.5));

        secondaryIslands.get(type).put(player.getUniqueId(), island);
        secondaryGrids.get(type).put(getGridKey(center), island);

        center.getChunk().load(true);
        storage.saveIsland(island, type);

        player.sendMessage("§7Tworzenie " + type.displayName + ", poczekaj chwilę...");
        plugin.getSchematicManager().pasteSchematic(center, "Default_Island", () -> {
            player.teleport(island.getHome());
            player.sendMessage("§a" + type.displayName + " została utworzona!");
        });
    }

    public void teleportHome(Player player, IslandType type) {
        if (type == IslandType.OVERWORLD) { teleportHome(player); return; }

        Island island = getIsland(player.getUniqueId(), type);
        if (island == null) {
            player.sendMessage("§cNie masz " + type.displayName + "!");
            return;
        }
        if (!isLocationSafe(island.getHome())) {
            player.sendMessage("§cTwój dom " + type.displayName
                    + " jest uszkodzony! Użyj §e/is sethome " + type.name().toLowerCase());
            return;
        }
        player.teleport(island.getHome());
        player.sendMessage("§aTeleportowano na dom " + type.displayName + ".");
    }

    public void setHome(Player player, IslandType type) {
        if (type == IslandType.OVERWORLD) { setHome(player); return; }

        Island island = getIsland(player.getUniqueId(), type);
        if (island == null) {
            player.sendMessage("§cNie masz " + type.displayName + "!");
            return;
        }
        if (!island.isInside(player.getLocation())) {
            player.sendMessage("§cMożesz ustawić dom tylko na terenie własnej " + type.displayName + "!");
            return;
        }
        if (!isLocationSafe(player.getLocation())) {
            player.sendMessage("§cNie możesz tu ustawić domu — miejsce jest niebezpieczne!");
            return;
        }
        island.setHome(player.getLocation());
        storage.saveIsland(island, type);
        player.sendMessage("§aPunkt domowy " + type.displayName + " został ustawiony!");
    }

    public void upgradeIslandToLevel(Player player, IslandType type, int targetLevel) {
        if (type == IslandType.OVERWORLD) { upgradeIslandToLevel(player, targetLevel); return; }

        Island island = getIsland(player.getUniqueId(), type);
        if (island == null) {
            player.sendMessage("§cNie masz " + type.displayName + "!");
            return;
        }

        int currentLevel = getCurrentLevel(player.getUniqueId(), type);
        if (targetLevel != currentLevel + 1) {
            player.sendMessage("§cMożesz ulepszyć wyspę tylko o jeden poziom na raz!");
            return;
        }
        if (targetLevel < 1 || targetLevel > islandSizes.length) {
            player.sendMessage("§cNieprawidłowy poziom!");
            return;
        }

        if (economyManager != null && economyManager.isAvailable()) {
            double cost = getUpgradeCost(type, targetLevel);
            if (!economyManager.has(player, cost)) {
                player.sendMessage("§cNie masz wystarczających środków! Potrzebujesz §e"
                        + economyManager.format(cost) + "§c.");
                return;
            }
            economyManager.withdraw(player, cost);
            player.sendMessage("§7Pobrano §e" + economyManager.format(cost) + "§7.");
        }

        int newSize = islandSizes[targetLevel - 1];
        island.setSize(newSize);
        storage.saveIsland(island, type);
        updateBorderForEveryoneOnIsland(island);
        player.sendMessage("§a" + type.displayName + " powiększona do poziomu §e" + targetLevel
                + " §a(" + (newSize * 2 + 1) + "x" + (newSize * 2 + 1) + ")§a!");
    }

    // -------------------------------------------------------------------------
    // Skan przy starcie — czyszczenie osieroconych obszarów
    // -------------------------------------------------------------------------

    public void performStartupScan() {
        plugin.getLogger().info("[SkyBlock] Rozpoczynanie skanowania indeksów wysp...");

        Set<Integer> occupiedIndexes = new HashSet<>();
        for (Island island : islandsByOwner.values()) {
            occupiedIndexes.add(island.getIndex());
        }

        List<Island> orphans = new ArrayList<>();
        for (int i = 0; i < currentIndex; i++) {
            if (!occupiedIndexes.contains(i) && !freeIndexes.contains(i)) {
                int[] spiral = SpiralCalculator.getSpiralPosition(i);
                int x = (spiral[0] * SPACING_CHUNKS) << 4;
                int z = (spiral[1] * SPACING_CHUNKS) << 4;
                Location center = new Location(plugin.getWorldManager().getWorld(), x + 8, 100, z + 8);
                orphans.add(new Island(null, center, 0, i, new ArrayList<>()));
            }
        }

        if (orphans.isEmpty()) return;

        plugin.getLogger().info("[SkyBlock] Znaleziono " + orphans.size()
                + " osieroconych obszarów. Czyszczenie zostanie uruchomione za chwilę.");

        // Opóźnienie 1 tick — serwer zdąży dokończyć ładowanie przed czyszczeniem bloków
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Island dummy : orphans) {
                generator.clearIsland(dummy, freeIndexes,
                        () -> storage.saveIndexState(IslandType.OVERWORLD, currentIndex, freeIndexes));
            }
            plugin.getLogger().info("[SkyBlock] Naprawiono " + orphans.size() + " osieroconych obszarów.");
        }, 1L);
    }
}