package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventListener {

    private final Plugin plugin;
    private final Map<String, GameEvent> eventRegistry = new HashMap<>();

    private BukkitRunnable tickSpeedRevertTask; // 필드로 선언

    private static final String[] POTION_EFFECTS = {
            "absorption", "bad_omen", "blindness", "conduit_power", "darkness",
            "fire_resistance", "glowing", "haste", "health_boost", "hero_of_the_village",
            "hunger", "infested", "instant_damage", "instant_health", "invisibility",
            "jump_boost", "levitation", "luck", "mining_fatigue", "nausea",
            "night_vision", "oozing", "poison", "raid_omen", "regeneration",
            "resistance", "saturation", "slowness", "slow_falling", "speed",
            "strength", "trial_omen", "unluck", "water_breathing", "weakness",
            "weaving", "wind_charged", "wither"
    };

    private static final String[] BOB_EFFECTS = {
            "speed", "strength", "regeneration", "instant_health",
            "fire_resistance", "water_breathing", "resistance"
    };

    public EventListener(Plugin plugin) {
        this.plugin = plugin;
        registerEvents();
    }

    private void registerEvents() {
        eventRegistry.put("item_remove", this::itemRemove);
        eventRegistry.put("tick_speed_change", this::tickSpeedChange);
        eventRegistry.put("player_hp_change", this::playerHpChange);
        eventRegistry.put("time_change", this::timeChange);
        eventRegistry.put("hotbar_change", this::hotbarChange);
        eventRegistry.put("player_random_effect_give", this::playerRandomEffectGive);
        eventRegistry.put("spawn_tnt", this::spawnTnt);
        eventRegistry.put("yeet", this::yeet); // "yeet" 이벤트도 spawn_tnt와 동일하게 처리

        eventRegistry.put("dragon_get_hp", this::dragonGetHp);
        eventRegistry.put("spawn_bob", this::spawnBob);
    }

    public void trigger(String eventKey, Player target) {
        GameEvent event = eventRegistry.get(eventKey);
        if (event == null) {
            plugin.getLogger().warning("등록되지 않은 이벤트 키: " + eventKey);
            return;
        }

        List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        event.execute(target, allPlayers);
    }

    // ===== 실제 이벤트 구현부 =====

    private void itemRemove(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        PlayerInventory inv = target.getInventory();
        ItemStack[] contents = inv.getContents();

        List<Integer> filledSlots = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                filledSlots.add(i);
            }
        }

        if (filledSlots.isEmpty())
            return;

        int slot = filledSlots.get((int) (Math.random() * filledSlots.size()));
        ItemStack removed = contents[slot];
        inv.setItem(slot, null);

        target.sendMessage(ChatColor.RED + "아이템 [" + removed.getType() + "] 이(가) 뒤주에 갇혔습니다.");
    }

    private void tickSpeedChange(Player target, List<Player> allPlayers) {

        int defaultTickSpeed = 20; // 기본 틱 속도

        // 이전에 걸려있던 되돌리기 예약이 있으면 취소 (이벤트가 30초 안에 재당첨될 경우 대비)
        if (tickSpeedRevertTask != null) {
            tickSpeedRevertTask.cancel();
            tickSpeedRevertTask = null;
        }

        // 틱 속도 20 ~ 100 사이에 숫자로 변경 (10 단위)
        int newTickSpeed = 20 + (int) (Math.random() * 9) * 10;

        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, newTickSpeed);
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + "랜덤 틱 속도가 " + newTickSpeed + "로 변경되었습니다.");

        tickSpeedRevertTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    world.setGameRule(GameRule.RANDOM_TICK_SPEED, defaultTickSpeed);
                }
                Bukkit.broadcastMessage(ChatColor.YELLOW + "랜덤 틱 속도가 원래 속도(" + defaultTickSpeed + ")로 되돌아왔습니다.");
                tickSpeedRevertTask = null;
            }
        };
        tickSpeedRevertTask.runTaskLater(plugin, 20 * 30); // 20틱(1초) × 30초 = 30초 뒤 실행
    }

    private void playerHpChange(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;
        // target 체력 1~ 20 사이 랜덤으로 변경
        target.setHealth(Math.max(1, Math.min(20, target.getHealth() + (Math.random() < 0.5 ? 1 : -1))));

        target.sendMessage(ChatColor.GREEN + "체력이 변경되었습니다.");
    }

    private void timeChange(Player target, List<Player> allPlayers) {
        // 전역 이벤트: 월드 시간 변경

        for (World world : Bukkit.getWorlds()) {
            long newTime = (long) (Math.random() * 24000); // 0 ~ 23999 사이의 랜덤 시간
            world.setTime(newTime);
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + "월드 시간이 변경되었습니다.");
    }

    private void hotbarChange(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;
        // target 핫바 아이템 셔플
        PlayerInventory inv = target.getInventory();
        ItemStack[] contents = inv.getContents();
        List<ItemStack> nonEmptyItems = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && item.getType() != Material.AIR) {
                nonEmptyItems.add(item);
            }
        }
        if (nonEmptyItems.isEmpty()) {
            return;
        }
        Collections.shuffle(nonEmptyItems);
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                inv.setItem(i, nonEmptyItems.remove(0));
            }
        }

        target.sendMessage(ChatColor.GREEN + "핫바 아이템 위치가 섞였습니다.");
    }

    private void playerRandomEffectGive(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        String effectId = POTION_EFFECTS[(int) (Math.random() * POTION_EFFECTS.length)];
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effectId));

        if (type == null) {
            plugin.getLogger().warning("알 수 없는 포션 효과: " + effectId);
            return;
        }

        int durationTicks = 15 * 20; // 15초 (1초 = 20틱)
        int amplifier = 0; // 앰플리파이어 0 = 1레벨

        target.addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
        target.sendMessage(
                ChatColor.LIGHT_PURPLE + target.getName() + "님에게 " + "효과가 부여되었습니다: " + type.getKey().getKey());
    }

    private void spawnTnt(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;
        // target 주변에 TNT 스폰

        target.getWorld().spawn(target.getLocation(), org.bukkit.entity.TNTPrimed.class, tnt -> {
            tnt.setFuseTicks(40); // 2초 후 폭발
            tnt.setYield(4.0f); // 폭발 범위
        });
        target.sendMessage(ChatColor.RED + "주변에 TNT가 스폰되었습니다!");
    }

    private void yeet(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        target.setVelocity(new Vector(0, 100, 0)); // 위로 100블럭 발사
        target.sendRichMessage("<rainbow>위로 발사!!");
    }

    private void dragonGetHp(Player target, List<Player> allPlayers) {
        // 엔더 월드에 있는 드래곤에게 영구적으로 체력 100을 추가

        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld == null) {
            plugin.getLogger().warning("엔더 월드를 찾을 수 없습니다.");
            return;
        }

        endWorld.getEnderDragonBattle().getEnderDragon().setHealth(
                Math.min(endWorld.getEnderDragonBattle().getEnderDragon().getMaxHealth() + 100,
                        endWorld.getEnderDragonBattle().getEnderDragon().getMaxHealth()));
        target.sendMessage(ChatColor.GREEN + "드래곤의 체력이 증가했습니다!");
    }

    private void spawnBob(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        Zombie bob = target.getWorld().spawn(target.getLocation(), Zombie.class);
        bob.setCustomName(ChatColor.DARK_RED + "Bob");
        bob.setCustomNameVisible(true);
        bob.setRemoveWhenFarAway(false);
        bob.setPersistent(true);

        equipBobArmor(bob);
        giveBobEffects(bob);

        Bukkit.broadcastMessage(ChatColor.DARK_RED + "★ Bob이 소환되었습니다! 조심하세요! ★");
    }

    private void equipBobArmor(Zombie bob) {
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);

        String[] protectionIds = { "protection", "blast_protection", "fire_protection", "projectile_protection" };

        for (ItemStack piece : new ItemStack[] { helmet, chestplate, leggings, boots }) {
            for (String id : protectionIds) {
                Enchantment enchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(id));
                if (enchant != null) {
                    piece.addUnsafeEnchantment(enchant, 16);
                }
            }
        }

        EntityEquipment equipment = bob.getEquipment();
        if (equipment == null)
            return;

        equipment.setHelmet(helmet);
        equipment.setChestplate(chestplate);
        equipment.setLeggings(leggings);
        equipment.setBoots(boots);

        // 죽어도 갑옷이 안 떨어지게
        equipment.setHelmetDropChance(0f);
        equipment.setChestplateDropChance(0f);
        equipment.setLeggingsDropChance(0f);
        equipment.setBootsDropChance(0f);
    }

    private void giveBobEffects(Zombie bob) {
        int amplifier = 255; // 255레벨
        int durationTicks = Integer.MAX_VALUE; // 사실상 무한 지속

        for (String id : BOB_EFFECTS) {
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(id));
            if (type == null) {
                plugin.getLogger().warning("알 수 없는 포션 효과: " + id);
                continue;
            }
            bob.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, false));
        }
    }
}