package dev.revivalo.dailyrewards.configuration.enums;

import com.cryptomorin.xseries.XMaterial;
import dev.dbassett.skullcreator.SkullCreator;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import dev.revivalo.dailyrewards.DailyRewardsPlugin;
import dev.revivalo.dailyrewards.configuration.YamlFile;
import dev.revivalo.dailyrewards.hooks.Hooks;
import dev.revivalo.dailyrewards.utils.TextUtils;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public enum Config {
    LANGUAGE,
    MENU_SIZE,
    SETTINGS_MENU_SIZE,
    FILL_BACKGROUND_ENABLED,
    BACKGROUND_ITEM,
    OPEN_MENU_AFTER_CLAIMING,
    JOIN_NOTIFICATION_BY_DEFAULT,
    JOIN_NOTIFICATION_COMMAND,
    ANNOUNCE_ENABLED,
    JOIN_NOTIFICATION_DELAY,
    JOIN_AUTO_CLAIM_DELAY,
    UNAVAILABLE_REWARD_SOUND,
    AUTO_CLAIM_REWARDS_ON_JOIN_BY_DEFAULT,
    AUTO_CLAIM_REWARDS_POSITION,
    JOIN_NOTIFICATION_POSITION,
    JOIN_NOTIFICATION_SOUND,
    DISABLED_WORLDS,
    FIRST_TIME_JOIN_REQUIRED_PLAY_TIME,

    SETTINGS_ENABLED_IN_MENU("settings-enabled-in-menu"),
    SETTINGS_POSITION("settings-position"),
    SETTINGS_ITEM("settings-item"),
    SETTINGS_JOIN_NOTIFICATION_ITEM("settings-join-notification-item"),
    SETTINGS_AUTO_CLAIM_ITEM("settings-auto-claim-item"),
    SETTINGS_BACK_ITEM("settings-back-item"),
    SETTINGS_BACK_POSITION("settings-back-position"),
    SETTINGS_ENABLED_IN_MENU,
    SETTINGS_POSITION,
    SETTINGS_ITEM,
    SETTINGS_BACK_ITEM,
    SETTINGS_BACK_POSITION,

    BACKEND("backend"),
    MYSQL_IP("mysql-ip"),
    MYSQL_PORT("mysql-port"),
    MYSQL_DBNAME("mysql-database-name"),
    MYSQL_USERNAME("mysql-username"),
    MYSQL_PASSWORD("mysql-password"),
    UPDATE_CHECKER("update-checker"),
    MYSQL_POOL_SETTINGS_MAXIMUM_POOL_SIZE("mysql-pool-settings-maximum-pool-size"),
    MYSQL_POOL_SETTINGS_MINIMUM_IDLE("mysql-pool-settings-minimum-idle"),
    MYSQL_POOL_SETTINGS_MAXIMUM_LIFETIME("mysql-pool-settings-maximum-lifetime"),
    MYSQL_POOL_SETTINGS_CONNECTION_TIMEOUT("mysql-pool-settings-connection-timeout"),
    BACKEND,
    MYSQL_IP,
    MYSQL_PORT,
    MYSQL_DATABASE_NAME,
    MYSQL_USERNAME,
    MYSQL_PASSWORD,
    UPDATE_CHECKER,
    MYSQL_POOL_SETTINGS_MAXIMUM_POOL_SIZE,
    MYSQL_POOL_SETTINGS_MINIMUM_IDLE,
    MYSQL_POOL_SETTINGS_MAXIMUM_LIFETIME,
    MYSQL_POOL_SETTINGS_CONNECTION_TIMEOUT,

    MYSQL_PROPERTIES,

    DAILY_ENABLED,
    DAILY_COOLDOWN,
    DAILY_COOLDOWN_FORMAT,
    DAILY_AVAILABLE_AFTER_FIRST_JOIN,
    DAILY_PLACEHOLDER,
    DAILY_POSITIONS,
    DAILY_SOUND,
    DAILY_AVAILABLE_ITEM,
    DAILY_UNAVAILABLE_ITEM,
    DAILY_REWARDS,
    DAILY_PREMIUM_REWARDS,

    WEEKLY_ENABLED,
    WEEKLY_COOLDOWN,
    WEEKLY_COOLDOWN_FORMAT,
    WEEKLY_AVAILABLE_AFTER_FIRST_JOIN,
    WEEKLY_PLACEHOLDER,
    WEEKLY_AVAILABLE_ITEM,
    WEEKLY_UNAVAILABLE_ITEM,
    WEEKLY_POSITIONS,
    WEEKLY_SOUND,
    WEEKLY_REWARDS,
    WEEKLY_PREMIUM_REWARDS,

    MONTHLY_ENABLED,
    MONTHLY_COOLDOWN,
    MONTHLY_COOLDOWN_FORMAT,
    MONTHLY_AVAILABLE_AFTER_FIRST_JOIN,
    MONTHLY_PLACEHOLDER,
    MONTHLY_AVAILABLE_ITEM,
    MONTHLY_UNAVAILABLE_ITEM,
    MONTHLY_POSITIONS,
    MONTHLY_SOUND,
    MONTHLY_REWARDS,
    MONTHLY_PREMIUM_REWARDS,
    CHECK_FOR_FULL_INVENTORY;

    private static final YamlFile configYamlFile = new YamlFile("config.yml",
            DailyRewardsPlugin.get().getDataFolder(), YamlFile.UpdateMethod.EVERYTIME);
    private static final Map<String, String> strings = new HashMap<>();
    private static final Map<String, List<String>> lists = new HashMap<>();
    private static final Map<String, ItemStack> items = new HashMap<>();

    private String getName() {
        return this.name();
    }

    public static void reload() {
        configYamlFile.reload();
        final YamlConfiguration configuration = configYamlFile.getConfiguration();

        ConfigurationSection configSection = configuration.getConfigurationSection("config");

        configSection
                .getKeys(false)
                .forEach(key -> {
                    String editedKey = key.toUpperCase(Locale.ENGLISH).replace("-", "_");
                    if (configSection.isList(key)) {
                        lists.put(editedKey, configSection.getStringList(key));
                    } else
                        strings.put(editedKey, configSection.getString(key));
                });

        loadItems();

        Lang.reload(LANGUAGE);
    }

    private static void loadItems() {
        Arrays.stream(values()).map(Enum::name).filter(valueName -> valueName.endsWith("ITEM")).forEach(valueItemName -> {
            String itemName = strings.get(valueItemName);
            if (itemName.length() > 64) {
                items.put(valueItemName, SkullCreator.itemFromBase64(itemName));
            } else if (itemName.startsWith("CustomModel")) {
                Material material = Material.valueOf(TextUtils.getPlaceholders(itemName, "[").stream().findFirst().get().replace("[", "").replace("]", "").toUpperCase(Locale.ENGLISH));
                int data = Integer.parseInt(TextUtils.getPlaceholders(itemName, "{").stream().findFirst().get().replace("{", "").replace("}", ""));
                final ItemStack itemStack = new ItemStack(material);
                final ItemMeta meta = itemStack.getItemMeta();
                meta.setCustomModelData(data);
                itemStack.setItemMeta(meta);
                items.put(valueItemName, itemStack);
            } else if (Hooks.getItemsAdderHook().isOn() && ItemsAdder.isCustomItem(itemName)) {
                items.put(valueItemName, CustomStack.getInstance(itemName).getItemStack());
            } else if (Hooks.getOraxenHook().isOn() && OraxenItems.exists(itemName)) {
                items.put(valueItemName, OraxenItems.getItemById(itemName).build());
            } else {
                items.put(valueItemName, XMaterial.matchXMaterial(itemName.toUpperCase(Locale.ENGLISH)).orElse(XMaterial.STONE).parseItem());
            }
        });
    }

    public Map<String, String> asStringMap() {
        Map<String, String> map = new HashMap<>();
        ConfigurationSection section = configYamlFile.getConfiguration().getConfigurationSection(asString());
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            String editedKey = key.toUpperCase(Locale.ENGLISH).replace("-", "_");
            map.put(editedKey, section.getString(key));
        }
        return map;
    }

    public List<String> asReplacedList(Map<String, String> definitions) {
        return lists.get(getName());
    }

    public String asString() {
        return strings.get(getName());
    }

    public ItemStack asAnItem() {
        return items.get(getName());
    }

    public String asUppercase() {
        return this.asString().toUpperCase();
    }

    public boolean asBoolean() {
        return Boolean.parseBoolean(asString());
    }

    public long asLong() {
        return Long.parseLong(asString());
    }

    public int asInt() {
        return Integer.parseInt(asString());
    }

    public List<Integer> asIntegerList() {
        return asReplacedList(Collections.emptyMap()).stream().map(Integer::parseInt).collect(Collectors.toList());
    }
}
