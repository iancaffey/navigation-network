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

    static Route of(RouteInfo routeInfo, String station, List<String> connections, String stop) {
        return ImmutableRoute.of(routeInfo, station, connections, stop);
    }

    static Route of(RouteInfo routeInfo, String station, Iterable<String> connections, String stop) {
        return ImmutableRoute.of(routeInfo, station, connections, stop);
    }

    @Auxiliary
    RouteInfo getRouteInfo();

    String getStation();

    List<String> getConnections();

    String getStop();

    interface Builder {
        Builder setRouteInfo(RouteInfo routeInfo);

        Builder setStation(String station);

        Builder addConnection(String station);

        Builder addConnections(String... stations);

        Builder addAllConnections(Iterable<String> stations);

        Builder setConnections(Iterable<String> stations);

        Builder setStop(String stop);

        Route build();
    }
}
