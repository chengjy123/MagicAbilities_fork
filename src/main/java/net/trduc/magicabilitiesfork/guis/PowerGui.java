package net.trduc.magicabilitiesfork.guis;

import net.trduc.magicabilitiesfork.data.MessagesManager;
import net.trduc.magicabilitiesfork.powers.Power;
import net.trduc.magicabilitiesfork.powers.PowerType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static net.trduc.magicabilitiesfork.data.PlayerData.getPlayerData;
import static net.trduc.magicabilitiesfork.players.PowerPlayer.players;

public class PowerGui implements Listener {

    private final Map<Inventory, Player> invPlayer = new HashMap<>();
    private final Map<Inventory, Integer> editingAbility = new HashMap<>();
    private final MessagesManager messages = MessagesManager.getInstance();

    public void openPowerGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, ChatColor.translateAlternateColorCodes('&', "&6技能管理"));

        fillBorder(inv);
        fillAbilityItems(inv, player);
        fillControlButtons(inv, player);

        cleanupOldInventories(player);
        invPlayer.put(inv, player);
        editingAbility.put(inv, -1);
        player.openInventory(inv);
    }

    private void fillBorder(Inventory inv) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName("");
        border.setItemMeta(meta);

        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 36; i < 45; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);
        inv.setItem(27, border);
        inv.setItem(35, border);
    }

    private void fillAbilityItems(Inventory inv, Player player) {
        if (!players.containsKey(player)) return;

        Power power = players.get(player).getPower();
        Map<Integer, Integer> binds = players.get(player).getBinds();

        for (int ability = 0; ability < 9; ability++) {
            int slot = binds.getOrDefault(ability, ability);
            String abilityName = power.getAbilityName(ability);
            Material material = getAbilityMaterial(ability);

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', abilityName));

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "技能编号: " + ChatColor.WHITE + ability);
            lore.add(ChatColor.GRAY + "绑定槽位: " + ChatColor.GOLD + slot);
            lore.add("");
            lore.add(ChatColor.GREEN + "左键点击: 修改绑定槽位");
            lore.add(ChatColor.YELLOW + "右键点击: 直接设置为当前槽位");
            meta.setLore(lore);

            item.setItemMeta(meta);
            inv.setItem(10 + ability, item);
        }
    }

    private void fillControlButtons(Inventory inv, Player player) {
        boolean isEnabled = players.containsKey(player) && players.get(player).getPower().isEnabled();

        ItemStack toggleBtn = new ItemStack(isEnabled ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta toggleMeta = toggleBtn.getItemMeta();
        toggleMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', isEnabled ? "&a技能已开启" : "&c技能已关闭"));
        List<String> toggleLore = new ArrayList<>();
        toggleLore.add(ChatColor.GRAY + "点击切换技能状态");
        toggleMeta.setLore(toggleLore);
        toggleBtn.setItemMeta(toggleMeta);
        inv.setItem(39, toggleBtn);

        ItemStack resetBtn = new ItemStack(Material.BLACK_WOOL);
        ItemMeta resetMeta = resetBtn.getItemMeta();
        resetMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7重置绑定"));
        List<String> resetLore = new ArrayList<>();
        resetLore.add(ChatColor.GRAY + "点击将所有技能重置为默认绑定");
        resetMeta.setLore(resetLore);
        resetBtn.setItemMeta(resetMeta);
        inv.setItem(41, resetBtn);
    }

    private Material getAbilityMaterial(int ability) {
        Material[] materials = {
                Material.WOODEN_SWORD,
                Material.STONE_SWORD,
                Material.IRON_SWORD,
                Material.DIAMOND_SWORD,
                Material.GOLDEN_SWORD,
                Material.BOW,
                Material.CROSSBOW,
                Material.TRIDENT,
                Material.SHIELD
        };
        return materials[ability % materials.length];
    }

    private void cleanupOldInventories(Player player) {
        List<Inventory> toRemove = new ArrayList<>();
        for (Map.Entry<Inventory, Player> e : invPlayer.entrySet()) {
            if (e.getValue().equals(player)) {
                toRemove.add(e.getKey());
            }
        }
        for (Inventory i : toRemove) {
            invPlayer.remove(i);
            editingAbility.remove(i);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        if (!invPlayer.containsKey(inv)) return;
        e.setCancelled(true);

        Player player = invPlayer.get(inv);
        int editing = editingAbility.get(inv);
        ItemStack clicked = e.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        if (editing >= 0) {
            if (clicked.getType() == Material.BARRIER) {
                editingAbility.put(inv, -1);
                openPowerGui(player);
                return;
            }

            int slot = e.getSlot();
            if (slot >= 10 && slot <= 18) {
                int targetSlot = slot - 10;
                if (players.containsKey(player)) {
                    players.get(player).changeBind(editing, targetSlot);
                    player.sendMessage(ChatColor.GREEN + "技能 " + editing + " 已绑定到槽位 " + targetSlot);
                }
                editingAbility.put(inv, -1);
                openPowerGui(player);
            }
            return;
        }

        int slot = e.getSlot();

        if (slot >= 10 && slot <= 18) {
            int ability = slot - 10;
            if (e.isRightClick()) {
                int currentSlot = player.getInventory().getHeldItemSlot();
                if (players.containsKey(player)) {
                    players.get(player).changeBind(ability, currentSlot);
                    player.sendMessage(ChatColor.GREEN + "技能 " + ability + " 已绑定到当前槽位 " + currentSlot);
                }
                openPowerGui(player);
            } else {
                editingAbility.put(inv, ability);
                openSlotSelectionGui(player, ability);
            }
            return;
        }

        if (slot == 39) {
            togglePower(player);
            openPowerGui(player);
            return;
        }

        if (slot == 41) {
            if (players.containsKey(player)) {
                players.get(player).resetBinds();
                player.sendMessage(ChatColor.GREEN + "已重置所有技能绑定！");
            }
            openPowerGui(player);
        }
    }

    private void openSlotSelectionGui(Player player, int ability) {
        Inventory inv = Bukkit.createInventory(null, 36, ChatColor.translateAlternateColorCodes('&', "&6选择绑定槽位 - 技能 " + ability));

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("");
        border.setItemMeta(borderMeta);

        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 27; i < 36; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);

        Map<Integer, Integer> binds = players.containsKey(player) ? players.get(player).getBinds() : new HashMap<>();

        for (int targetSlot = 0; targetSlot < 9; targetSlot++) {
            Material material = getSlotMaterial(targetSlot, binds);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6槽位 " + targetSlot));

            List<String> lore = new ArrayList<>();
            int currentAbilityForSlot = -1;
            for (Map.Entry<Integer, Integer> entry : binds.entrySet()) {
                if (entry.getValue().equals(targetSlot)) {
                    currentAbilityForSlot = entry.getKey();
                    break;
                }
            }
            if (currentAbilityForSlot >= 0) {
                String abilityName = players.get(player).getPower().getAbilityName(currentAbilityForSlot);
                lore.add(ChatColor.GRAY + "当前绑定: " + ChatColor.translateAlternateColorCodes('&', abilityName));
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
            inv.setItem(10 + targetSlot, item);
        }

        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c取消"));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(22, cancel);

        cleanupOldInventories(player);
        invPlayer.put(inv, player);
        editingAbility.put(inv, ability);
        player.openInventory(inv);
    }

    private Material getSlotMaterial(int slot, Map<Integer, Integer> binds) {
        Material[] materials = {
                Material.SLIME_BALL,
                Material.MAGMA_CREAM,
                Material.GLOW_INK_SAC,
                Material.GUNPOWDER,
                Material.SUGAR,
                Material.FEATHER,
                Material.ENDER_PEARL,
                Material.BLAZE_POWDER,
                Material.NETHER_STAR
        };
        return materials[slot % materials.length];
    }

    private void togglePower(Player player) {
        if (!players.containsKey(player)) return;

        Power power = players.get(player).getPower();
        boolean newState = !power.isEnabled();
        power.setEnabled(newState);
        getPlayerData(player).setEnabled(newState);

        if (newState) {
            player.sendMessage(messages.get("events.power_enabled"));
        } else {
            player.sendMessage(messages.get("events.power_disabled"));
        }
    }
}
