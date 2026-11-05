package gg.itzkatze.katze.results;

import gg.itzkatze.katze.player.KatzePlayer;

import java.util.List;
import java.util.UUID;

public record ResultRoster(UUID id, long startedAt, List<Member> members) {
    public ResultRoster {
        members = List.copyOf(members);
    }

    public record Member(UUID id, String name) {}

    public static ResultRoster capture() {
        return new ResultRoster(UUID.randomUUID(), System.currentTimeMillis(), KatzePlayer.getOnlinePlayers().stream()
                .filter(KatzePlayer::isParticipating)
                .map(player -> new Member(player.getUuid(), player.getName())).toList());
    }
}
