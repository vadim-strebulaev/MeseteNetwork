package com.example.myplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class MyPlugin extends JavaPlugin {

    // Хранение очков игроков (ник -> очки)
    private final Map<String, Integer> playerScores = new HashMap<>();

    // Список hologram (ArmorStand) для очистки / обновления
    private final List<ArmorStand> holograms = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("MyPlugin включён!");
        // Можно загружать сохраненные очки из файла, если нужно
    }

    @Override
    public void onDisable() {
        getLogger().info("MyPlugin выключен!");
        removeHolograms();
        // Можно сохранять очки в файл, если нужно
    }

    // Удаляем старые holograms
    private void removeHolograms() {
        for (ArmorStand as : holograms) {
            if (!as.isDead()) {
                as.remove();
            }
        }
        holograms.clear();
    }

    // Обновляем список плашек рядом со спавном
    private void updateScoreboard() {
        removeHolograms();

        World mainWorld = Bukkit.getWorld("world");
        if (mainWorld == null) return;

        Location baseLocation = mainWorld.getSpawnLocation().clone().add(2, 1, 0); // чуть правее и чуть выше спавна

        // Сортируем игроков по очкам по убыванию
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(playerScores.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int line = 0;
        for (int i = 0; i < sortedList.size(); i++) {
            String playerName = sortedList.get(i).getKey();
            int score = sortedList.get(i).getValue();

            Location hologramLocation = baseLocation.clone().add(0, -line * 0.3, 0); // понижаем по Y для каждой строки

            ArmorStand as = (ArmorStand) mainWorld.spawnEntity(hologramLocation, EntityType.ARMOR_STAND);
            as.setCustomNameVisible(true);
            as.setCustomName((i + 1) + " " + playerName + " - " + score);
            as.setInvisible(true);
            as.setInvulnerable(true);
            as.setGravity(false);
            as.setMarker(true); // маленький hitbox

            holograms.add(as);
            line++;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            World mainWorld = Bukkit.getWorld("world");
            if (mainWorld == null) {
                player.sendMessage("§cОшибка: основной мир не найден!");
                return true;
            }
            player.teleport(mainWorld.getSpawnLocation());
            player.sendMessage("§aТелепортация на спавн!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("hi")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            Bukkit.broadcastMessage("§6Сервер приветствует игрока §e" + player.getName() + "!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("addscore")) {
            // Команда: /addscore <ник> <очки>
            if (!sender.hasPermission("myplugin.admin")) {
                sender.sendMessage("§cУ вас нет прав для этой команды.");
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage("§cИспользование: /addscore <ник> <очки>");
                return true;
            }

            String targetName = args[0];
            int points;
            try {
                points = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cОчки должны быть числом!");
                return true;
            }

            playerScores.put(targetName, playerScores.getOrDefault(targetName, 0) + points);
            sender.sendMessage("§aДобавлено " + points + " очков игроку " + targetName + ".");
            updateScoreboard();
            return true;
        }

        return false;
    }
}
