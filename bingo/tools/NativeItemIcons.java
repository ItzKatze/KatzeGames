import java.nio.file.Path;

final class NativeItemIcons extends NativeIcons {
    NativeItemIcons(Path directory, String minecraft) { super(directory, minecraft, "minecraft-inventory", 64, ":clientmod:exportItemIcons"); }
}
