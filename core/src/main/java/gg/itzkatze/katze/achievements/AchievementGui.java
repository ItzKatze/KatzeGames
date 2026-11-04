package gg.itzkatze.katze.achievements;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.GUIItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.text.Texts;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class AchievementGui extends KatzeGUI {
    private static final int[] ENTRIES = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    private static final Set<UUID> REFRESHING = new HashSet<>();
    private final AchievementCategory category;
    private final Achievement detail;
    private int page;

    public AchievementGui() {
        this(null, null, 0);
    }

    private AchievementGui(AchievementCategory category, Achievement detail, int page) {
        super(detail != null ? detail.title() : category == null ? Component.translatable("katze.achievements.title") : category.title(), 54);
        this.category = category;
        this.detail = detail;
        this.page = page;
    }

    public static void refresh(UUID player) {
        if (!(GUI_MAP.get(player) instanceof AchievementGui) || !REFRESHING.add(player)) return;
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            REFRESHING.remove(player);
            if (!(GUI_MAP.get(player) instanceof AchievementGui gui)) return;
            KatzePlayer.adapt(player).ifPresent(viewer -> {
                gui.setItems(viewer);
                gui.updateInventory(viewer);
            });
        });
    }

    @Override
    public boolean allowItemMoving() { return false; }

    @Override
    public void onOpen(KatzePlayer viewer) {
        if (detail != null) title = AchievementService.get().title(viewer.getUuid(), detail);
    }

    @Override
    public void setItems(KatzePlayer viewer) {
        clear();
        fill(ItemStackCreator.fillerItem());
        set(GUIClickableItem.getCloseItem(49));
        set(namePackButton(viewer));
        if (category == null) {
            List<Achievement> all = Arrays.asList(Achievement.values());
            set(fixed(4, summary(viewer, all)));
            AchievementCategory[] categories = AchievementCategory.values();
            for (int i = 0; i < categories.length; i++) {
                AchievementCategory entry = categories[i];
                set(button(ENTRIES[i], ItemStackCreator.getStack(entry.title().color(KatzeColor.YELLOW), entry.icon(),
                        List.of(completion(viewer, Achievement.in(entry)), text("open_category"))),
                        player -> new AchievementGui(entry, null, 0).open(player)));
            }
            return;
        }
        set(button(48, ItemStackCreator.getStack(text("back"), Material.ARROW, List.of()),
                player -> (detail == null ? new AchievementGui() : new AchievementGui(category, null, page)).open(player)));
        if (detail != null) {
            set(fixed(4, render(viewer, detail)));
            for (int i = 0; i < detail.stages().size(); i++) set(fixed(20 + i, stage(viewer, detail.stages().get(i))));
            return;
        }
        List<Achievement> entries = Achievement.in(category);
        set(fixed(4, summary(viewer, entries)));
        int pages = Math.max(1, Math.ceilDiv(entries.size(), ENTRIES.length));
        page = Math.clamp(page, 0, pages - 1);
        for (int i = 0; i < ENTRIES.length; i++) {
            int index = page * ENTRIES.length + i;
            if (index >= entries.size()) break;
            Achievement entry = entries.get(index);
            set(button(ENTRIES[i], render(viewer, entry), player -> new AchievementGui(category, entry, page).open(player)));
        }
        if (entries.isEmpty()) set(fixed(22, ItemStackCreator.getStack(text("empty"), Material.BARRIER, List.of())));
        Component pageIndicator = Component.translatable("katze.achievements.page", TextArg.number("page", page + 1),
                TextArg.number("pages", pages)).color(KatzeColor.LIGHT_GRAY);
        if (page > 0) set(button(45, ItemStackCreator.getStack(text("previous"), Material.ARROW, List.of(pageIndicator)), player -> turn(player, -1)));
        if (page + 1 < pages) set(button(53, ItemStackCreator.getStack(text("next"), Material.ARROW, List.of(pageIndicator)), player -> turn(player, 1)));
    }

    private void turn(KatzePlayer player, int direction) {
        page += direction;
        setItems(player);
        updateInventory(player);
    }

    private ItemStack summary(KatzePlayer viewer, List<Achievement> entries) {
        AchievementProfile profile = AchievementService.get().profile(viewer.getUuid());
        AchievementLevel level = profile.level();
        Component title = Component.translatable("katze.achievements.level", TextArg.number("level", level.level())).color(KatzeColor.GOLD);
        List<Component> lore = List.of(
                Component.translatable("katze.achievements.total_xp", TextArg.number("xp", profile.xp())).color(KatzeColor.LIGHT_GRAY),
                Component.translatable("katze.achievements.level_progress", TextArg.number("into", level.into()),
                        TextArg.number("cost", level.cost()), TextArg.number("level", level.level() + 1)).color(KatzeColor.LIME),
                Component.empty(), completion(viewer, entries));
        return ItemStackCreator.getStack(title, Material.EXPERIENCE_BOTTLE, lore);
    }

    private Component completion(KatzePlayer viewer, List<Achievement> entries) {
        AchievementProfile profile = AchievementService.get().profile(viewer.getUuid());
        return Component.translatable("katze.achievements.completion", TextArg.number("done", entries.stream().mapToInt(profile::completedStages).sum()),
                TextArg.number("total", entries.stream().mapToInt(entry -> entry.stages().size()).sum())).color(KatzeColor.LIGHT_GRAY);
    }

    private ItemStack render(KatzePlayer viewer, Achievement achievement) {
        AchievementService service = AchievementService.get();
        AchievementProfile profile = service.profile(viewer.getUuid());
        int completed = profile.completedStages(achievement);
        boolean done = completed == achievement.stages().size();
        List<Component> lore = new ArrayList<>();
        lore.addAll(description(viewer, achievement.description(), done));
        lore.add(Component.empty());
        if (!done && achievement.stages().size() > 1) {
            lore.add(Component.translatable("katze.achievements.completion", TextArg.number("done", completed),
                    TextArg.number("total", achievement.stages().size())).color(KatzeColor.LIGHT_GRAY));
        }
        AchievementStage next = achievement.stages().stream().filter(stage -> !profile.completed(achievement, stage)).findFirst().orElse(achievement.stages().getLast());
        if (done && achievement.maximum() > 1) {
            lore.add(Component.translatable("katze.achievements.best", TextArg.number("best", profile.best(achievement))).color(KatzeColor.LIGHT_GRAY));
        }
        if (!done) lore.add(reward(next));
        if (!lore.getLast().equals(Component.empty())) lore.add(Component.empty());
        lore.add(text(done ? "completed" : profile.best(achievement) > 0 ? "in_progress" : "locked"));
        if (achievement.stages().size() > 1) lore.add(text("details"));
        return glow(ItemStackCreator.getStack(service.title(viewer.getUuid(), achievement).color(next.rarity().color()), achievement.icon(), lore), done);
    }

    private GUIClickableItem namePackButton(KatzePlayer viewer) {
        boolean custom = AchievementService.get().usesCustomNames(viewer.getUuid());
        Component mode = text(custom ? "names.custom" : "names.auto");
        ItemStack item = ItemStackCreator.getStack(Component.translatable("katze.achievements.names.title", TextArg.component("mode", mode)).color(KatzeColor.YELLOW),
                Material.NAME_TAG, List.of(text("names.hint")));
        return button(46, item, player -> {
            AchievementService service = AchievementService.get();
            if (!service.setCustomNames(player.getUuid(), player.getName(), !service.usesCustomNames(player.getUuid()))) {
                player.sendMessage(text("names.save_failed").color(KatzeColor.RED));
                return;
            }
            new AchievementGui(category, detail, page).open(player);
        });
    }

    private ItemStack stage(KatzePlayer viewer, AchievementStage stage) {
        AchievementProfile profile = AchievementService.get().profile(viewer.getUuid());
        boolean completed = profile.completed(detail, stage);
        List<Component> lore = new ArrayList<>();
        lore.addAll(description(viewer, detail.description(stage), completed));
        lore.add(Component.empty());
        lore.add(reward(stage));
        lore.add(Component.empty());
        lore.add(text(completed ? "completed" : "locked"));
        if (completed) lore.add(Component.translatable("katze.achievements.completed_at", TextArg.text("date", DATE.format(Instant.ofEpochMilli(profile.completedAt(detail, stage))))).color(KatzeColor.LIGHT_GRAY));
        return glow(ItemStackCreator.getStack(Component.translatable("katze.achievements.stage", TextArg.text("stage", stage.id())).color(stage.rarity().color()), detail.icon(), lore), completed);
    }

    private Component reward(AchievementStage stage) {
        return Component.translatable("katze.achievements.reward", TextArg.component("rarity", stage.rarity().title()), TextArg.number("xp", stage.rarity().xp())).color(KatzeColor.GOLD);
    }

    private List<Component> description(KatzePlayer viewer, Component requirement, boolean revealed) {
        String description = PlainTextComponentSerializer.plainText().serialize(Texts.render(requirement, viewer.getPlayer()));
        List<Component> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : description.split("\\s+")) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > 48) {
                lines.add(Component.text(line.toString(), KatzeColor.LIGHT_GRAY));
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(Component.text(line.toString(), KatzeColor.LIGHT_GRAY));
        if (revealed) return lines;
        return lines.stream().<Component>map(wrappedLine -> Component.text(
                PlainTextComponentSerializer.plainText().serialize(wrappedLine).replaceAll("\\S", "x"), KatzeColor.LIGHT_GRAY)
                .decorate(TextDecoration.OBFUSCATED)).toList();
    }

    private static Component text(String key) {
        return Component.translatable("katze.achievements." + key).color(switch (key) {
            case "locked" -> KatzeColor.RED;
            case "completed" -> KatzeColor.LIME;
            default -> KatzeColor.YELLOW;
        });
    }

    private static ItemStack glow(ItemStack item, boolean glowing) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(glowing);
        item.setItemMeta(meta);
        return item;
    }

    private static GUIItem fixed(int slot, ItemStack item) {
        return new GUIItem(slot) {
            @Override
            public ItemStack getItem(KatzePlayer viewer) { return item; }
        };
    }

    private static GUIClickableItem button(int slot, ItemStack item, Consumer<KatzePlayer> action) {
        return new GUIClickableItem(slot) {
            @Override
            public ItemStack getItem(KatzePlayer viewer) { return item; }
            @Override
            public void run(InventoryClickEvent event, KatzePlayer player) { action.accept(player); }
        };
    }
}
