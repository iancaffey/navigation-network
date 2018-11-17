package io.navigation;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public interface NetworkFinder {
    Optional<Station> findNearestStation(Coordinate coordinate);

    Stream<Station> findReachableStations(Coordinate coordinate);

    Optional<Stop> findNearestStop(Coordinate coordinate);

    Stream<Stop> findReachableStops(Coordinate coordinate);
}
