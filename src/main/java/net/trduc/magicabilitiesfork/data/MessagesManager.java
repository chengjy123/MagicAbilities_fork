package net.trduc.magicabilitiesfork.data;

import net.trduc.magicabilitiesfork.MagicAbilitiesfork;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessagesManager {

    private static MessagesManager instance;
    private final Map<String, String> messages = new HashMap<>();
    private FileConfiguration config;
    private File configFile;
    private boolean loaded = false;

    private MessagesManager() {
    }

    public static synchronized MessagesManager getInstance() {
        if (instance == null) {
            instance = new MessagesManager();
        }
        return instance;
    }

    public void init() {
        if (loaded) return;
        configFile = new File(MagicAbilitiesfork.magicPlugin.getDataFolder(), "messages.yml");

        if (!configFile.exists()) {
            MagicAbilitiesfork.magicPlugin.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defaultStream = MagicAbilitiesfork.magicPlugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        loadSection("", config);
        loaded = true;
    }

    private void loadMessages() {
        init();
    }

    private void loadSection(String prefix, ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (section.isConfigurationSection(key)) {
                loadSection(fullKey, section.getConfigurationSection(key));
            } else {
                String value = section.getString(key);
                if (value != null) {
                    messages.put(fullKey, ChatColor.translateAlternateColorCodes('&', value));
                }
            }
        }
    }

    public String get(String key) {
        if (!loaded) init();
        return messages.getOrDefault(key, key);
    }

    public String get(String key, Map<String, String> placeholders) {
        String message = get(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public String get(String key, String... replacements) {
        String message = get(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }

    public void reload() {
        messages.clear();
        loadMessages();
    }
}
