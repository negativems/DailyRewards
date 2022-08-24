package cz.revivalo.dailyrewards.rewardmanager;

import cz.revivalo.dailyrewards.lang.Lang;
import cz.revivalo.dailyrewards.playerconfig.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Cooldowns {

    public String getCooldown(Player p, String type, boolean formatted){
        FileConfiguration data = PlayerData.getConfig(p);
        long cd = data.getLong("rewards." + type) - System.currentTimeMillis();
        if (formatted){
            switch (type){
                case "daily":
                    return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(cd),
                            TimeUnit.MILLISECONDS.toMinutes(cd) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(cd)),
                            TimeUnit.MILLISECONDS.toSeconds(cd) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(cd)));
                case "weekly":
                case "monthly":
                    return String.format(Lang.COOLDOWNFORMAT.content(p).replace("%days%", "%02d").replace("%hours%", "%02d"), TimeUnit.MILLISECONDS.toDays(cd),
                            TimeUnit.MILLISECONDS.toHours(cd) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(cd)));

            }
        } else {
            return String.valueOf(data.getLong("rewards." + type) - System.currentTimeMillis());
        }
        return null;
    }

    public void set(Player p){
        FileConfiguration data = PlayerData.getConfig(p);
        if (!data.isConfigurationSection("rewards")) {
            long currentTime = System.currentTimeMillis();
            Objects.requireNonNull(data).set("rewards.daily", currentTime + Lang.DAILY_COOLDOWN.getLong());
            Objects.requireNonNull(data).set("rewards.weekly", currentTime + Lang.WEEKLY_COOLDOWN.getLong());
            Objects.requireNonNull(data).set("rewards.monthly", currentTime + Lang.MONTHLY_COOLDOWN.getLong());
            PlayerData.getConfig(p).save();
        }
    }


    public void setCooldown(final Player player, String type){
        final PlayerData playerData = PlayerData.getConfig(player);
        long cd = 0;
        long currentTime = System.currentTimeMillis();
        switch (type){
            case "daily":
                cd = currentTime + Lang.DAILY_COOLDOWN.getLong();
                break;
            case "weekly":
                cd = currentTime + Lang.WEEKLY_COOLDOWN.getLong();
                break;
            case "monthly":
                cd = currentTime + Lang.MONTHLY_COOLDOWN.getLong();
                break;
        }
        playerData.set("rewards." + type, cd);
        playerData.save();
    }
}
