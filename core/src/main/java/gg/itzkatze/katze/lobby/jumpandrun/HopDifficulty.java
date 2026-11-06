package gg.itzkatze.katze.lobby.jumpandrun;

enum HopDifficulty {
    NORMAL,
    HARD,
    EXTREME,
    SLIME_EASY,
    SLIME_HARD;

    boolean isSlime() {
        return this == SLIME_EASY || this == SLIME_HARD;
    }
}
