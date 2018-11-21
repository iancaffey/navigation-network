package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import lombok.RequiredArgsConstructor;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Enclosing
@ImmutableNavigationNetworkStyle
public interface NetworkFinderFactory<C> {
    static <C> FindAny<C> findAny() {
        return ImmutableNetworkFinderFactory.FindAny.<C>builder().build();
    }

    static <C> FindFirst<C> findFirst() {
        return ImmutableNetworkFinderFactory.FindFirst.<C>builder().build();
    }

    NetworkFinder<C> create(NetworkView<C> networkView);

    @Immutable
    interface FindAny<C> extends NetworkFinderFactory<C> {
        @Override
        default NetworkFinder<C> create(NetworkView<C> networkView) {
            return new NetworkFinder<>(networkView);
        }

        class NetworkFinder<C> extends AbstractNetworkFinder<C> {
            public NetworkFinder(NetworkView<C> networkView) {
                super(networkView);
            }

            @Override
            public Optional<Station> findPreferredStation(C coordinate) {
                return findAvailableStations(coordinate).findAny();
            }

            @Override
            public Optional<Stop> findPreferredStop(C coordinate) {
                return findAvailableStops(coordinate).findAny();
            }

            @Override
            public String toString() {
                return "FindAny";
            }
        }
    }

    @Immutable
    interface FindFirst<C> extends NetworkFinderFactory<C> {
        @Override
        default NetworkFinder<C> create(NetworkView<C> networkView) {
            return new NetworkFinder<>(networkView);
        }

        class NetworkFinder<C> extends AbstractNetworkFinder<C> {
            public NetworkFinder(NetworkView<C> networkView) {
                super(networkView);
            }

            @Override
            public Optional<Station> findPreferredStation(C coordinate) {
                return findAvailableStations(coordinate).findFirst();
            }

            @Override
            public Optional<Stop> findPreferredStop(C coordinate) {
                return findAvailableStops(coordinate).findFirst();
            }

            @Override
            public String toString() {
                return "FindFirst";
            }
        }
    }

    @RequiredArgsConstructor
    abstract class AbstractNetworkFinder<C> implements NetworkFinder<C> {
        private final NetworkView<C> networkView;

        @Override
        public Stream<Station> findAvailableStations(C coordinate) {
            NetworkGraph networkGraph = networkView.getNetworkGraph();
            NetworkCoverage<C> networkCoverage = networkView.getNetworkCoverage();
            return networkGraph.getStations().stream().filter(station -> networkCoverage.withinRange(station, coordinate));
        }

        @Override
        public Stream<Stop> findAvailableStops(C coordinate) {
            NetworkGraph networkGraph = networkView.getNetworkGraph();
            NetworkCoverage<C> networkCoverage = networkView.getNetworkCoverage();
            return networkGraph.getStops().stream().filter(stop -> networkCoverage.withinRange(stop, coordinate));
        }
    }
}
