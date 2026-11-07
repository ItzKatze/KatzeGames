package gg.itzkatze.katze.bingo.match;

import java.util.List;
import java.util.UUID;

public record BingoCompetitor(String id, String name, Integer teamId, List<Member> members) {
    public BingoCompetitor { members = List.copyOf(members); }
    public record Member(UUID id, String name) {}
    public boolean contains(UUID player) { return members.stream().anyMatch(member -> member.id().equals(player)); }
}
