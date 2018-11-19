package io.navigation;

import java.util.Set;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public interface NetworkView<C> {
    NetworkInfo getNetworkInfo();

    NetworkCoverage<C> getNetworkCoverage();

    Set<Station> getStations();

    Set<Stop> getStops();
}
