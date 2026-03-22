package stark.skyBlockTest2.shop.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import stark.skyBlockTest2.shop.gui.AuctionCreateGui;

public class AuctionCommand implements CommandExecutor {

    private final AuctionCreateGui auctionCreateGui;

    public AuctionCommand(AuctionCreateGui auctionCreateGui) {
        this.auctionCreateGui = auctionCreateGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTylko gracz może użyć tej komendy!");
            return true;
        }

        auctionCreateGui.open(player);
        return true;
    }
}