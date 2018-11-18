package io.navigation;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public interface NetworkFinder<C> {
    Optional<Station> findPreferredStation(C coordinate);

    Stream<Station> findAvailableStations(C coordinate);

    Optional<Stop> findPreferredStop(C coordinate);

    Stream<Stop> findAvailableStops(C coordinate);
}
