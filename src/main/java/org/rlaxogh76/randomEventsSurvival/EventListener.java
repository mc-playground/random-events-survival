package org.rlaxogh76.randomEventsSurvival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
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

public class EventListener implements Listener {

    private final Plugin plugin;
    private final Map<String, GameEvent> eventRegistry = new HashMap<>();

    private BukkitRunnable tickSpeedRevertTask; // 필드로 선언

    private double pendingDragonHealthBonus = 0; // 드래곤 체력 증가량을 저장하는 필드

    public java.util.Set<String> getEventKeys() { // 이벤트 키를 외부에서 조회할 수 있는 메서드
        return java.util.Collections.unmodifiableSet(eventRegistry.keySet()); // 외부에서 수정할 수 없도록 unmodifiableSet으로 반환
    }

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

    private static final EntityType[] SPAWNABLE_MOBS = {
            EntityType.ALLAY, EntityType.ARMADILLO, EntityType.AXOLOTL, EntityType.BAT, EntityType.BEE,
            EntityType.BLAZE, EntityType.BOGGED, EntityType.BREEZE, EntityType.CAMEL, EntityType.CAT,
            EntityType.CAVE_SPIDER, EntityType.CHICKEN, EntityType.COD, EntityType.COW, EntityType.CREEPER,
            EntityType.DOLPHIN, EntityType.DONKEY, EntityType.DROWNED, EntityType.ELDER_GUARDIAN,
            EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.EVOKER, EntityType.FOX, EntityType.FROG,
            EntityType.GHAST, EntityType.GLOW_SQUID, EntityType.GOAT, EntityType.GUARDIAN, EntityType.HOGLIN,
            EntityType.HORSE, EntityType.HUSK, EntityType.LLAMA, EntityType.MAGMA_CUBE, EntityType.MOOSHROOM,
            EntityType.MULE, EntityType.OCELOT, EntityType.PANDA, EntityType.PARROT, EntityType.PHANTOM,
            EntityType.PIG, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER,
            EntityType.POLAR_BEAR, EntityType.PUFFERFISH, EntityType.RABBIT, EntityType.RAVAGER,
            EntityType.SALMON, EntityType.SHEEP, EntityType.SHULKER, EntityType.SILVERFISH, EntityType.SKELETON,
            EntityType.SKELETON_HORSE, EntityType.SLIME, EntityType.SNIFFER, EntityType.SPIDER, EntityType.SQUID,
            EntityType.STRAY, EntityType.STRIDER, EntityType.TADPOLE, EntityType.TRADER_LLAMA,
            EntityType.TROPICAL_FISH, EntityType.TURTLE, EntityType.VEX, EntityType.VILLAGER,
            EntityType.VINDICATOR, EntityType.WANDERING_TRADER, EntityType.WARDEN, EntityType.WITCH,
            EntityType.WITHER_SKELETON, EntityType.WOLF, EntityType.ZOGLIN, EntityType.ZOMBIE,
            EntityType.ZOMBIE_HORSE, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN
            // 엔더 드래곤(ENDER_DRAGON)은 제외
    };

    private static final String[] BOB_EFFECTS = {
            "speed", "strength", "regeneration", "instant_health",
            "fire_resistance", "water_breathing", "resistance"
    };

    public EventListener(Plugin plugin) {
        this.plugin = plugin;
        registerEvents();
        Bukkit.getPluginManager().registerEvents(this, plugin); // 드래곤 소환 감지를 위한 플러그인
    }

    private void registerEvents() {
        eventRegistry.put("item_remove", this::itemRemove);
        eventRegistry.put("tick_speed_change", this::tickSpeedChange);
        eventRegistry.put("player_hp_change", this::playerHpChange);
        eventRegistry.put("time_change", this::timeChange);
        eventRegistry.put("hotbar_change", this::hotbarChange);
        eventRegistry.put("player_random_effect_give", this::playerRandomEffectGive);
        eventRegistry.put("spawn_tnt", this::spawnTnt);
        eventRegistry.put("yeet", this::yeet);
        eventRegistry.put("freeze_player", this::freezePlayer);
        eventRegistry.put("firework", this::firework);
        eventRegistry.put("tamed_wolf", this::tamedWolf);
        eventRegistry.put("set_spawn", this::setSpawn);
        eventRegistry.put("block_remove", this::blockRemove);
        eventRegistry.put("poop", this::poop);
        eventRegistry.put("spawn_random_mob", this::spawnRandomMob);
        eventRegistry.put("burn_player", this::burnPlayer);

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

    // 실제 이벤트 구현부

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

        target.sendMessage(ChatColor.RED + "[" + removed.getType() + "] 아이템이 인벤토리에서 제거되었습니다.");
    }

    private void tickSpeedChange(Player target, List<Player> allPlayers) {

        int defaultTickSpeed = 20; // 기본 틱 속도

        // 이전에 걸려있던 되돌리기 예약이 있으면 취소 (이벤트가 30초 안에 재당첨될 경우 대비)
        if (tickSpeedRevertTask != null) {
            tickSpeedRevertTask.cancel();
            tickSpeedRevertTask = null;
        }

        // 틱 속도 20 ~ 100 사이에 숫자로 변경 (20 단위)
        int newTickSpeed = 20 + (int) (Math.random() * 9) * 20;

        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.RANDOM_TICK_SPEED, newTickSpeed);
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + "틱 속도가 " + newTickSpeed + "로 변경되었습니다.");

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
        tickSpeedRevertTask.runTaskLater(plugin, 20 * 30); // 20틱(1초) x 30초 = 30초 뒤 실행
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
        target.sendMessage(ChatColor.RED + "주변에 TNT가 스폰되었습니다.");
    }

    private void yeet(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        target.setVelocity(new Vector(0, 100, 0)); // 위로 100블럭 발사
        target.sendRichMessage("<rainbow>하늘로 날아갑니다!");
    }

    private void dragonGetHp(Player target, List<Player> allPlayers) {
        // 엔더 월드에 있는 드래곤에게 영구적으로 체력 100을 추가.
        // 드래곤이 아직 소환되지 않았거나(처치되어 사라진 상태 포함) 없으면,
        // 증가분을 적립해뒀다가 소환 시(onDragonSpawn) 한꺼번에 적용한다.

        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld == null) {
            plugin.getLogger().warning("엔더 월드를 찾을 수 없습니다.");
            return;
        }

        EnderDragon dragon = endWorld.getEnderDragonBattle() != null
                ? endWorld.getEnderDragonBattle().getEnderDragon()
                : null;

        if (dragon == null) {
            pendingDragonHealthBonus += 100;
            if (target != null && target.isOnline()) {
                target.sendMessage(ChatColor.GRAY + "엔더 드래곤이 아직 소환되지 않아, 다음 소환 시 체력이 "
                        + (int) pendingDragonHealthBonus + " 증가합니다.");
            }
            return;
        }

        applyDragonHealthBonus(dragon, 100);

        if (target != null && target.isOnline()) {
            target.sendMessage(ChatColor.GREEN + "드래곤의 체력이 영구적으로 증가했습니다!");
        }
    }

    // 엔더 드래곤이 (재)소환될 때 호출됨 - 크리스탈을 통한 재소환도 CreatureSpawnEvent로 감지됨
    @EventHandler
    public void onDragonSpawn(CreatureSpawnEvent event) {
        if (pendingDragonHealthBonus <= 0)
            return;

        if (!(event.getEntity() instanceof EnderDragon dragon))
            return;

        double bonus = pendingDragonHealthBonus;
        pendingDragonHealthBonus = 0; // 먼저 초기화해서 중복 적용 방지

        applyDragonHealthBonus(dragon, bonus);

        Bukkit.broadcastMessage(ChatColor.GREEN + "엔더 드래곤의 체력이 영구적으로 " + (int) bonus + " 증가했습니다!");
    }

    // 최대 체력과 현재 체력을 함께 올려서 "영구적으로" 체력이 증가하도록 처리
    private void applyDragonHealthBonus(EnderDragon dragon, double amount) {
        double newMax = dragon.getMaxHealth() + amount;
        dragon.setMaxHealth(newMax);
        dragon.setHealth(newMax);
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

        Bukkit.broadcastMessage(ChatColor.DARK_RED + "Bob이 소환되었습니다!");
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

    private void freezePlayer(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 255)); // 10초 동안 이동 불가
        target.sendMessage(ChatColor.AQUA + "너무 추워서 얼어붙었습니다..");
    }

    private void firework(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        target.getWorld().spawn(target.getLocation(), org.bukkit.entity.Firework.class, firework -> {
            org.bukkit.inventory.meta.FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                    .withColor(org.bukkit.Color.RED)
                    .withFade(org.bukkit.Color.YELLOW)
                    .with(org.bukkit.FireworkEffect.Type.BALL)
                    .build());
            meta.setPower(2);
            firework.setFireworkMeta(meta);
        });

        target.sendRichMessage("<rainbow>이벤트 당첨을 축하드립니다!");
    }

    private void tamedWolf(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        org.bukkit.entity.Wolf wolf = target.getWorld().spawn(target.getLocation(), org.bukkit.entity.Wolf.class);
        wolf.setOwner(target);
        wolf.setTamed(true);
        wolf.setCustomName(ChatColor.GRAY + target.getName() + "'의 늑대");
        wolf.setCustomNameVisible(true);

        target.sendMessage(ChatColor.GREEN + "당신을 따르는 늑대가 소환되었습니다!");
    }

    private void setSpawn(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        target.setBedSpawnLocation(target.getLocation(), true);
        target.sendMessage(ChatColor.YELLOW + "스폰 지점이 현재 위치로 설정되었습니다!");
    }

    private void blockRemove(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        org.bukkit.block.Block block1 = target.getLocation().getBlock();
        org.bukkit.block.Block block2 = block1.getRelative(org.bukkit.block.BlockFace.DOWN);

        block1.setType(Material.AIR);
        block2.setType(Material.AIR);

        target.sendMessage(ChatColor.RED + "너무 무거워서 바닥이 무너졌습니다!");
    }

    private void poop(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        ItemStack poopItem = new ItemStack(Material.BROWN_DYE, 64);
        ItemMeta meta = poopItem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("똥"); // 색을 넣으면 자동으로 이탤릭 해제됨
            meta.setLore(List.of("된장 냄새가 난다.. 아닌가?"));
            poopItem.setItemMeta(meta);
        }

        target.getInventory().addItem(poopItem);

        target.sendMessage(ChatColor.DARK_RED + "뿌직");
    }

    private void spawnRandomMob(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        EntityType randomMob = SPAWNABLE_MOBS[(int) (Math.random() * SPAWNABLE_MOBS.length)];
        target.getWorld().spawnEntity(target.getLocation(), randomMob);

        target.sendMessage(ChatColor.RED + "랜덤 소환된 몹 :" + randomMob.name());
    }

    private void burnPlayer(Player target, List<Player> allPlayers) {
        if (target == null || !target.isOnline())
            return;

        target.setFireTicks(20 * 5); // 5초 동안 불타게 함
        target.sendMessage(ChatColor.RED + "젠장 에이스 이 공격은 대체 뭐냐!!");
    }

}