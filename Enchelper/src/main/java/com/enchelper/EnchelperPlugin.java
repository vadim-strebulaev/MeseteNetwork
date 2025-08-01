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
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import java.util.Map;
import java.util.stream.Collectors;

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
    private boolean isEnchantAllowed(ItemStack item, ItemStack addition) {
        if (!(addition.getItemMeta() instanceof EnchantmentStorageMeta meta)) return true;

        for (Enchantment ench : meta.getStoredEnchants().keySet()) {
            String id = ench.getKey().toString().toLowerCase();

            // Конченный 1 и 2 → только TOTEM_OF_UNDYING
            if (id.startsWith("enchelper:konchen") || id.startsWith("enchelper:konchen")) {
                if (item.getType() != Material.TOTEM_OF_UNDYING) return false;
            }

            // Нувориш 1 и 2 → мечи и топоры
            if (id.startsWith("enchelper:nuvorish") || id.startsWith("enchelper:nuvorish")) {
                if (!(item.getType().toString().contains("SWORD") || item.getType().toString().contains("AXE"))) return false;
            }

            // Бур 1,2,3 → кирка и лопата
            if (id.startsWith("enchelper:bur") || id.startsWith("enchelper:bur") || id.startsWith("enchelper:bur")) {
                if (!(item.getType().toString().contains("PICKAXE") || item.getType().toString().contains("SHOVEL"))) return false;
            }

            // Ксилофилия → только топор
            if (id.startsWith("enchelper:ksilofilia")) {
                if (!item.getType().toString().contains("AXE")) return false;
            }

            // Запасной план → только WOODEN_PICKAXE
            if (id.startsWith("enchelper:backup_plan")) {
                if (item.getType() != Material.WOODEN_PICKAXE) return false;
            }
        }

        return true;
    }


    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack item = event.getInventory().getItem(0); // базовый предмет
        ItemStack addition = event.getInventory().getItem(1); // книга
        ItemStack result = event.getResult();

        if (item == null || addition == null || result == null) return;

        // Лог: какие чары есть в книге
        if (addition.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            String enchants = meta.getStoredEnchants().entrySet().stream()
                    .map(e -> e.getKey().getKey() + " " + e.getValue())
                    .collect(Collectors.joining(", "));

            getLogger().info("🔍 [Anvil] " + item.getType() + " + " + addition.getType() +
                    " [" + enchants + "] проверяются на кастомные чары...");
        }

        // --- Проверка: кастомные чары применяются только на правильные предметы ---
        if (!isEnchantAllowed(item, addition)) {
            event.setResult(null); // ❌ отменяем результат (запрещаем крафт)
            getLogger().warning("⛔ [Anvil] Запрещено накладывать эти чары на " + item.getType());
        }
    }


    /**
     * ✅ Доп. защита — запрещаем забирать предмет из слота результата
     */
    @EventHandler
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory)) return;

        if (event.getSlotType() == InventoryType.SlotType.RESULT) {
            AnvilInventory anvil = (AnvilInventory) event.getInventory();
            ItemStack item = anvil.getItem(0);
            ItemStack addition = anvil.getItem(1);

            if (item == null || addition == null) return;

            if (!isValidEnchantCombination(item, addition)) {
                event.setCancelled(true); // ✅ блокируем клик
                event.getWhoClicked().sendMessage("§c❌ Невозможно зачаровать этот предмет данной книгой!");
                getLogger().warning("⛔ [Anvil] Игрок попытался обойти запрет (клик по результату)!");
            }
        }
    }


    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem(); // предмет, который чарится

        // Логируем
        getLogger().info("🔍 [EnchantTable] Попытка зачаровать " + item.getType());

        // Если предмет не разрешён — отменяем
        if (!isAllowedForAnyCustomEnchant(item)) {
            event.setCancelled(true);
            getLogger().warning("⛔ [EnchantTable] Запрещено зачаровывать " + item.getType() + " кастомными чарами!");
        }
    }

    /**
     * Проверка, можно ли наложить кастомный чар с книги на предмет
     */
    private boolean isValidEnchantCombination(ItemStack target, ItemStack book) {
        // Проверяем все кастомные чары
        if (hasCustomEnchant(book, "enchelper:konchenniy") || hasCustomEnchant(book, "enchelper:konchenniy")) {
            return target.getType() == Material.TOTEM_OF_UNDYING;
        }

        if (hasCustomEnchant(book, "enchelper:nuvorish") || hasCustomEnchant(book, "enchelper:nuvorish")) {
            return isSwordOrAxe(target);
        }

        if (hasCustomEnchant(book, "enchelper:bur") || hasCustomEnchant(book, "enchelper:bur") || hasCustomEnchant(book, "enchelper:bur")) {
            return isPickaxeOrShovel(target);
        }

        if (hasCustomEnchant(book, "enchelper:ksilofilia")) {
            return isAxe(target);
        }

        if (hasCustomEnchant(book, "enchelper:backup_plan")) {
            return target.getType() == Material.WOODEN_PICKAXE;
        }

        // Если нет кастомных чаров — разрешаем
        return true;
    }

    /**
     * Проверка, разрешён ли предмет для любого из кастомных чаров (для стола)
     */
    private boolean isAllowedForAnyCustomEnchant(ItemStack item) {
        Material m = item.getType();
        return m == Material.TOTEM_OF_UNDYING || isSwordOrAxe(item) || isPickaxeOrShovel(item) || isAxe(item) || m == Material.WOODEN_PICKAXE;
    }

    /**
     * Проверяет, есть ли кастомный чар на предмете
     */
    private boolean hasCustomEnchant(ItemStack stack, String id) {
        return getEnchantmentLevel(null, id, new ItemStack[]{stack}) > 0;
    }

    /**
     * Утилиты для проверки типа предмета
     */
    private boolean isSwordOrAxe(ItemStack item) {
        String name = item.getType().toString();
        return name.contains("SWORD") || name.contains("AXE");
    }

    private boolean isPickaxeOrShovel(ItemStack item) {
        String name = item.getType().toString();
        return name.contains("PICKAXE") || name.contains("SHOVEL");
    }

    private boolean isAxe(ItemStack item) {
        return item.getType().toString().contains("AXE");
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
