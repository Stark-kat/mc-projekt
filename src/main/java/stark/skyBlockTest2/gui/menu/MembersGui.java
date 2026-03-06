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

import java.util.UUID;

public class MembersGui {

    private final IslandManager islandManager;

    public MembersGui(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void open(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());
        if (island == null) return;

        Inventory inv = Bukkit.createInventory(new MenuHolder(), 27, "§8Członkowie wyspy");
        GuiBuilder gui = new GuiBuilder(inv);

        // Właściciel — slot 10 (nieklikalne)
        OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
        boolean viewerIsOwner = player.getUniqueId().equals(island.getOwner());

        gui.set(10, new ItemBuilder(Material.PLAYER_HEAD)
                .skull(owner)
                .name(IslandRole.OWNER.getDisplayName() + " §f" + owner.getName())
                .lore(
                        owner.isOnline() ? "§aOnline" : "§7Offline",
                        viewerIsOwner ? " " : "",
                        viewerIsOwner ? "§eKliknij, aby zarządzać" : ""
                )
                // Właściciel może kliknąć na siebie (zobaczy opcję odejścia? Nie — właściciel musi najpierw transferować)
                .build());

        // Członkowie — sloty 12+
        int slot = 12;
        for (UUID memberUUID : island.getMembers()) {
            if (slot > 16) break;
            OfflinePlayer member = Bukkit.getOfflinePlayer(memberUUID);
            IslandRole role = island.getRole(memberUUID);
            String roleLabel = (role != null ? role : IslandRole.MEMBER).getDisplayName();
            boolean isSelf = memberUUID.equals(player.getUniqueId());

            gui.set(slot, new ItemBuilder(Material.PLAYER_HEAD)
                    .skull(member)
                    .name(roleLabel + " §f" + (member.getName() != null ? member.getName() : "?")
                            + (isSelf ? " §7(Ty)" : ""))
                    .lore(
                            member.isOnline() ? "§aOnline" : "§7Offline",
                            " ",
                            "§eKliknij, aby zarządzać"
                    )
                    .setString("action", "MemberInfo")
                    .setString("target_uuid", memberUUID.toString())
                    .build());
            slot++;
        }

        // Przycisk "Zbanowani" — widoczny tylko dla właściciela i co-leadera
        IslandRole viewerRole = island.getRole(player.getUniqueId());
        if (viewerRole != null && viewerRole.canBan()) {
            int banCount = island.getBannedPlayers().size();
            gui.set(20, new ItemBuilder(Material.BARRIER)
                    .name("§c§lZbanowani graczy §7(" + banCount + ")")
                    .lore("§7Kliknij, aby zobaczyć listę.")
                    .setString("action", "OpenBans")
                    .build());
        }

        gui.fill(GuiItems.grayBackground());
        gui.set(22, GuiItems.closeButton());

        player.openInventory(inv);
    }
}