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

public class MemberSettingsGui {

    public void open(Player admin, OfflinePlayer target) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(), 27, Component.text("Zarządzaj: " + target.getName()));
        GuiBuilder gui = new GuiBuilder(inv);

        // Głowa gracza jako podgląd
        gui.set(4, GuiItems.playerHead(target, false));

        // Przycisk: Awansuj na lidera
        gui.set(11, new ItemBuilder(Material.GOLDEN_HELMET)
                .name("§6Przekaż Lidera")
                .lore("§7Uważaj, to działanie jest nieodwracalne!")
                .setString("action", "PromoteToLeader")
                .setString("target_uuid", target.getUniqueId().toString())
                .build());

        // Przycisk: Wyrzuć z wyspy
        gui.set(15, new ItemBuilder(Material.RED_TERRACOTTA)
                .name("§cWyrzuć z wyspy")
                .lore("§7Gracz straci dostęp do tej wyspy.")
                .setString("action", "KickMember")
                .setString("target_uuid", target.getUniqueId().toString())
                .build());
        gui.set(22, new ItemBuilder(Material.ARROW)
                .name("§7Wróć do listy członków")
                .setString("action", "members")
                .build());

        gui.set(26, GuiItems.closeButton());
        gui.fill(GuiItems.grayBackground());

        admin.openInventory(inv);
    }
}
