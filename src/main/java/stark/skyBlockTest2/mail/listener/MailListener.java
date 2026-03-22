package stark.skyBlockTest2.mail.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import stark.skyBlockTest2.mail.MailManager;

public class MailListener implements Listener {

    private final MailManager mailManager;

    public MailListener(MailManager mailManager) {
        this.mailManager = mailManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        mailManager.loadInbox(e.getPlayer().getUniqueId());

        int unread = mailManager.getUnreadCount(e.getPlayer().getUniqueId());
        if (unread > 0) {
            e.getPlayer().sendMessage("§6§l✉ §r§7Masz §e" + unread
                    + "§7 nieprzeczytanych wiadomości. §8(/poczta)");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        mailManager.unloadInbox(e.getPlayer().getUniqueId());
    }
}