package io.navigation;

import com.google.common.collect.ImmutableSet;
import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import lombok.NonNull;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@ImmutableNavigationNetworkStyle
public interface NavigationNetwork {
    static Builder builder() {
        return ImmutableNavigationNetwork.builder();
    }

    static NavigationNetwork of(NetworkInfo networkInfo,
                                NetworkFinderFactory networkFinderFactory, RouteFinderFactory routeFinderFactory,
                                Set<Station> stations, Set<Stop> stops) {
        return ImmutableNavigationNetwork.of(networkInfo, networkFinderFactory, routeFinderFactory, stations, stops);
    }

    static NavigationNetwork of(NetworkInfo networkInfo,
                                NetworkFinderFactory networkFinderFactory, RouteFinderFactory routeFinderFactory,
                                Iterable<? extends Station> stations, Iterable<? extends Stop> stops) {
        return ImmutableNavigationNetwork.of(networkInfo, networkFinderFactory, routeFinderFactory, stations, stops);
    }

    NetworkInfo getNetworkInfo();

    NetworkFinderFactory getNetworkFinderFactory();

    RouteFinderFactory getRouteFinderFactory();

    Set<Station> getStations();

    Set<Stop> getStops();

    @Derived
    default NetworkFinder getNetworkFinder() {
        return getNetworkFinderFactory().create(getNetworkInfo(), getStations(), getStops());
    }

    @Derived
    default RouteFinder getRouteFinder() {
        return getRouteFinderFactory().create(getNetworkInfo(), getStations(), getStops());
    }

    default Optional<Station> findNearestStation(@NonNull Coordinate coordinate) {
        return getNetworkFinder().findNearestStation(coordinate);
    }

    default Stream<Station> findReachableStations(@NonNull Coordinate coordinate) {
        return getNetworkFinder().findReachableStations(coordinate);
    }

    default Optional<Stop> findNearestStop(@NonNull Coordinate coordinate) {
        return getNetworkFinder().findNearestStop(coordinate);
    }

    default Stream<Stop> findReachableStops(@NonNull Coordinate coordinate) {
        return getNetworkFinder().findReachableStops(coordinate);
    }

    default Optional<Route> findRoute(@NonNull Station station, @NonNull Stop stop) {
        return getRouteFinder().findRoute(station, stop);
    }

    default Optional<Route> findNearestRoute(@NonNull Coordinate start, @NonNull Coordinate destination) {
        Station station = findNearestStation(start).orElseThrow(() -> new UnreachableStationException(start, this));
        Stop stop = findNearestStop(destination).orElseThrow(() -> new UnreachableStopException(start, this));
        return findRoute(station, stop);
    }

    default Stream<Route> findRoutes(@NonNull Coordinate start, @NonNull Coordinate destination) {
        Set<Station> stations = findReachableStations(start).collect(ImmutableSet.toImmutableSet());
        if (stations.isEmpty()) {
            throw new UnreachableStationException(start, this);
        }
        Set<Stop> stops = findReachableStops(destination).collect(ImmutableSet.toImmutableSet());
        if (stops.isEmpty()) {
            throw new UnreachableStationException(destination, this);
        }
        return stations.parallelStream()
                .flatMap(station -> stops.stream().map(stop -> findRoute(station, stop)))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    interface Builder {
        Builder setNetworkInfo(NetworkInfo networkInfo);

        Builder setNetworkFinderFactory(NetworkFinderFactory networkFinderFactory);

        Builder setRouteFinderFactory(RouteFinderFactory routeFinderFactory);

        Builder addStation(Station station);

        Builder addStations(Station... stations);

        Builder addAllStations(Iterable<? extends Station> stations);

        Builder setStations(Iterable<? extends Station> stations);

        Builder addStop(Stop stop);

        Builder addStops(Stop... stops);

        Builder addAllStops(Iterable<? extends Stop> stops);

        Builder setStops(Iterable<? extends Stop> stops);

        NavigationNetwork build();
    }
}