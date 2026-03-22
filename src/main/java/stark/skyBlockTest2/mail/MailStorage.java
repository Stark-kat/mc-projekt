package stark.skyBlockTest2.mail;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import stark.skyBlockTest2.SkyBlockTest2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class MailStorage {

    private final SkyBlockTest2 plugin;
    private final File          mailDir;

    public MailStorage(SkyBlockTest2 plugin) {
        this.plugin  = plugin;
        this.mailDir = new File(plugin.getDataFolder(), "mail");
        if (!mailDir.exists()) mailDir.mkdirs();
    }

    // =========================================================================
    // Zapis
    // =========================================================================

    public void save(MailMessage msg) {
        File file = getFile(msg.getRecipientUuid());
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String path = "messages." + msg.getId();
        cfg.set(path + ".sender-uuid",    msg.getSenderUuid() != null ? msg.getSenderUuid().toString() : null);
        cfg.set(path + ".sender-name",    msg.getSenderName());
        cfg.set(path + ".subject",        msg.getSubject());
        cfg.set(path + ".message",        msg.getMessage());
        cfg.set(path + ".sent-at",        msg.getSentAt());
        cfg.set(path + ".read",           msg.isRead());
        cfg.set(path + ".claimed",        msg.isClaimed());
        cfg.set(path + ".reply-to-id",    msg.getReplyToId());
        cfg.set(path + ".money",          msg.getMoney());

        if (msg.hasItem()) {
            cfg.set(path + ".item",
                    Base64.getEncoder().encodeToString(msg.getItem().serializeAsBytes()));
        }

        saveAsync(file, cfg);
    }

    public void delete(UUID playerUuid, String messageId) {
        File file = getFile(playerUuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("messages." + messageId, null);
        saveAsync(file, cfg);
    }

    // =========================================================================
    // Odczyt
    // =========================================================================

    public List<MailMessage> load(UUID playerUuid) {
        File file = getFile(playerUuid);
        if (!file.exists()) return new ArrayList<>();

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("messages");
        if (section == null) return new ArrayList<>();

        List<MailMessage> result = new ArrayList<>();

        for (String id : section.getKeys(false)) {
            try {
                String path = "messages." + id;

                String senderUuidStr = cfg.getString(path + ".sender-uuid");
                UUID   senderUuid    = senderUuidStr != null ? UUID.fromString(senderUuidStr) : null;
                String senderName    = cfg.getString(path + ".sender-name", "System");
                String subject       = cfg.getString(path + ".subject", "");
                String message       = cfg.getString(path + ".message", "");
                long   sentAt        = cfg.getLong(path + ".sent-at");
                boolean read         = cfg.getBoolean(path + ".read", false);
                boolean claimed      = cfg.getBoolean(path + ".claimed", false);
                String replyToId     = cfg.getString(path + ".reply-to-id");
                double money         = cfg.getDouble(path + ".money", 0.0);

                ItemStack item = null;
                String itemBase64 = cfg.getString(path + ".item");
                if (itemBase64 != null) {
                    item = ItemStack.deserializeBytes(Base64.getDecoder().decode(itemBase64));
                }

                MailMessage msg = new MailMessage(id, senderUuid, senderName, playerUuid,
                        subject, message, item, money, sentAt, replyToId);
                if (read)    msg.markRead();
                if (claimed) msg.markClaimed();

                result.add(msg);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "[MailStorage] Błąd ładowania wiadomości '" + id + "' dla " + playerUuid, e);
            }
        }

        result.sort(Comparator.comparingLong(MailMessage::getSentAt).reversed());
        return result;
    }

    // =========================================================================
    // Atomic async save
    // =========================================================================

    private void saveAsync(File file, FileConfiguration cfg) {
        String yaml = cfg.saveToString();
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
            try (FileWriter writer = new FileWriter(tmp)) {
                writer.write(yaml);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "[MailStorage] Błąd zapisu " + file.getName(), e);
                return;
            }
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file); }
        });
    }

    private File getFile(UUID uuid) {
        return new File(mailDir, uuid.toString() + ".yml");
    }
}