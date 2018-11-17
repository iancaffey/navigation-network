package io.navigation;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public interface NetworkFinder {
    Optional<Station> findPreferredStation(Coordinate coordinate);

    Stream<Station> findAvailableStations(Coordinate coordinate);

    Optional<Stop> findPreferredStop(Coordinate coordinate);

    Stream<Stop> findAvailableStops(Coordinate coordinate);
}
