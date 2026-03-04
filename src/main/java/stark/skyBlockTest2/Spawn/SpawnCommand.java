package stark.skyBlockTest2.Spawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;



public class SpawnCommand implements CommandExecutor {

    private final TeleportManager teleportManager;

    public SpawnCommand(TeleportManager teleportManager) {
        this.teleportManager = teleportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players");
            return true;
        }

        if (!teleportManager.hasSpawn()) {
            p.sendMessage("§cSpawn nie ustawiony");
            return true;
        }

        teleportManager.teleportWithDelay(p);
        return true;
    }
}
