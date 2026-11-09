package gg.itzkatze.katze.commands.framework;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHandlerTest {

    @Test
    void registeredHandlerRoutesToTheCurrentCommandAfterReloading() {
        CommandHandler handler = new CommandHandler("migrationtest");
        CommandSourceStack source = consoleSource();
        TestCommand initial = new TestCommand("initial");
        try {
            handler.execute(source, new String[]{"first"});
            assertEquals("first", initial.argument);
            assertEquals(List.of("initial"), handler.suggest(source, new String[]{""}));
        } finally {
            KatzeCommand.unregister(initial);
        }

        TestCommand replacement = new TestCommand("replacement");
        try {
            handler.execute(source, new String[]{"second"});
            assertEquals("second", replacement.argument);
            assertEquals(List.of("replacement"), handler.suggest(source, new String[]{""}));
        } finally {
            KatzeCommand.unregister(replacement);
        }
        assertTrue(handler.suggest(source, new String[]{""}).isEmpty());
    }

    @Test
    void commandNamesAndAliasesRemainStableWithTurkishServerLocale() {
        TestCommand command = new TestCommand("suggestion");
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertTrue(command.matches("MIGRATION_ALIAS"));
            assertTrue(KatzeCommand.getRegisteredCommandNames().contains("migrationtest"));
            assertTrue(KatzeCommand.getRegisteredCommandNames().contains("migration_alias"));
        } finally {
            Locale.setDefault(original);
            KatzeCommand.unregister(command);
        }
    }

    private static CommandSourceStack consoleSource() {
        ConsoleCommandSender sender = (ConsoleCommandSender) Proxy.newProxyInstance(
                ConsoleCommandSender.class.getClassLoader(), new Class<?>[]{ConsoleCommandSender.class},
                (proxy, method, args) -> { throw new AssertionError("Unexpected sender call: " + method); });
        return (CommandSourceStack) Proxy.newProxyInstance(
                CommandSourceStack.class.getClassLoader(), new Class<?>[]{CommandSourceStack.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getSender")) return sender;
                    throw new AssertionError("Unexpected source call: " + method);
                });
    }

    private static final class TestCommand extends KatzeCommand {

        private final String suggestion;
        private String argument;

        private TestCommand(String suggestion) {
            super("migrationtest", List.of("migration_alias"), CallerType.CONSOLE,
                    "/migrationtest", PermissionDefault.TRUE, "Migration test");
            this.suggestion = suggestion;
        }

        @Override
        protected void executeCommand(CommandCaller caller, String[] args) {
            argument = args[0];
        }

        @Override
        protected List<String> tabComplete(CommandCaller caller, String[] args) {
            return List.of(suggestion);
        }
    }
}
