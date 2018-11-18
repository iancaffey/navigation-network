package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import lombok.RequiredArgsConstructor;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;

import java.util.Optional;
import java.util.Set;
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

    NetworkFinder<C> create(NetworkInfo networkInfo, NetworkCoverage<C> networkCoverage, Set<Station> stations, Set<Stop> stops);

    @Immutable
    interface FindAny<C> extends NetworkFinderFactory<C> {
        @Override
        default NetworkFinder<C> create(NetworkInfo networkInfo, NetworkCoverage<C> networkCoverage, Set<Station> stations, Set<Stop> stops) {
            return new NetworkFinder<>(networkCoverage, stations, stops);
        }

        class NetworkFinder<C> extends AbstractNetworkFinder<C> {
            public NetworkFinder(NetworkCoverage<C> networkCoverage, Set<Station> stations, Set<Stop> stops) {
                super(networkCoverage, stations, stops);
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
        default NetworkFinder<C> create(NetworkInfo networkInfo, NetworkCoverage<C> networkCoverage, Set<Station> stations, Set<Stop> stops) {
            return new NetworkFinder<>(networkCoverage, stations, stops);
        }

        class NetworkFinder<C> extends AbstractNetworkFinder<C> {
            public NetworkFinder(NetworkCoverage<C> networkCoverage, Set<Station> stations, Set<Stop> stops) {
                super(networkCoverage, stations, stops);
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
        private final NetworkCoverage<C> networkCoverage;
        private final Set<Station> stations;
        private final Set<Stop> stops;

        @Override
        public Stream<Station> findAvailableStations(C coordinate) {
            return stations.stream().filter(station -> networkCoverage.contains(station, coordinate));
        }

        @Override
        public Stream<Stop> findAvailableStops(C coordinate) {
            return stops.stream().filter(stop -> networkCoverage.contains(stop, coordinate));
        }
    }
}
