package gg.itzkatze.katze.mobarmy;

record ArenaLayout(int width, int height, int length, int floor, int roofLayer) {
    ArenaLayout {
        if (width < 3 || length < 3 || roofLayer < 2 || roofLayer >= height) throw new IllegalArgumentException("Invalid arena layout");
    }

    int roof() { return floor + roofLayer; }
    int maxHeight() { return Math.max(floor + height, roof() + 8); }

    boolean contains(double x, double y, double z, boolean waiting) {
        return x >= 0 && x < width && z >= 0 && z < length
                && y >= floor && y < (waiting ? roof() + 8 : roof());
    }

    boolean containsMob(double x, double y, double z) {
        return x >= -4 && x <= width + 4 && z >= -4 && z <= length + 4 && y >= floor - 4 && y <= roof() + 4;
    }
}
