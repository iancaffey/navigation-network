package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.gson.Gson;
import org.immutables.value.Value.Immutable;

import java.util.Set;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@Gson.TypeAdapters
@ImmutableNavigationNetworkStyle
public interface NetworkGraph {
    static Builder builder() {
        return ImmutableNetworkGraph.builder();
    }

    static NetworkGraph of(Iterable<? extends Station> stations, Iterable<? extends Stop> stops) {
        return ImmutableNetworkGraph.of(stations, stops);
    }

    static NetworkGraph of(Set<Station> stations, Set<Stop> stops) {
        return ImmutableNetworkGraph.of(stations, stops);
    }

    Set<Station> getStations();

    Set<Stop> getStops();

    interface Builder {
        Builder addStation(Station station);

        Builder addStations(Station... stations);

        Builder addAllStations(Iterable<? extends Station> stations);

        Builder setStations(Iterable<? extends Station> stations);

        Builder addStop(Stop stop);

        Builder addStops(Stop... stops);

        Builder addAllStops(Iterable<? extends Stop> stops);

        Builder setStops(Iterable<? extends Stop> stops);

        NetworkGraph build();
    }
}
