package gg.itzkatze.katze.allitems;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.allitems.data.AllItemsData;
import gg.itzkatze.katze.data.BiomeSimplified;
import gg.itzkatze.katze.data.KatzeAdvancement;
import gg.itzkatze.katze.data.KatzeStructure;
import gg.itzkatze.katze.data.ObjectiveNames;
import gg.itzkatze.katze.data.ObtainableObjectives;
import gg.itzkatze.katze.data.StructureSimplified;
import gg.itzkatze.katze.level.LevelStore;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.StructureScanner;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AllItemsGameListener implements Listener {

    private final AllItemsData data;
    private final Map<NamespacedKey, KatzeAdvancement> advancementByKey = new ConcurrentHashMap<>();
    private BukkitTask structureTask;
    private final Set<UUID> pendingInventoryChecks = new HashSet<>();



    public AllItemsGameListener(AllItemsData data) {
        this.data = data;
        for (KatzeAdvancement advancement : KatzeAdvancement.values()) {
            advancementByKey.put(advancement.getKey(), advancement);
        }
    }

    public void initialize() {
        startStructureTask();
    }

    public void cleanup() {
        if (structureTask != null) {
            structureTask.cancel();
            structureTask = null;
        }
        StructureScanner.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) scheduleInventoryCheck(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) scheduleInventoryCheck(player);
    }

    private void scheduleInventoryCheck(Player player) {
        UUID id = player.getUniqueId();
        if (!KatzeGames.getTimer().isRunning() || !pendingInventoryChecks.add(id)) return;
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            pendingInventoryChecks.remove(id);
            if (player.isOnline() && KatzeGames.getTimer().isRunning() && KatzePlayer.adapt(player).isParticipating()) {
                checkInventory(player);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!KatzeGames.getTimer().isRunning()) {
            return;
        }
        if (!AllItemsSettings.isBiomesEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!KatzePlayer.adapt(player).isParticipating()) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Biome biome = player.getWorld().getBiome(player.getLocation());
        BiomeSimplified simplified = getSimplifiedBiome(biome);
        if (simplified == null) {
            return;
        }
        if (data.recordBiomeFound(player, simplified)) {
            discover(player, AllItemsXp.BIOME, AllItemsObjectiveType.BIOME, ObjectiveNames.of(simplified));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!KatzeGames.getTimer().isRunning()) {
            return;
        }
        if (!AllItemsSettings.isMobsEnabled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }
        if (!KatzePlayer.adapt(killer).isParticipating()) {
            return;
        }

        EntityType type = entity.getType();
        if (!ObtainableObjectives.isMob(type)) {
            return;
        }

        if (data.recordMobFound(killer, type)) {
            discover(killer, AllItemsXp.MOB, AllItemsObjectiveType.MOB, ObjectiveNames.of(type));
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (!KatzeGames.getTimer().isRunning()) {
            return;
        }
        if (!AllItemsSettings.isAdvancementsEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!KatzePlayer.adapt(player).isParticipating()) {
            return;
        }

        KatzeAdvancement advancement = advancementByKey.get(event.getAdvancement().getKey());
        if (advancement == null) {
            return;
        }

        if (data.recordAdvancementFound(player, advancement)) {
            discover(player, AllItemsXp.ADVANCEMENT, AllItemsObjectiveType.ADVANCEMENT, ObjectiveNames.of(advancement));
        }
    }

    private BiomeSimplified getSimplifiedBiome(Biome biome) {
        for (BiomeSimplified simplified : BiomeSimplified.values()) {
            if (simplified.getBiomes().contains(biome)) {
                return simplified;
            }
        }
        return null;
    }

    private void startStructureTask() {
        structureTask = Bukkit.getScheduler().runTaskTimer(KatzeGames.get(), () -> {
            if (!KatzeGames.getTimer().isRunning()) {
                return;
            }


            Registry<Structure> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
            for (KatzePlayer katzePlayer : KatzePlayer.getOnlinePlayers()) {
                if (!katzePlayer.isParticipating()) continue;
                Player player = katzePlayer.getPlayer();
                if (player == null) continue;

                checkInventory(player);
                if (AllItemsSettings.isStructuresEnabled()) checkStructuresForPlayer(player, registry);
            }
        }, 40L, 40L);
    }

    private void checkStructuresForPlayer(Player player, Registry<Structure> registry) {
        for (StructureSimplified simplified : StructureSimplified.values()) {
            if (data.hasPlayerFoundStructure(player.getUniqueId(), simplified)) {
                continue;
            }
            for (KatzeStructure structure : simplified.getStructures()) {
                Structure bukkitStruct = registry.get(structure.getKey());
                if (bukkitStruct != null && checkPlayerLocationInStructure(player, structure.getKey(), bukkitStruct)) {
                    if (data.recordStructureFound(player, simplified)) {
                        discover(player, AllItemsXp.STRUCTURE, AllItemsObjectiveType.STRUCTURE, ObjectiveNames.of(simplified));
                    }
                    return;
                }
            }
        }
    }

    private boolean checkPlayerLocationInStructure(Player player, NamespacedKey structureKey, Structure bukkitStruct) {
        return StructureScanner.contains(player.getLocation(), structureKey);
    }

    private void checkInventory(Player player) {
        if (!AllItemsSettings.isItemsEnabled()) return;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.isEmpty() || !ObtainableObjectives.isItem(stack.getType())) continue;
            if (data.recordItemFound(player, stack.getType())) {
                discover(player, AllItemsXp.ITEM, AllItemsObjectiveType.ITEM, ObjectiveNames.of(stack.getType()));
            }
        }
    }
    private void discover(Player player, AllItemsXp reward, AllItemsObjectiveType type, Component objective) {
        LevelStore.get().grant(player, reward.xp());
        announceDiscovery(player, type, objective);
    }

    private void announceDiscovery(Player player, AllItemsObjectiveType type, Component objective) {
        Component translatedType = Component.translatable("katze.allitems.discovery.type." + type.name().toLowerCase(Locale.ROOT));
        if (AllItemsSettings.isTeamMode()) {
            KatzeGames.broadcast(Component.translatable("katze.allitems.discovery.team",
                    TextArg.text("player", player.getName()),
                    TextArg.component("objective", objective),
                    TextArg.component("type", translatedType)));
        } else {
            KatzeGames.broadcast(Component.translatable("katze.allitems.discovery.player",
                    TextArg.text("player", player.getName()),
                    TextArg.component("objective", objective),
                    TextArg.component("type", translatedType)));
        }
        KatzePlayer.adapt(player).sendMessage(Component.translatable("katze.allitems.discovery.personal",
                TextArg.component("objective", objective)).color(KatzeColor.LIME));
    }
}
