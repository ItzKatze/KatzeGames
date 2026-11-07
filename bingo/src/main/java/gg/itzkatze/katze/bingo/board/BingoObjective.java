package gg.itzkatze.katze.bingo.board;

public record BingoObjective(String id, Category category, Difficulty difficulty, String target, int count,
                             String icon, String texture, int effort, String dependency, String feature, String dimension) {
    public enum Category { ITEM, MOB, BIOME, STRUCTURE, ADVANCEMENT }
}
