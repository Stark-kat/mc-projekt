package stark.skyBlockTest2.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import stark.skyBlockTest2.gui.builder.GuiBuilder;
import stark.skyBlockTest2.gui.builder.ItemBuilder;
import stark.skyBlockTest2.gui.item.GuiItems;
import stark.skyBlockTest2.island.Island;
import stark.skyBlockTest2.island.IslandManager;
import stark.skyBlockTest2.island.IslandRole;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BannedPlayersGui {

    private final IslandManager islandManager;

    public BannedPlayersGui(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void open(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) {
            player.sendMessage("§cNie należysz do żadnej wyspy!");
            return;
        }

        // Tylko właściciel i co-leader mogą zobaczyć GUI banów
        IslandRole role = island.getRole(player.getUniqueId());
        if (role == null || !role.canBan()) {
            player.sendMessage("§cNie masz uprawnień do zarządzania banami!");
            return;
        }

        List<UUID> banned = new ArrayList<>(island.getBannedPlayers());
        int banCount = banned.size();

        // Rozmiar inventory dopasowany do liczby banów (min 27, max 54)
        int rows = Math.max(3, Math.min(6, (int) Math.ceil((banCount + 9) / 9.0) + 1));
        int size = rows * 9;

        Inventory gui = Bukkit.createInventory(new MenuHolder(), size,
                "§8Zbanowani §7(" + banCount + ")");
        GuiBuilder builder = new GuiBuilder(gui);

        builder.fill(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        // Nagłówek — info o liczbie banów
        builder.set(4, new ItemBuilder(Material.BARRIER)
                .name("§c§lZbanowani gracze")
                .lore(
                        "§7Liczba banów: §c" + banCount,
                        " ",
                        "§7Kliknij na gracza, aby go odbanować."
                )
                .build());

        // Lista zbanowanych — sloty 9 do (size-10)
        int slot = 9;
        for (UUID bannedUUID : banned) {
            if (slot >= size - 9) break; // Nie przekraczamy dolnego paska

            OfflinePlayer bannedPlayer = Bukkit.getOfflinePlayer(bannedUUID);
            String name = bannedPlayer.getName() != null ? bannedPlayer.getName() : bannedUUID.toString();
            String lastSeen = bannedPlayer.isOnline() ? "§aOnline" : "§7Offline";

            builder.set(slot, new ItemBuilder(Material.PLAYER_HEAD)
                    .skull(bannedPlayer)
                    .name("§c" + name)
                    .lore(
                            "§7Status: " + lastSeen,
                            " ",
                            "§eKliknij, aby odbanować!"
                    )
                    .setString("action", "UnbanPlayer")
                    .setString("target_uuid", bannedUUID.toString())
                    .build());
            slot++;
        }

        // Dolny pasek
        int bottomRow = size - 9;
        builder.set(bottomRow + 4, new ItemBuilder(Material.ARROW)
                .name("§cCofnij")
                .setString("action", "members")
                .build());

        player.openInventory(gui);
    }
}