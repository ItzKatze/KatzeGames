package gg.itzkatze.katze.recipe;

import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeCommand extends KatzeCommand {

    public RecipeCommand() {
        super(
                "recipe",
                List.of(),
                CallerType.PLAYER,
                "/recipe <item>",
                PermissionDefault.TRUE,
                "Opens the crafting recipe GUI for the specified item"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (!(caller instanceof KatzePlayer player)) return;

        if (args.length == 0) {
            sendUsage(caller);
            return;
        }

        Material material = Material.matchMaterial(args[0]);

        if (material == null || !material.isItem() || material.isAir()) {
            caller.sendMessage(Component.translatable("katze.recipe.material_not_found"));
            return;
        }

        ItemStack stack = new ItemStack(material);
        new CraftingRecipeGUI(stack).open(player);
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toUpperCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (Material mat : Material.values()) {
                if (mat.isItem() && mat.name().startsWith(prefix)) {
                    suggestions.add(mat.name().toLowerCase(Locale.ROOT));
                }
            }
            return suggestions;
        }
        return List.of();
    }
}
