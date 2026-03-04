package stark.skyBlockTest2.util;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

public class ChatUtil {

    public static void sendInviteMessage(Player target, String ownerName) {
        target.sendMessage(" ");
        target.sendMessage("§eGracz §6" + ownerName + " §ezaprasza Cię do swojej wyspy!");

        // Przycisk AKCEPTUJ
        TextComponent accept = new TextComponent("§8[§a§lAKCEPTUJ§8]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/island accept"));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aKliknij, aby dołączyć do wyspy")));

        // Przycisk ODRZUĆ
        TextComponent deny = new TextComponent("§8[§c§lODRZUĆ§8]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/island decline"));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§cKliknij, aby odrzucić zaproszenie")));

        TextComponent message = new TextComponent("§eWybierz opcję: ");
        message.addExtra(accept);
        message.addExtra(new TextComponent("  "));
        message.addExtra(deny);

        target.spigot().sendMessage(message);
        target.sendMessage("§7(Zaproszenie wygaśnie za 60 sekund)");
        target.sendMessage(" ");
    }
}