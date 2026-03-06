package stark.skyBlockTest2.gui.menu;

import net.kyori.adventure.text.Component;
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

public class MemberSettingsGui {

    private final IslandManager islandManager;

    public MemberSettingsGui(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void open(Player viewer, OfflinePlayer target) {
        Island island = islandManager.getIsland(viewer.getUniqueId());
        if (island == null) return;

        IslandRole viewerRole = island.getRole(viewer.getUniqueId());
        IslandRole targetRole = island.getRole(target.getUniqueId());
        if (targetRole == null) targetRole = IslandRole.MEMBER;

        // Właściciel nie powinien być tu widoczny jako target
        if (targetRole == IslandRole.OWNER) return;

        boolean viewingSelf = viewer.getUniqueId().equals(target.getUniqueId());
        boolean isOwner     = viewerRole == IslandRole.OWNER;
        boolean isCoLeader  = targetRole == IslandRole.CO_LEADER;

        String targetName = target.getName() != null ? target.getName() : "?";

        Inventory inv = Bukkit.createInventory(new MenuHolder(), 27,
                Component.text("Zarządzaj: " + targetName));
        GuiBuilder gui = new GuiBuilder(inv);

        // --- Głowa gracza (slot 4) ---
        gui.set(4, new ItemBuilder(Material.PLAYER_HEAD)
                .skull(target)
                .name("§e§l" + targetName)
                .lore(
                        "§7Status: " + (target.isOnline() ? "§aOnline" : "§7Offline"),
                        "§7Rola: " + targetRole.getDisplayName()
                )
                .build());

        if (viewingSelf) {
            // --- Gracz patrzy na siebie — opcja odejścia (slot 13) ---
            gui.set(13, new ItemBuilder(Material.RED_BED)
                    .name("§c§lOdejdź z wyspy")
                    .lore(
                            "§7Opuścisz wyspę i stracisz do niej dostęp.",
                            " ",
                            "§cKliknij, aby opuścić wyspę!"
                    )
                    .setString("action", "LeaveIsland")
                    .build());

        } else {
            // --- Kafelek roli Co-Leader (slot 11) ---
            if (isOwner) {
                // Właściciel może toggle Co-Leader
                if (isCoLeader) {
                    gui.set(11, new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
                            .name("§e§lCo-Leader §a✔")
                            .lore(
                                    "§7Ten gracz może banować,",
                                    "§7kickować i zapraszać graczy.",
                                    " ",
                                    "§cKliknij, aby odebrać rolę Co-Leader."
                            )
                            .glow(true)
                            .setString("action", "SetCoLeader")
                            .setString("target_uuid", target.getUniqueId().toString())
                            .build());
                } else {
                    gui.set(11, new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE)
                            .name("§7§lCzłonek")
                            .lore(
                                    "§7Zwykła rola — brak dodatkowych uprawnień.",
                                    " ",
                                    "§eKliknij, aby nadać rolę §eCo-Leader§e."
                            )
                            .setString("action", "SetCoLeader")
                            .setString("target_uuid", target.getUniqueId().toString())
                            .build());
                }
            } else {
                // Co-Leader lub niżej — tylko podgląd roli
                gui.set(11, new ItemBuilder(isCoLeader
                        ? Material.YELLOW_STAINED_GLASS_PANE
                        : Material.CYAN_STAINED_GLASS_PANE)
                        .name(targetRole.getDisplayName())
                        .lore("§8Tylko właściciel może zmieniać role.")
                        .build());
            }

            // --- Przekaż własność (slot 13, tylko właściciel) ---
            if (isOwner) {
                gui.set(13, new ItemBuilder(Material.GOLDEN_HELMET)
                        .name("§6Przekaż Lidera")
                        .lore(
                                "§7Przekaż własność wyspy graczowi §e" + targetName + "§7.",
                                "§7Staniesz się zwykłym członkiem.",
                                " ",
                                "§c§lNIEODWRACALNE!"
                        )
                        .setString("action", "PromoteToLeader")
                        .setString("target_uuid", target.getUniqueId().toString())
                        .build());
            }

            // --- Wykop (slot 15, owner i co-leader mogą) ---
            if (viewerRole != null && viewerRole.canKick()) {
                gui.set(15, new ItemBuilder(Material.RED_TERRACOTTA)
                        .name("§cWyrzuć z wyspy")
                        .lore("§7Gracz straci dostęp do tej wyspy.")
                        .setString("action", "KickMember")
                        .setString("target_uuid", target.getUniqueId().toString())
                        .build());
            }
        }

        gui.set(22, new ItemBuilder(Material.ARROW)
                .name("§7Wróć do listy członków")
                .setString("action", "members")
                .build());

        gui.set(26, GuiItems.closeButton());
        gui.fill(GuiItems.grayBackground());

        viewer.openInventory(inv);
    }
}