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
public interface NetworkFinderFactory {
    static FindFirst findFirst() {
        return ImmutableNetworkFinderFactory.FindFirst.of();
    }

    NetworkFinder create(NetworkInfo networkInfo, Set<Station> stations, Set<Stop> stops);

    @Immutable
    interface FindFirst extends NetworkFinderFactory {
        @Override
        default NetworkFinder create(NetworkInfo networkInfo, Set<Station> stations, Set<Stop> stops) {
            return new NetworkFinder(stations, stops);
        }

        @RequiredArgsConstructor
        class NetworkFinder implements io.navigation.NetworkFinder {
            private final Set<Station> stations;
            private final Set<Stop> stops;

            @Override
            public Optional<Station> findNearestStation(Coordinate coordinate) {
                return findReachableStations(coordinate).findFirst();
            }

            @Override
            public Stream<Station> findReachableStations(Coordinate coordinate) {
                return stations.stream().filter(station -> station.getServiceArea().canService(coordinate));
            }

            @Override
            public Optional<Stop> findNearestStop(Coordinate coordinate) {
                return findReachableStops(coordinate).findFirst();
            }

            @Override
            public Stream<Stop> findReachableStops(Coordinate coordinate) {
                return stops.stream().filter(station -> station.getServiceArea().canService(coordinate));
            }
        }
    }
}