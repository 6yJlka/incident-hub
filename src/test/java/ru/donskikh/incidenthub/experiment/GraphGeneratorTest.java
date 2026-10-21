package ru.donskikh.incidenthub.experiment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.experiment.graph.DependencyEdge;
import ru.donskikh.incidenthub.experiment.graph.GraphGenerator;
import ru.donskikh.incidenthub.experiment.graph.GraphSpec;
import ru.donskikh.incidenthub.experiment.graph.ServiceGraph;
import ru.donskikh.incidenthub.experiment.graph.ServiceNode;
import ru.donskikh.incidenthub.experiment.io.DemoGraphLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.donskikh.incidenthub.experiment.ExperimentFixtures.*;

class GraphGeneratorTest {
    static Stream<Shape> shapes() {
        return Stream.of(new Shape(new GraphSpec.Tree(2, 3), 13, 12, false),
                new Shape(new GraphSpec.Layered(3, 4, .5), 12, 16, false),
                new Shape(new GraphSpec.Diamonds(2, 3), 11, 12, false),
                new Shape(new GraphSpec.Cyclic(new GraphSpec.Layered(3, 4, .5), .25), 12, 20, true));
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void generatesReproducibleConnectedGraphsWithExpectedSizeAndCycles(Shape shape) {
        var generator = new GraphGenerator();
        for (long seed = 0; seed < 10; seed++) {
            var command = graphCommand(shape.spec(), seed);
            var graph = generator.generate(command);
            assertThat(graph).isEqualTo(generator.generate(command));
            assertThat(graph.services()).hasSize(shape.nodes());
            assertThat(graph.edges()).hasSize(shape.edges());
            assertThat(reachable(graph, 1, false, true)).hasSize(shape.nodes());
            assertThat(acyclic(graph)).isEqualTo(!shape.cycles());
        }
        assertThat(generator.generate(graphCommand(shape.spec(), 42)))
                .isNotEqualTo(generator.generate(graphCommand(shape.spec(), 43)));
    }

    @Test
    void layeredEdgesJoinAdjacentLayersAtRequestedDensity() {
        var graph = new GraphGenerator().generate(graphCommand(new GraphSpec.Layered(4, 3, .6), 42));
        assertThat(graph.edges()).hasSize((int) Math.round(.6 * 3 * 9));
        var depths = new java.util.TreeMap<Long, Integer>();
        for (int layer = 0; layer < 4; layer++) {
            final int depth = layer;
            var ready = graph.services().stream().filter(service -> !depths.containsKey(service.id()))
                    .filter(service -> graph.edges().stream().filter(edge -> edge.dependentId() == service.id())
                            .allMatch(edge -> depths.containsKey(edge.dependencyId()))).toList();
            assertThat(ready).hasSize(3);
            ready.forEach(service -> depths.put(service.id(), depth));
        }
        assertThat(graph.edges()).allSatisfy(edge ->
                assertThat(depths.get(edge.dependentId())).isEqualTo(depths.get(edge.dependencyId()) + 1));
    }

    @Test
    void diamondsHaveTwoBranchesAndSharedBoundaryVertices() {
        var graph = new GraphGenerator().generate(graphCommand(new GraphSpec.Diamonds(3, 2), 4));
        assertThat(graph.services()).hasSize(10);
        assertThat(graph.edges()).hasSize(12);
        assertThat(graph.services().stream().filter(service -> graph.edges().stream()
                .filter(edge -> edge.dependentId() == service.id()).count() == 2)).hasSize(3);
        assertThat(acyclic(graph)).isTrue();
    }

    @Test
    void handlesSingletonsAndMinimumPositiveCycleRate() {
        for (GraphSpec spec : List.of(new GraphSpec.Tree(0, 2), new GraphSpec.Layered(1, 1, 0))) {
            var graph = new GraphGenerator().generate(graphCommand(spec, 42));
            assertThat(graph.services()).hasSize(1);
            assertThat(graph.edges()).isEmpty();
        }
        var cyclic = new GraphGenerator().generate(graphCommand(new GraphSpec.Cyclic(new GraphSpec.Layered(2, 1, 1), .001), 42));
        assertThat(cyclic.edges()).hasSize(2);
        assertThat(acyclic(cyclic)).isFalse();
    }

    @Test
    void demoResourceIsCanonicalAndContainsTheExpectedCatalogue() {
        var graph = new DemoGraphLoader().load();
        assertThat(graph.services()).hasSize(10);
        assertThat(graph.edges()).hasSize(10);
        assertThat(new GraphGenerator(graph).generate(graphCommand(new GraphSpec.Demo(), 42))).isEqualTo(graph);
        assertThat(reachable(graph, 1, false, true)).hasSize(10);
    }

    @Test
    void graphRejectsInvalidStructureAndProtectsCollections() {
        var nodes = new ArrayList<>(graph(2).services());
        var edges = new ArrayList<>(List.of(edge(2, 1)));
        var graph = new ServiceGraph(nodes, edges);
        nodes.clear(); edges.clear();
        assertThat(graph.services()).hasSize(2);
        assertThat(graph.edges()).hasSize(1);
        assertThatThrownBy(() -> graph.edges().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> graph.services().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new ServiceGraph(List.of(), List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ServiceGraph(graph.services(), List.of(edge(2, 1),
                new DependencyEdge(2, 1, DependencyType.DATA)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ServiceGraph(graph.services(), List.of(edge(3, 1)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DependencyEdge(1, 1, DependencyType.SYNC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ServiceGraph(List.of(graph.services().getFirst(), graph.services().getFirst()), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ServiceGraph(List.of(new ServiceNode(1, "SAME", "One", ServiceTier.TIER_1),
                new ServiceNode(2, "SAME", "Two", ServiceTier.TIER_2)), List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsImpossibleTopologyParameters() {
        assertThatThrownBy(() -> new GraphSpec.Tree(-1, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GraphSpec.Layered(2, 4, .1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GraphSpec.Layered(1, 2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GraphSpec.Diamonds(1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GraphSpec.Cyclic(new GraphSpec.Layered(2, 2, 1), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GraphGenerator().generate(null)).isInstanceOf(NullPointerException.class);
    }

    private record Shape(GraphSpec spec, int nodes, int edges, boolean cycles) {}
}
