package dev.revivalo.dailyrewards.manager.reward.action;

import dev.revivalo.dailyrewards.DailyRewardsPlugin;
import dev.revivalo.dailyrewards.api.event.PlayerClaimRewardEvent;
import dev.revivalo.dailyrewards.api.event.PlayerPreClaimRewardEvent;
import dev.revivalo.dailyrewards.configuration.file.Config;
import dev.revivalo.dailyrewards.configuration.file.Lang;
import dev.revivalo.dailyrewards.manager.cooldown.Cooldown;
import dev.revivalo.dailyrewards.manager.cooldown.CooldownManager;
import dev.revivalo.dailyrewards.manager.reward.ActionsExecutor;
import dev.revivalo.dailyrewards.manager.reward.Reward;
import dev.revivalo.dailyrewards.manager.reward.RewardType;
import dev.revivalo.dailyrewards.manager.reward.action.checker.AvailableSlotsInInventoryChecker;
import dev.revivalo.dailyrewards.manager.reward.action.checker.Checker;
import dev.revivalo.dailyrewards.manager.reward.action.checker.DisabledWorldCheck;
import dev.revivalo.dailyrewards.manager.reward.action.checker.EnoughPlayTimeChecker;
import dev.revivalo.dailyrewards.manager.reward.action.response.ActionResponse;
import dev.revivalo.dailyrewards.manager.reward.action.response.ClaimActionResponse;
import dev.revivalo.dailyrewards.user.User;
import dev.revivalo.dailyrewards.user.UserHandler;
import dev.revivalo.dailyrewards.util.PermissionUtil;
import dev.revivalo.dailyrewards.util.PlayerUtil;
import dev.revivalo.dailyrewards.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClaimAction implements RewardAction<RewardType> {
    private final CommandSender executor;
    private final List<Checker> checkers;
    private boolean announce = true;
    private boolean menuShouldOpen = Config.OPEN_MENU_AFTER_CLAIMING.asBoolean();

    public ClaimAction(CommandSender executor) {
        this.executor = executor;

        checkers = new ArrayList<Checker>() {{
            List<String> disabledWorlds = Config.DISABLED_WORLDS.asReplacedList();
            if (!disabledWorlds.isEmpty()) add(new DisabledWorldCheck(disabledWorlds));
            if (Config.CHECK_FOR_FULL_INVENTORY.asBoolean()) add(new AvailableSlotsInInventoryChecker());
            if (Config.FIRST_TIME_JOIN_REQUIRED_PLAY_TIME.asInt() > 0) add(new EnoughPlayTimeChecker());
        }};
    }

    @Override
    public ActionResponse execute(OfflinePlayer offlinePlayer, RewardType type) {
        final User user = UserHandler.getUser(offlinePlayer.getUniqueId());
        if (!user.isOnline()) {
            return ActionResponse.Type.UNAVAILABLE_PLAYER;
        }

        final Player player = offlinePlayer.getPlayer();
        final Reward reward = DailyRewardsPlugin.getRewardManager()
                .getRewardByType(type)
                .orElse(null);

        if (reward == null) {
            player.sendMessage(Lang.REWARD_DISABLED.asColoredString(player));
            return ClaimActionResponse.UNAVAILABLE_REWARD;
        }

        final List<String> rewardActions = TextUtil.replaceList(PermissionUtil.hasPremium(player, type) ? reward.getPremiumRewards() : reward.getDefaultRewards(), new HashMap<String, String>() {{
            put("player", player.getName());
        }});

        PlayerPreClaimRewardEvent playerPreClaimRewardEvent = new PlayerPreClaimRewardEvent(player, reward.getType(), rewardActions);
        Bukkit.getPluginManager().callEvent(playerPreClaimRewardEvent);

        if (playerPreClaimRewardEvent.isCancelled()) {
            return ClaimActionResponse.UNKNOWN;
        }

        if (!PermissionUtil.hasPermission(player, type.getPermission())) {
            //if (!player.hasPermission(type.getPermission())) {
            //if (!fromCommand) return;
            if (announce) player.sendMessage(Lang.INSUFFICIENT_PERMISSION.asColoredString(player)
                    .replace("%permission%", type.getPermission()));
            return ClaimActionResponse.INSUFFICIENT_PERMISSIONS;
        }

        for (Checker checker : getCheckers()) {
            if (PermissionUtil.hasPermission(player, checker.getBypassPermission())) {
                continue;
            }

            if (!checker.check(player)) {
                if (announce) player.sendMessage(checker.getFailedCheckMessage());
                return checker.getClaimActionResponse();
            }
        }

        Cooldown cooldown = user.getCooldown(reward.getType());
        if (cooldown.isClaimable()) {

            if (rewardActions.isEmpty()) {
                player.sendMessage(Lang.REWARDS_ARE_NOT_SET.asColoredString(player));
            } else {
                PlayerClaimRewardEvent playerClaimRewardEvent = new PlayerClaimRewardEvent(player, reward.getType());
                Bukkit.getPluginManager().callEvent(playerClaimRewardEvent);

                ActionsExecutor.executeActions(
                        player,
                        reward.getName(),
                        TextUtil.findAndReturnActions(rewardActions)
                );

                //rewardCommands.forEach(command -> Bukkit.dispatchCommand(DailyRewardsPlugin.getConsole(), command));
            }

            CooldownManager.setCooldown(user, reward);

            if (announce) {
                PlayerUtil.playSound(player, reward.getSound());

                player.sendTitle(reward.getTitle(), reward.getSubtitle());

                if (Config.ANNOUNCE_ENABLED.asBoolean()) {
                    Bukkit.broadcastMessage((PermissionUtil.hasPremium(player, type) ? reward.getCollectedPremiumMessage(player) : reward.getCollectedMessage(player)).replace("%player%", player.getName()));
                }
            }
            if (!menuShouldOpen) player.closeInventory();

        } else {
            if (!menuShouldOpen) {
                player.sendMessage(Lang.COOLDOWN_MESSAGE.asReplacedString(player, new HashMap<String, String>() {{
                    put("%type%", type.getPlaceholder());
                    put("%time%", cooldown.getFormat(reward.getCooldownFormat()));
                }}));
            }
            PlayerUtil.playSound(player, Config.UNAVAILABLE_REWARD_SOUND.asUppercase());
        }

        return ActionResponse.Type.PROCEEDED;
    }

    @Override
    public boolean menuShouldOpen() {
        return menuShouldOpen;
    }

    public ClaimAction disableAnnounce() {
        this.announce = false;
        return this;
    }

    public ClaimAction disableMenuOpening() {
        this.menuShouldOpen = false;
        return this;
    }

    @Override
    public CommandSender getExecutor() {
        return executor;
    }

    @Override
    public PermissionUtil.Permission getPermission() {
        return null;
    }

    @Override
    public List<Checker> getCheckers() {
        return checkers;
    }
}