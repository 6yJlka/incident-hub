package ru.donskikh.incidenthub.experiment.replay;

import ru.donskikh.incidenthub.alert.correlation.*;
import java.util.List;
import java.util.Objects;

public record ReplayGroup(long id, OpenAlertGroupSnapshot snapshot, List<ReplayMember> members) {
    public ReplayGroup {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        members = List.copyOf(Objects.requireNonNull(members, "members must not be null"));
        if (id != snapshot.id() || members.isEmpty() || !snapshot.members().equals(members.stream().map(member ->
                new CorrelationGroupMember(member.alert().id(), member.alert().businessServiceId(), member.alert().occurredAt(),
                        member.alert().signalType(), member.alert().severity())).toList())) {
            throw new IllegalArgumentException("Replay group and snapshot must have identical members and id");
        }
    }
}
