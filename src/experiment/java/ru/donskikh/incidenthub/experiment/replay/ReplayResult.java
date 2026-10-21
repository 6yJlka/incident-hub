package ru.donskikh.incidenthub.experiment.replay;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public record ReplayResult(List<ReplayGroup> groups) {
    public ReplayResult {
        groups = Objects.requireNonNull(groups, "groups must not be null").stream().sorted(Comparator.comparingLong(ReplayGroup::id)).toList();
        var ids = new TreeSet<Long>();
        var alerts = new TreeSet<Long>();
        var keys = new TreeSet<String>();
        for (var group : groups) {
            if (!ids.add(group.id())) throw new IllegalArgumentException("Duplicate group id");
            group.members().forEach(member -> {
                if (!alerts.add(member.alert().id()) || !keys.add(member.externalKey())) throw new IllegalArgumentException("Duplicate replay alert");
            });
        }
    }
}
