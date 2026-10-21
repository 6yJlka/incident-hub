package ru.donskikh.incidenthub.experiment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.experiment.graph.DependencyEdge;
import ru.donskikh.incidenthub.experiment.graph.DistortGraphCommand;
import ru.donskikh.incidenthub.experiment.graph.GraphDistorter;
import ru.donskikh.incidenthub.experiment.graph.GraphDistortion;
import ru.donskikh.incidenthub.experiment.io.DemoGraphLoader;

import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.donskikh.incidenthub.experiment.ExperimentFixtures.*;

class GraphDistorterTest {
    @ParameterizedTest
    @ValueSource(doubles = {0, .15, .3, .5, 1})
    void removesAndAddsRoundedFractionsOfTrueEdgesWithoutReaddingRemovedOnes(double rate) {
        var graph = new DemoGraphLoader().load();
        var command = new DistortGraphCommand(graph, new GraphDistortion(rate, rate, 42), DependencyType.SYNC);
        var observed = new GraphDistorter().distort(command);
        var trueEdges = new TreeSet<>(DependencyEdge.ORDER);
        trueEdges.addAll(graph.edges());
        var observedEdges = new TreeSet<>(DependencyEdge.ORDER);
        observedEdges.addAll(observed.edges());
        assertThat(observed.services()).isEqualTo(graph.services());
        assertThat(trueEdges.stream().filter(edge -> !observedEdges.contains(edge)).count()).isEqualTo(Math.round(rate * 10));
        assertThat(observedEdges.stream().filter(edge -> !trueEdges.contains(edge)).count()).isEqualTo(Math.round(rate * 10));
        assertThat(observed).isEqualTo(new GraphDistorter().distort(command));
    }

    @Test
    void changesSelectedEdgesWithSeedAndRejectsInsufficientComplement() {
        var graph = new DemoGraphLoader().load();
        assertThat(new GraphDistorter().distort(new DistortGraphCommand(graph, new GraphDistortion(.5, .5, 1), DependencyType.SYNC)))
                .isNotEqualTo(new GraphDistorter().distort(new DistortGraphCommand(graph, new GraphDistortion(.5, .5, 2), DependencyType.SYNC)));
        assertThatThrownBy(() -> new GraphDistorter().distort(new DistortGraphCommand(graph(2, edge(1, 2), edge(2, 1)),
                new GraphDistortion(1, 1, 42), DependencyType.DATA))).isInstanceOf(IllegalArgumentException.class);
        assertThat(new GraphDistorter().distort(new DistortGraphCommand(graph(1), new GraphDistortion(1, 1, 42), DependencyType.SYNC)))
                .isEqualTo(graph(1));
        assertThatThrownBy(() -> new GraphDistortion(Double.NaN, 0, 42)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GraphDistortion(0, 1.1, 42)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GraphDistorter().distort(null)).isInstanceOf(NullPointerException.class);
    }
}
