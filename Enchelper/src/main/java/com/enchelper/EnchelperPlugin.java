package com.enchelper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.List;
import java.util.Map;

public class EnchelperPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("✅ Enchelper оптимизирован и запущен!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyEffects(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> applyEffects(event.getPlayer()), 5L);
    }

    @EventHandler
    public void onArmorBreak(PlayerItemBreakEvent event) {
        applyEffects(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Bukkit.getScheduler().runTaskLater(this, () -> applyEffects((Player) event.getWhoClicked()), 2L);
        }
    }

    // Обновляем эффекты левитации, силы, скорости, сопротивления
    private void applyEffects(Player player) {
        applyRunawayEffect(player);
        applyKonchennyiEffect(player);
        applyAbsolutEffect(player);
        applyFastFeetEffect(player);
    }
    // --- Pfпрет чар --- 
    private static final Map<String, List<Material>> ENCHANT_RESTRICTIONS = new HashMap<>() {{
        put("enchelper:konchenniy", Arrays.asList(Material.TOTEM_OF_UNDYING));
        put("enchelper:konchenniy2", Arrays.asList(Material.TOTEM_OF_UNDYING));

        put("enchelper:nuvorish", Arrays.asList(Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                                                Material.IRON_SWORD, Material.STONE_SWORD, Material.WOODEN_SWORD,
                                                Material.GOLDEN_SWORD,
                                                Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                                                Material.IRON_AXE, Material.STONE_AXE, Material.WOODEN_AXE,
                                                Material.GOLDEN_AXE));
        put("enchelper:nuvorish2", ENCHANT_RESTRICTIONS.get("enchelper:nuvorish"));

        put("enchelper:bur", Arrays.asList(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                                            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                                            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                                            Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL));
        put("enchelper:bur2", ENCHANT_RESTRICTIONS.get("enchelper:bur"));
        put("enchelper:bur3", ENCHANT_RESTRICTIONS.get("enchelper:bur"));

        put("enchelper:ksilofilia", Arrays.asList(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                                                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE));

        put("enchelper:backup_plan", Arrays.asList(Material.WOODEN_PICKAXE));
    }};

    private boolean isAllowedFor(String enchantId, Material type) {
            List<Material> allowed = ENCHANT_RESTRICTIONS.get(enchantId);
            return allowed != null && allowed.contains(type);
        }

        private List<String> getCustomEnchantIds(ItemStack stack) {
        List<String> list = new ArrayList<>();
        // твоя реализация: например, чекаешь через getEnchantmentLevel для каждого id
        for (String id : ENCHANT_RESTRICTIONS.keySet()) {
            if (getEnchantmentLevel(null, id, new ItemStack[]{stack}) > 0) {
                list.add(id);
            }
        }
        return list;
    }


    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack item = event.getInventory().getItem(0);   // предмет
        ItemStack addition = event.getInventory().getItem(1); // книга
        if (item == null || addition == null) return;

        // Получаем все кастомные чары на книге
        List<String> enchants = getCustomEnchantIds(addition);
        for (String id : enchants) {
            if (!isAllowedFor(id, item.getType())) {
                event.setResult(null); // ❌ отменяем результат
                return;
            }
        }
    }


    // --- Левитация (runaway) ---
    private void applyRunawayEffect(Player player) {
        int level = getEnchantmentLevel(player, "enchelper:runaway", player.getInventory().getArmorContents());
        if (level > 0) {
            int potionLevel = level - 1;
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 999999, potionLevel, true, false, true));
            getLogger().info("🟢 [" + player.getName() + "] Левитация уровня " + level + " применена");
        } else {
            player.removePotionEffect(PotionEffectType.LEVITATION);
            getLogger().info("🔴 [" + player.getName() + "] Левитация снята");
        }
    }

    // --- Конченный: сопротивление, сила, скорость (тотем в левой руке) ---
    private void applyKonchennyiEffect(Player player) {
        ItemStack leftHand = player.getInventory().getItemInOffHand();
        if (leftHand != null && leftHand.getType() == Material.TOTEM_OF_UNDYING) {
            int level = getEnchantmentLevel(player, "enchelper:konchennyi", new ItemStack[]{leftHand});
            if (level > 0) {
                // Сопротивление и сила + скорость (зависит от уровня)
                int resistanceLevel = Math.min(level, 2) - 1; // 0 или 1
                int strengthLevel = Math.min(level, 2) - 1;   // 0 или 1
                int speedLevel = (level == 2) ? 0 : 1;       // уровень скорости 0 или 1

                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 999999, resistanceLevel, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 999999, strengthLevel, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, speedLevel, true, false, true));
                getLogger().info("🟢 [" + player.getName() + "] Эффекты Конченный lvl " + level + " применены");
                return;
            }
        }
        // Если нет тотема или зачарования - убираем эффекты
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        getLogger().info("🔴 [" + player.getName() + "] Эффекты Конченный сняты");
    }

    // --- Абсолют (нагрудник): сила 2, скорость 1 ---
    private void applyAbsolutEffect(Player player) {
        int level = getEnchantmentLevel(player, "enchelper:absolut", new ItemStack[]{player.getInventory().getChestplate()});
        if (level > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 999999, 1, true, false, true)); // сила 2 (уровни с 0)
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, true, false, true)); // скорость 1
            getLogger().info("🟢 [" + player.getName() + "] Эффекты Абсолют применены");
        } else {
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.SPEED);
            getLogger().info("🔴 [" + player.getName() + "] Эффекты Абсолют сняты");
        }
    }

    // --- Быстрые ноги (сапоги): скорость 3 ---
    private void applyFastFeetEffect(Player player) {
        int level = getEnchantmentLevel(player, "enchelper:fast_feet", new ItemStack[]{player.getInventory().getBoots()});
        if (level > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 2, true, false, true)); // скорость 3
            getLogger().info("🟢 [" + player.getName() + "] Эффекты Быстрые ноги применены");
        } else {
            player.removePotionEffect(PotionEffectType.SPEED);
            getLogger().info("🔴 [" + player.getName() + "] Эффекты Быстрые ноги сняты");
        }
    }

    // Получение уровня зачарования по ключу из массива предметов
    private int getEnchantmentLevel(Player player, String enchantKey, ItemStack[] items) {
        int maxLevel = 0;
        for (ItemStack item : items) {
            if (item == null) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> ench : meta.getEnchants().entrySet()) {
                    if (ench.getKey().getKey().toString().equals(enchantKey) && ench.getValue() > maxLevel) {
                        maxLevel = ench.getValue();
                    }
                }
            }
        }
        return maxLevel;
    }
    @EventHandler
    public void onPlayerHitBedrock(PlayerInteractEvent event) {
        // Проверяем, что клик был ЛКМ по блоку
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null) return;

        // Проверяем, что целевой блок — бедрок
        if (block.getType() != Material.BEDROCK) return;

        // Проверяем кастомное зачарование
        int backupPlanLevel = getEnchantmentLevel(player, "enchelper:backup_plan", new ItemStack[]{tool});
        if (backupPlanLevel <= 0) return;

        // Проверяем, что это кирка
        if (!tool.getType().toString().contains("PICKAXE")) return;

        // ✅ Ломаем бедрок безопасно
        block.setType(Material.AIR);

        // ✅ Уничтожаем текущий инструмент (только один)
        tool.setAmount(0);

        // ✅ Обновляем инвентарь
        player.updateInventory();

        // Логируем
        getLogger().info("🟢 [" + player.getName() + "] сломал бедрок с запасным планом, кирка уничтожена!");
    }
    // --- Событие ломания блока (для бур, ксилофилии и запасного плана) ---
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null) return;
       
        // БУР
        int burLevel = getEnchantmentLevel(player, "enchelper:bur", new ItemStack[]{tool});
        if (burLevel > 0 && (tool.getType().toString().contains("PICKAXE") || tool.getType().toString().contains("SHOVEL"))) {

            // --- Размер области ---
            int radius = 1;   // половина ширины (1 → 3x3)
            int yRadius = 0;  // высота (0 → 1 блок)
            if (burLevel >= 2) yRadius = 1; // уровни 2 и 3 → 3 блока по Y

            Material originalType = event.getBlock().getType();

            event.setCancelled(true);

            int cx = event.getBlock().getX();
            int cy = event.getBlock().getY();
            int cz = event.getBlock().getZ();

            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int y = cy - yRadius; y <= cy + yRadius; y++) {
                    for (int z = cz - radius; z <= cz + radius; z++) {
                        Block block = event.getBlock().getWorld().getBlockAt(x, y, z);
                        Material type = block.getType();

                        // --- Проверка можно ли копать ---
                        if (burLevel == 3 || canToolMineBlock(tool.getType(), type)) {
                            block.breakNaturally(tool);
                        }
                    }
                }
            }

            damageItem(tool, player);
            getLogger().info("🟢 [" + player.getName() + "] Использует бур lvl " + burLevel);
            return;
        }

        // Ксилофилия
        int xylophiliaLevel = getEnchantmentLevel(player, "enchelper:xylophilia", new ItemStack[]{tool});
        if (xylophiliaLevel > 0 && tool.getType().toString().contains("AXE")) {
            Block block = event.getBlock();
            if (block.getType().name().endsWith("_LOG")) {
                event.setCancelled(true);
                breakTreeUpwards(block, tool);
                damageItem(tool, player);
                getLogger().info("🟢 [" + player.getName() + "] Использует ксилофилию");
            }
        }
    }


    private void damageItem(ItemStack item, Player player) {
        if (item == null) return;
        if (!(item.getType().getMaxDurability() > 0)) return;

        short durability = item.getDurability();
        durability++;
        if (durability >= item.getType().getMaxDurability()) {
            // ломаем предмет
            player.getInventory().remove(item);
            player.updateInventory();
            getLogger().info("🔴 [" + player.getName() + "] Инструмент сломан");
        } else {
            item.setDurability(durability);
        }
    }

    private void breakTreeUpwards(Block block, ItemStack tool) {
        Block current = block;
        while (current != null && current.getType().name().endsWith("_LOG")) {
            current.breakNaturally(tool);
            current = current.getWorld().getBlockAt(current.getX(), current.getY() + 1, current.getZ());
        }
    }

    // Проверка, может ли инструмент копать блок (упрощённо)
    private boolean canToolMineBlock(Material toolType, Material blockType) {
    // --- Кирка ---
    if (toolType.toString().contains("PICKAXE")) {
        // список для кирки (можешь дополнять)
        return blockType.toString().contains("STONE") ||
               blockType.toString().contains("COBBLESTONE") ||
               blockType.toString().contains("ORE") ||
               blockType == Material.NETHERRACK ||
               blockType == Material.OBSIDIAN ||
               blockType == Material.BRICKS ||
               blockType == Material.ANDESITE ||
               blockType == Material.DIORITE ||
               blockType == Material.GRANITE;
    }

    // --- Лопата ---
    if (toolType.toString().contains("SHOVEL")) {
        return blockType == Material.DIRT ||
               blockType == Material.GRASS_BLOCK ||
               blockType == Material.SAND ||
               blockType == Material.RED_SAND ||
               blockType == Material.GRAVEL ||
               blockType == Material.CLAY ||
               blockType == Material.SOUL_SAND ||
               blockType == Material.SOUL_SOIL;
    }

    return false;
}

}
