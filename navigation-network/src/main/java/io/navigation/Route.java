package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.gson.Gson;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import java.util.List;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@Gson.TypeAdapters
@ImmutableNavigationNetworkStyle
public interface Route {
    static Builder builder() {
        return ImmutableRoute.builder();
    }

    static Route of(RouteInfo routeInfo, Station station, List<Station> connections, Stop stop) {
        return ImmutableRoute.of(routeInfo, station, connections, stop);
    }

    static Route of(RouteInfo routeInfo, Station station, Iterable<? extends Station> connections, Stop stop) {
        return ImmutableRoute.of(routeInfo, station, connections, stop);
    }

    @Auxiliary
    RouteInfo getRouteInfo();

    Station getStation();

    List<Station> getConnections();

    Stop getStop();

    interface Builder {
        Builder setRouteInfo(RouteInfo routeInfo);

        Builder setStation(Station station);

        Builder addConnection(Station station);

        Builder addConnections(Station... stations);

        Builder addAllConnections(Iterable<? extends Station> stations);

        Builder setConnections(Iterable<? extends Station> stations);

        Builder setStop(Stop stop);

        Route build();
    }
}
