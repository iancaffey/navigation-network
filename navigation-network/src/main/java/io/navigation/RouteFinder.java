package io.navigation;

import java.util.Optional;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public interface RouteFinder {
    Optional<Route> findRoute(Station station, Stop stop);
}
