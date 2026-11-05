package gg.itzkatze.katze.game.seed;

import java.util.List;

@FunctionalInterface
public interface SeedFeatureSearch {
    SeedSearchResult find(List<SeedFeature> alternatives);
}
