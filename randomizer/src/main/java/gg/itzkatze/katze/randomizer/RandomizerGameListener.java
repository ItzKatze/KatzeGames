package gg.itzkatze.katze.randomizer;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.team.KatzeTeam;
import gg.itzkatze.katze.util.WorldStorage;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class RandomizerGameListener implements Listener {

    private static final String FILE_NAME = "randomizer_mappings.json";

    private final RandomizerMappings mappings;
    private final BiFunction<Material, Integer, ItemStack> itemFactory;
    private final Map<Location, ExplosionDrops> explosionDrops = new HashMap<>();
    private final Map<Location, List<ItemStack>> brokenContainers = new HashMap<>();
    private int explosionTick = -1;
    private int containerTick = -1;

    private record ExplosionDrops(Player player, List<ItemStack> contents) {}

    public RandomizerGameListener() {
        this(buildItemPool());
    }

    RandomizerGameListener(List<Material> itemPool) {
        this(itemPool, ItemStack::new);
    }

    RandomizerGameListener(List<Material> itemPool, BiFunction<Material, Integer, ItemStack> itemFactory) {
        mappings = new RandomizerMappings(itemPool);
        this.itemFactory = itemFactory;
    }

    public void saveToFile() {
        WorldStorage.save(FILE_NAME, mappings.save());
    }

    public void loadFromFile() {
        if (!WorldStorage.exists(FILE_NAME)) return;
        RandomizerMappings.SaveData data = WorldStorage.load(FILE_NAME, RandomizerMappings.SaveData.class);
        if (data != null) mappings.load(data);
    }

    public void resetMappings() {
        mappings.clear();
        explosionDrops.clear();
        brokenContainers.clear();
        explosionTick = -1;
        containerTick = -1;
        WorldStorage.delete(FILE_NAME);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        if (!blockDropsEnabled()) return;
        List<ItemStack> contents = containerTick == Bukkit.getCurrentTick()
                ? brokenContainers.remove(event.getBlock().getLocation()) : null;
        if (contents == null) contents = containerContents(event.getBlockState());
        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            ItemStack replacement = randomizeBlockDrop(stack, contents, event.getPlayer());
            if (replacement != stack) item.setItemStack(replacement);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!blockDropsEnabled() || !event.isDropItems()) return;
        int tick = Bukkit.getCurrentTick();
        if (containerTick != tick) brokenContainers.clear();
        containerTick = tick;
        BlockState state = event.getBlock().getState();
        if (state instanceof Container) brokenContainers.put(state.getLocation(), containerContents(state));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreakBlock(BlockBreakBlockEvent event) {
        if (!blockDropsEnabled()) return;
        List<ItemStack> contents = containerContents(event.getBlock().getState());
        event.getDrops().replaceAll(stack -> randomizeBlockDrop(stack, contents, null));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        trackExplosion(event.blockList(), responsiblePlayer(event.getEntity()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        trackExplosion(event.blockList(), null);
    }

    private void trackExplosion(List<Block> blocks, Player player) {
        if (!blockDropsEnabled()) return;
        int tick = Bukkit.getCurrentTick();
        if (explosionTick != tick) explosionDrops.clear();
        explosionTick = tick;
        for (Block block : blocks) {
            explosionDrops.put(block.getLocation(), new ExplosionDrops(player, containerContents(block.getState())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionItemSpawn(ItemSpawnEvent event) {
        if (explosionDrops.isEmpty()) return;
        if (explosionTick != Bukkit.getCurrentTick()) {
            explosionDrops.clear();
            return;
        }
        ExplosionDrops drops = explosionDrops.get(event.getLocation().toBlockLocation());
        if (drops == null || !blockDropsEnabled()) return;
        Item item = event.getEntity();
        ItemStack stack = item.getItemStack();
        ItemStack replacement = randomizeBlockDrop(stack, drops.contents(), drops.player());
        if (replacement != stack) item.setItemStack(replacement);
    }

    private boolean blockDropsEnabled() {
        return ChallengeManager.isGameplayActive(ChallengeType.RANDOMIZER) && RandomizerSettings.isBlockDropsEnabled();
    }

    private Player responsiblePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof TNTPrimed tnt) return responsiblePlayer(tnt.getSource());
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return responsiblePlayer(shooter);
        }
        return entity instanceof LivingEntity living ? living.getKiller() : null;
    }

    private List<ItemStack> containerContents(BlockState state) {
        List<ItemStack> contents = new ArrayList<>();
        if (state instanceof Container container) {
            for (ItemStack stack : container.getSnapshotInventory().getContents()) {
                if (stack != null && !stack.isEmpty()) contents.add(stack.clone());
            }
        }
        return contents;
    }

    ItemStack randomizeBlockDrop(ItemStack stack, List<ItemStack> contents, Player player) {
        if (stack == null || stack.isEmpty()) return stack;
        int storedAmount = contents.stream().filter(content -> content.isSimilar(stack)).mapToInt(ItemStack::getAmount).sum();
        if (storedAmount < stack.getAmount()) return randomize(stack, player);
        int remaining = stack.getAmount();
        for (ItemStack content : contents) {
            if (content.isSimilar(stack)) {
                int preserved = Math.min(remaining, content.getAmount());
                content.setAmount(content.getAmount() - preserved);
                remaining -= preserved;
                if (remaining == 0) return stack;
            }
        }
        return stack;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (!ChallengeManager.isGameplayActive(ChallengeType.RANDOMIZER)) return;
        if (!RandomizerSettings.isMobDropsEnabled()) return;
        randomize(event.getDrops(), event.getEntity().getKiller());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (!ChallengeManager.isGameplayActive(ChallengeType.RANDOMIZER)) return;
        Player player = event.getEntity() instanceof Player p ? p : null;
        ExplosionDrops containerDrops = null;
        List<ItemStack> contents = null;
        if (event.getInventoryHolder() instanceof BlockState state) {
            int tick = Bukkit.getCurrentTick();
            if (containerTick == tick) contents = brokenContainers.get(state.getLocation());
            if (explosionTick == tick) {
                containerDrops = explosionDrops.get(state.getLocation());
                if (containerDrops != null) {
                    player = containerDrops.player();
                    contents = containerDrops.contents();
                }
            }
        }
        if (RandomizerSettings.isChestLootEnabled()) randomize(event.getLoot(), player);
        if (contents != null) {
            for (ItemStack stack : event.getLoot()) {
                if (stack != null && !stack.isEmpty()) contents.add(stack.clone());
            }
        }
    }

    void randomize(List<ItemStack> drops, Player player) {
        drops.replaceAll(stack -> randomize(stack, player));
    }

    private ItemStack randomize(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty()) return stack;
        Material replacement = mappings.replace(mappingOwner(player), stack.getType(),
                () -> SessionPersistence.requestSave(ChallengeType.RANDOMIZER));
        return itemFactory.apply(replacement, stack.getAmount());
    }

    private String mappingOwner(Player player) {
        if (player == null) return null;
        return mappingOwner(player, RandomizerSettings.isSharedMappingsEnabled(), RandomizerSettings.isTeamMappingsEnabled());
    }

    static String mappingOwner(Player player, boolean sharedMappings, boolean teamMappings) {
        if (player == null) return null;
        if (teamMappings) {
            KatzeTeam team = KatzePlayer.adapt(player.getUniqueId()).map(KatzePlayer::getTeam).orElse(null);
            if (team != null) return "team:" + team.getId();
        } else if (sharedMappings) {
            return null;
        }
        return "player:" + player.getUniqueId();
    }

    private static List<Material> buildItemPool() {
        List<Material> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir()) materials.add(material);
        }
        Collections.shuffle(materials);
        return materials;
    }
}
