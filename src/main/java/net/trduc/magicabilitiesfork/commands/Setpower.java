package net.trduc.magicabilitiesfork.commands;

import net.trduc.magicabilitiesfork.data.MessagesManager;
import net.trduc.magicabilitiesfork.data.PlayerData;
import net.trduc.magicabilitiesfork.powers.PowerType;
import net.trduc.magicabilitiesfork.powers.RandomPowerAssigner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class Setpower implements CommandExecutor {

    private final MessagesManager messages = MessagesManager.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("setpower")) return false;
        if (args.length != 2) {
            sender.sendMessage(messages.get("commands.setpower.usage"));
            return true;
        }

        PowerType powerType = null;
        boolean isRandom = false;

        if (args[1].equalsIgnoreCase("random")) {
            isRandom = true;
        } else {
            try {
                powerType = PowerType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(messages.get("commands.setpower.unknown_power") + args[1] + "。使用 'random' 或有效的技能名称。");
                return true;
            }
        }

        List<Player> targets = new ArrayList<>();
        if (args[0].equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(messages.get("commands.setpower.player_not_found") + args[0]);
                return true;
            }
            targets.add(target);
        }

        int success = 0;
        for (Player target : targets) {
            if (!players.containsKey(target)) {
                sender.sendMessage(messages.get("commands.setpower.no_player_entry", "player", target.getName()));
                continue;
            }
            try {
                if (isRandom) {
                    final PlayerData pd = PlayerData.getPlayerData(target);
                    if (pd.getPower() != PowerType.NONE) {
                        sender.sendMessage(ChatColor.YELLOW + target.getName() + " 已经拥有技能 (" + pd.getPower().name().toLowerCase() + ")，无法分配随机技能。");
                        continue;
                    }
                    PowerType finalType2 = RandomPowerAssigner.randomPower();
                    target.sendMessage(messages.get("commands.setpower.received_random") + ChatColor.YELLOW + ChatColor.BOLD + finalType2.name().replace('_', ' '));
                } else {
                    players.get(target).changePower(powerType);
                }
                success++;
            } catch (Exception e) {
                sender.sendMessage(messages.get("commands.setpower.error", "player", target.getName()) + e.getMessage());
            }
        }
        sender.sendMessage(success > 0
                ? messages.get("commands.setpower.success", "count", String.valueOf(success), "power", isRandom ? "random" : powerType.name().toLowerCase())
                : messages.get("commands.setpower.failed"));
        return true;
    }
}