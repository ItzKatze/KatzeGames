package gg.itzkatze.katze.game.seed;

public record SeedSearchArea(double x, double z, int radius) {
    public SeedSearchArea {
        if (radius < 1 || !Double.isFinite(x) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Invalid seed search area");
        }
    }

    public boolean contains(double targetX, double targetZ) {
        double dx = targetX - x;
        double dz = targetZ - z;
        return dx * dx + dz * dz <= (double) radius * radius;
    }
}
