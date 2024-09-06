package dev.revivalo.dailyrewards.hook;

import dev.revivalo.dailyrewards.DailyRewardsPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public interface Hook<T> {
    default void preRegister() {
        register();
        if (isOn()) {
            DailyRewardsPlugin.get().getLogger().log(Level.INFO, this.getClass().getSimpleName() + " has been registered.");
        }
    }

    void register();
    boolean isOn();

    @Nullable
    T getApi();
}
