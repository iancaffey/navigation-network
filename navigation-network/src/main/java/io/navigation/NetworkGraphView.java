package io.navigation;

import java.util.Set;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public interface NetworkGraphView {
    NetworkInfo getNetworkInfo();

    Set<Station> getStations();

    Set<Stop> getStops();
}
