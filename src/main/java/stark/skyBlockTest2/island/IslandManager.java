package stark.skyBlockTest2.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import stark.skyBlockTest2.SkyBlockTest2;
import stark.skyBlockTest2.Spawn.TeleportManager;
import stark.skyBlockTest2.border.BorderManager;
import stark.skyBlockTest2.island.listener.IslandProtectionListener;


import java.util.*;

public class IslandManager {

    private final SkyBlockTest2 plugin;
    private final Map<UUID, Island> playerToIsland = new HashMap<>();
    private final Map<UUID, Island> islandsByOwner = new HashMap<>();
    private final Map<Long, Island> islandGrid = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final IslandGenerator generator = new IslandGenerator();
    private final IslandStorage storage;
    private final TeleportManager teleportManager;
    private BorderManager borderManager;


    private final int[] islandSizes = {0, 1, 2, 3, 4};
    private final int SPACING_CHUNKS = 10;

    private int currentIndex;
    private final List<Integer> freeIndexes = new ArrayList<>();

    public void setBorderManager(BorderManager borderManager) {
        this.borderManager = borderManager;
    }

    public IslandManager(SkyBlockTest2 plugin, TeleportManager teleportManager) {
        this.plugin = plugin;
        this.storage = new IslandStorage(plugin);
        this.teleportManager = teleportManager;
        this.currentIndex = storage.getCurrentIndex();
        this.freeIndexes.addAll(storage.getFreeIndexes());

        Map<UUID, Island> loaded = storage.loadIslands();
        islandsByOwner.putAll(loaded);
        for (Island island : loaded.values()) {
            // Dodaj właściciela
            playerToIsland.put(island.getOwner(), island);
            registerIslandInMemory(island);

            // Dodaj wszystkich członków
            for (UUID memberUuid : island.getMembers()) {
                playerToIsland.put(memberUuid, island);
            }
        }

    }

    public boolean hasIsland(UUID uuid) {
        return playerToIsland.containsKey(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return islandsByOwner.containsKey(uuid);
    }

    public Island getIsland(UUID uuid) {
        return playerToIsland.get(uuid);
    }

    public IslandStorage getStorage() { return this.storage; }

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
            storage.setCurrentIndex(currentIndex);
        }

        storage.setFreeIndexes(freeIndexes);
        int[] spiral = SpiralCalculator.getSpiralPosition(index);

        int x = (spiral[0] * SPACING_CHUNKS) <<4;
        int z = (spiral[1] * SPACING_CHUNKS) <<4;
        Location center = new Location(world, x + 8, 100, z + 8);

        Island island = new Island(player.getUniqueId(), center, 0, index, new ArrayList<>());
        island.setHome(center.clone().add(0.5, 0.1, 0.5));

        islandsByOwner.put(player.getUniqueId(), island);
        registerIslandInMemory(island); // Rejestrujemy wszędzie

        center.getChunk().load(true);

        plugin.getSchematicManager().pasteSchematic(center, "Default_Island");
        storage.saveIsland(island);

        player.teleport(center.clone().add(0.5, 0, 0.5));
        player.sendMessage("§aWyspa została utworzona!");
    }

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
            player.sendMessage("nie możesz tu ustawić domu");
            return;
        }
            island.setHome(player.getLocation());
            storage.saveIsland(island);
            player.sendMessage("§aPunkt domowy wyspy został ustawiony!");
    }

    private boolean isLocationSafe(Location loc) {
        Block ground = loc.clone().subtract(0, 1, 0).getBlock();
        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();

        return ground.getType().isSolid() &&
                feet.getType().isAir() &&
                head.getType().isAir();
    }

    public void teleportHome(Player player) {
        Island island = getIsland(player.getUniqueId());

        if (island == null) {
            player.sendMessage("§cNie należysz do żadnej wyspy!");
            return;
        }
        if(!isLocationSafe(island.getHome())) {
            player.sendMessage("§cTwój dom wyspy jest uszkodzony! napraw go komendą /is repairhome");
            return;
        }
        player.teleport(island.getHome());
        player.sendMessage("§aTeleportowano na dom wyspy.");
    }

    public void repairHomeToNearest(Island island) {
        Location currentHome = island.getHome();
        Location center = island.getCenter();

        // 1. Zdefiniuj zakresy
        int maxR = 10;
        int maxYOffset = 20;

        // Przeszukujemy warstwy Y (od najbliższej obecnej wysokości)
        for (int yOffset = 0; yOffset <= maxYOffset; yOffset++) {
            // Generujemy listę Y do sprawdzenia w tej iteracji (góra i dół)
            int[] yCheck = (yOffset == 0) ? new int[]{currentHome.getBlockY()}
                    : new int[]{currentHome.getBlockY() + yOffset, currentHome.getBlockY() - yOffset};

            for (int y : yCheck) {
                if (y < 0 || y > 255) continue;

                // 2. Szukanie w kwadratach (zamiast powielać sprawdzanie środka)
                for (int r = 0; r <= maxR; r++) {
                    for (int x = -r; x <= r; x++) {
                        for (int z = -r; z <= r; z++) {
                            // Klucz optymalizacji: sprawdzaj tylko brzegi "kwadratu" o promieniu r
                            // aby nie sprawdzać ponownie środka, który był sprawdzony dla r-1
                            if (Math.abs(x) == r || Math.abs(z) == r) {

                                Location checkLoc = new Location(currentHome.getWorld(),
                                        currentHome.getBlockX() + x, y, currentHome.getBlockZ() + z);

                                // 3. Sprawdzenie granic wyspy
                                if (isWithinIsland(checkLoc, island)) {
                                    if (isLocationSafe(checkLoc)) {
                                        setAndSaveHome(island, checkLoc, currentHome.getYaw(), currentHome.getPitch());
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Emergency Fallback (jeśli nie znaleziono nic bezpiecznego)
        handleEmergencyHome(island, center);
    }

    private boolean isWithinIsland(Location loc, Island island) {
        return island.isInside(loc);
    }

    private void setAndSaveHome(Island island, Location loc, float yaw, float pitch) {
        Location newHome = loc.clone().add(0.5, 0.1, 0.5);
        newHome.setYaw(yaw);
        newHome.setPitch(pitch);
        island.setHome(newHome);
        storage.saveIsland(island);
    }

    private void handleEmergencyHome(Island island, Location center) {
        // Ustawiamy wysokość awaryjną (np. 100 lub średnia wysokość wysp na serwerze)
        Location emergencyLoc = center.clone();
        emergencyLoc.setY(100);

        // Pobieramy blok pod nogami (Y = 99)
        Block blockBelow = emergencyLoc.clone().subtract(0, 1, 0).getBlock();

        // Jeśli pod nogami jest powietrze lub niebezpieczny blok (np. lawa), stawiamy szkło
        if (blockBelow.isEmpty() || blockBelow.isLiquid()) {
            blockBelow.setType(org.bukkit.Material.GLASS);
        }

        // Centrujemy gracza na bloku (0.5) i lekko podnosimy (0.1), by nie utknął w podłożu
        Location finalHome = emergencyLoc.add(0.5, 0.1, 0.5);

        island.setHome(finalHome);
        storage.saveIsland(island);
    }

    public void deleteIsland(Player player) {
        UUID ownerUUID = player.getUniqueId();
        Island island = islandsByOwner.remove(ownerUUID);

        if (island == null) {
            player.sendMessage("§cNie jesteś właścicielem żadnej wyspy!");
            return;
        }

        // Usuwamy z pamięci
        playerToIsland.remove(ownerUUID);
        for (UUID memberUUID : island.getMembers()) {
            playerToIsland.remove(memberUUID);
        }
        islandGrid.remove(getGridKey(island.getCenter())); // Usuwamy z siatki!

        teleportEveryoneFromIsland(island);
        generator.clearIsland(island, freeIndexes, storage);
        storage.deleteIsland(ownerUUID);

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

    public void teleportToIsland(Player visitor, String targetOwnerName) {
        // 1. Znajdź gracza docelowego (właściciela)
        org.bukkit.OfflinePlayer targetOwner = Bukkit.getOfflinePlayer(targetOwnerName);

        if (!targetOwner.hasPlayedBefore() && !targetOwner.isOnline()) {
            visitor.sendMessage("§cNie znaleziono gracza o takiej nazwie.");
            return;
        }

        // 2. Pobierz wyspę tego gracza
        Island targetIsland = islandsByOwner.get(targetOwner.getUniqueId());

        if (targetIsland == null) {
            visitor.sendMessage("§cTen gracz nie posiada wyspy.");
            return;
        }

        // 3. Sprawdź, czy gracz nie próbuje odwiedzić własnej wyspy (opcjonalne)
        if (targetIsland.isMember(visitor.getUniqueId())) {
            teleportHome(visitor); // Jeśli swój, używamy standardowego home
            return;
        }

        // 4. KLUCZOWE: Sprawdzenie ustawień GUI
        if (!targetIsland.canVisitorDo(IslandProtectionListener.IslandAction.TELEPORT_VISIT)) {
            visitor.sendMessage("§cTa wyspa jest obecnie prywatna. Właściciel zablokował odwiedziny.");
            return;
        }

        // 5. Sprawdzenie bezpieczeństwa punktu Home
        if (!isLocationSafe(targetIsland.getHome())) {
            visitor.sendMessage("§cPunkt domowy tej wyspy jest obecnie niebezpieczny. Nie można się przeteleportować.");
            return;
        }

        // 6. Teleportacja
        visitor.teleport(targetIsland.getHome());
        visitor.sendMessage("§aTeleportowano na wyspę gracza §e" + targetOwner.getName());

        // Powiadomienie właściciela (jeśli online)
        Player ownerOnline = Bukkit.getPlayer(targetOwner.getUniqueId());
        if (ownerOnline != null && ownerOnline.isOnline()) {
            ownerOnline.sendMessage("§7Gracz §e" + visitor.getName() + " §7odwiedził Twoją wyspę.");
        }
    }

    public boolean isOnOwnIsland(Player player, Location location) {
        Island island = getIsland(player.getUniqueId());
        if (island == null) return false;
        return island.isInside(location);
    }

    public Island getIslandAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        if (!location.getWorld().equals(plugin.getWorldManager().getWorld())) return null;

        Island island = islandGrid.get(getGridKey(location));

        if (island != null && island.isInside(location)) {
            return island;
        }
        return null;
    }

    public void sendInvite(Player sender, Player target) {
        UUID senderUUID = sender.getUniqueId();
        Island island = islandsByOwner.get(senderUUID);
        if (sender.equals(target)) {
            sender.sendMessage("§cNie możesz zaprosić samego siebie!");
            return;
        }

        if (island.getMembers().size() >= 4) { // Przykład: lider + 4 członków = 5 osób max
            sender.sendMessage("§cTwoja wyspa jest pełna! Maksymalna liczba członków to 4.");
            return;
        }

        if (!isOwner(sender.getUniqueId())) {
            sender.sendMessage("§cTylko lider wyspy może zapraszać graczy!");
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

        // Auto-usuwanie zaproszenia po 60 sekundach
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.remove(target.getUniqueId()) != null) {
                if (target.isOnline()) target.sendMessage("§cZaproszenie od " + sender.getName() + " wygasło.");
                if (sender.isOnline()) sender.sendMessage("§cZaproszenie dla " + target.getName() + " wygasło.");
            }
        }, 1200L); // 20 ticks * 60 seconds
    }

    public void acceptInvite(Player player) {
        UUID playerUUID = player.getUniqueId();
        UUID ownerUUID = pendingInvites.remove(playerUUID);

        if (ownerUUID == null) {
            player.sendMessage("§cNie masz żadnych oczekujących zaproszeń!");
            return;
        }

        Island island = islandsByOwner.get(ownerUUID);
        if (island == null) {
            player.sendMessage("§cWyspa już nie istnieje.");
            return;
        }

        // Dodanie do danych wyspy i mapy uprawnień
        island.getMembers().add(playerUUID);
        playerToIsland.put(playerUUID, island);

        // Zapis do pliku YAML
        storage.saveIsland(island);

        player.sendMessage("§aDołączyłeś do wyspy!");
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) owner.sendMessage("§e" + player.getName() + " §adołączył do Twojej wyspy!");
    }

    // Metoda odrzucenia
    public void declineInvite(Player player) {
        if (pendingInvites.remove(player.getUniqueId()) != null) {
            player.sendMessage("§cOdrzuciłeś zaproszenie.");
        } else {
            player.sendMessage("§cNie masz zaproszeń do odrzucenia.");
        }
    }

    public void kickMember(Player owner, UUID targetUUID) {
        UUID ownerUUID = owner.getUniqueId();

        if (!isOwner(ownerUUID)) {
            owner.sendMessage("§cTylko lider może wyrzucać członków!");
            return;
        }

        Island island = islandsByOwner.get(ownerUUID);
        if (island == null) return;

        if (!island.getMembers().contains(targetUUID)) {
            owner.sendMessage("§cTen gracz nie jest członkiem Twojej wyspy!");
            return;
        }

        island.getMembers().remove(targetUUID);
        playerToIsland.remove(targetUUID);

        storage.saveIsland(island);

        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
        owner.sendMessage("§aWyrzuciłeś gracza " + (targetName != null ? targetName : "Nieznany") + " z wyspy.");

        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage("§cZostałeś wyrzucony z wyspy!");
            teleportManager.teleportWithoutDelay(targetPlayer);
        }
    }

    public void leaveIsland(Player player) {
        UUID playerUUID = player.getUniqueId();

        // 1. Sprawdzamy czy w ogóle ma wyspę
        if (!hasIsland(playerUUID)) {
            player.sendMessage("§cNie należysz do żadnej wyspy!");
            return;
        }

        // 2. Właściciel nie może opuścić własnej wyspy w ten sposób
        if (isOwner(playerUUID)) {
            player.sendMessage("§cJesteś liderem! Aby zlikwidować wyspę, użyj /island delete.");
            return;
        }

        // 3. Pobieramy wyspę i usuwamy gracza
        Island island = getIsland(playerUUID);
        island.getMembers().remove(playerUUID);
        playerToIsland.remove(playerUUID);

        // 4. Zapis i teleportacja
        storage.saveIsland(island);
        player.sendMessage("§aOpuściłeś wyspę.");
        teleportManager.teleportWithoutDelay(player);

        // Powiadomienie lidera
        Player owner = Bukkit.getPlayer(island.getOwner());
        if (owner != null) {
            owner.sendMessage("§eGracz " + player.getName() + " opuścił Twoją wyspę.");
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

        // 1. Właściciel
        org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
        String ownerStatus = owner.isOnline() ? "§a●" : "§c●";
        player.sendMessage("§7Właściciel: " + ownerStatus + " §e" + owner.getName());

        // 2. Członkowie
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
            currentOwner.sendMessage("§cMożesz przekazać lidera tylko członkowi twojej wyspy");
            return;
        }

        storage.deleteIsland(oldOwnerUUID);

        island.getMembers().remove(newOwnerUUID);
        island.getMembers().add(oldOwnerUUID);

        island.setOwner(newOwnerUUID);

        islandsByOwner.remove(oldOwnerUUID);
        islandsByOwner.put(newOwnerUUID, island);

        playerToIsland.put(newOwnerUUID, island);
        playerToIsland.put(oldOwnerUUID, island);

        storage.saveIsland(island);

        String newOwnerName = Bukkit.getOfflinePlayer(newOwnerUUID).getName();
        currentOwner.sendMessage("§aPrzekazałeś lidera graczowi §e" + (newOwnerName != null ? newOwnerName : "Nieznany"));

        Player targetPlayer = Bukkit.getPlayer(newOwnerUUID);
        if (targetPlayer != null) {
            targetPlayer.sendMessage("§aZostałeś nowym liderem wyspy!");
        }
    }

    public void upgradeIslandSize(Player player) {
        Island island = islandsByOwner.get(player.getUniqueId());
        if (island == null) {
            player.sendMessage("§cTylko właściciel może ulepszać wyspę!");
            return;
        }
        int currentSize = island.getSize();
        int nextSize = -1;

        // Szukamy następnego progu
        for (int size : islandSizes) {
            if (size > currentSize) {
                nextSize = size;
                break;
            }
        }

        if (nextSize != -1) {
            island.setSize(nextSize);
            storage.saveIsland(island); // Zapisujemy nowy rozmiar w bazie/pliku
            updateBorderForEveryoneOnIsland(island); // Aktualizajca borderu dla wszystkich na wyspie
            player.sendMessage("§aTwoja wyspa została powiększona do rozmiaru " + (nextSize * 2 + 1) + "x" + (nextSize * 2 + 1) + "!");
        } else {
            player.sendMessage("§cOsiągnąłeś już maksymalny rozmiar wyspy!");
        }
    }

    public void setIslandLevel(Player player, int level) {
        if (level < 1 || level > 5) return;

        Island island = getIsland(player.getUniqueId());
        if (island == null) return;

        int newSize = islandSizes[level - 1];
        island.setSize(newSize);
        storage.saveIsland(island);

        updateBorderForEveryoneOnIsland(island);
    }

    private void updateBorderForEveryoneOnIsland(Island island) {
        if (borderManager == null) return;

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // Sprawdzamy świat i czy gracz w ogóle jest na jakiejś wyspie
            Island currentIsland = getIslandAt(onlinePlayer.getLocation());

            // Jeśli gracz stoi na tej konkretnej wyspie, odświeżamy mu border
            if (currentIsland != null && currentIsland.equals(island)) {
                borderManager.updateBorder(onlinePlayer);
            }
        }
    }

    public void performStartupScan() {
        this.plugin.getLogger().info("§e[SkyBlock] Rozpoczynanie skanowania indeksów wysp...");

        // Zbieramy wszystkie zajęte indeksy
        Set<Integer> occupiedIndexes = new HashSet<>();
        for (Island island : islandsByOwner.values()) {
            occupiedIndexes.add(island.getIndex());
        }

        int recoveredCount = 0;

        // Przeszukujemy wszystkie indeksy do aktualnego maksimum
        for (int i = 0; i < currentIndex; i++) {
            // Jeśli indeks nie jest zajęty przez gracza ORAZ nie ma go w wolnych indeksach
            if (!occupiedIndexes.contains(i) && !freeIndexes.contains(i)) {

                // Odtwarzamy lokalizację na podstawie indeksu (używając Twojego kalkulatora spiralnego)
                int[] spiral = SpiralCalculator.getSpiralPosition(i);
                int x = (spiral[0] * SPACING_CHUNKS) << 4;
                int z = (spiral[1] * SPACING_CHUNKS) << 4;
                Location center = new Location(plugin.getWorldManager().getWorld(), x + 8, 100, z + 8);

                // Tworzymy tymczasowy obiekt Island (potrzebny tylko dla parametrów czyszczenia)
                Island dummyIsland = new Island(null, center, 0, i, new ArrayList<>());

                // Uruchamiamy czyszczenie (to samo, które dopisze indeks do freeIndexes po zakończeniu)
                generator.clearIsland(dummyIsland, freeIndexes, storage);

                recoveredCount++;
            }
        }

        if (recoveredCount > 0) {
            this.plugin.getLogger().info("[SkyBlock] Naprawiono " + recoveredCount + " osieroconych obszarów");
        }
    }
}
