package stark.skyBlockTest2.mail.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import stark.skyBlockTest2.mail.gui.MailGui;

public class MailCommand implements CommandExecutor {

    private final MailGui mailGui;

    public MailCommand(MailGui mailGui) {
        this.mailGui = mailGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko gracze mogą używać tej komendy.");
            return true;
        }
        mailGui.open(player, 0);
        return true;
    }
}