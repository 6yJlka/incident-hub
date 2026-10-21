package ru.donskikh.incidenthub.experiment.replay;

import ru.donskikh.incidenthub.alert.correlation.*;
import ru.donskikh.incidenthub.experiment.ScenarioObservation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public final class ScenarioReplay {
    public ReplayResult replay(ScenarioObservation observation, AlertCorrelator correlator) {
        Objects.requireNonNull(observation, "observation must not be null");
        Objects.requireNonNull(correlator, "correlator must not be null");
        var graph = new DependencyGraphSnapshot(observation.observedGraph().services().stream().map(service -> service.id()).toList(),
                observation.observedGraph().edges().stream().map(edge -> new DependencyArc(edge.dependentId(), edge.dependencyId(), edge.type())).toList());
        var groups = new TreeMap<Long, ReplayGroup>();
        long alertId = 0;
        long groupId = 0;
        for (var incoming : observation.alerts()) {
            var alert = new CorrelationAlert(++alertId, incoming.serviceId(), incoming.occurredAt(), incoming.signalType(), incoming.severity());
            var decision = Objects.requireNonNull(correlator.correlate(alert,
                    new CorrelationContext(groups.values().stream().map(ReplayGroup::snapshot).toList(), graph)), "Correlation decision must not be null");
            var members = new ArrayList<ReplayMember>();
            var first = alert.occurredAt();
            var last = first;
            long selected;
            if (decision instanceof CorrelationDecision.JoinGroup join) {
                selected = join.groupId();
                var group = groups.get(selected);
                if (group == null) throw new IllegalStateException("Correlator selected a group outside the context");
                members.addAll(group.members());
                if (group.snapshot().firstAlertAt().isBefore(first)) first = group.snapshot().firstAlertAt();
                if (group.snapshot().lastAlertAt().isAfter(last)) last = group.snapshot().lastAlertAt();
            } else selected = ++groupId;
            members.add(new ReplayMember(incoming.externalKey(), alert,
                    decision instanceof CorrelationDecision.JoinGroup join ? join.linkStrength() : null, decision.linkEvidence()));
            var snapshot = new OpenAlertGroupSnapshot(selected, decision.rootAssessment(), first, last, members.stream().map(member ->
                    new CorrelationGroupMember(member.alert().id(), member.alert().businessServiceId(), member.alert().occurredAt(),
                            member.alert().signalType(), member.alert().severity())).toList());
            groups.put(selected, new ReplayGroup(selected, snapshot, members));
        }
        return new ReplayResult(List.copyOf(groups.values()));
    }
}
