package stark.skyBlockTest2.util;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class JoinMessage implements Listener {

    private final CounterManager counterManager;

    public JoinMessage(CounterManager counterManager) {
        this.counterManager = counterManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        String msg;

        if(player.hasPlayedBefore()){
            msg= "§aMiło Cię znowu widzieć, §f" + player.getName() + "§a!";
        } else {
            msg= "§6Witaj §f" + player.getName() + "§6! Jesteś tu pierwszy raz 🎉";
            counterManager.add("UnikalniGracze");
        }
        player.sendMessage(msg);
    }
}
