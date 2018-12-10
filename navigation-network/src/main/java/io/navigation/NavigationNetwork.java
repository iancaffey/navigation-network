package io.navigation;

import com.google.common.collect.ImmutableSet;
import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import lombok.NonNull;
import org.immutables.value.Value.Auxiliary;
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
public interface NavigationNetwork<C> extends NetworkView<C> {
    static <C> Builder<C> builder() {
        return ImmutableNavigationNetwork.builder();
    }

    @Override
    @Auxiliary
    NetworkInfo getNetworkInfo();

    @Override
    NetworkCoverage<C> getNetworkCoverage();

    @Override
    NetworkGraph getNetworkGraph();

    @Auxiliary
    NetworkFinderFactory<C> getNetworkFinderFactory();

    @Auxiliary
    RouteFinderFactory getRouteFinderFactory();

    @Derived
    default NetworkFinder<C> getNetworkFinder() {
        return getNetworkFinderFactory().create(this);
    }

    @Derived
    default RouteFinder getRouteFinder() {
        return getRouteFinderFactory().create(getNetworkGraph());
    }

    default Optional<Station> findPreferredStation(@NonNull C coordinate) {
        return getNetworkFinder().findPreferredStation(coordinate);
    }

    default Stream<Station> findAvailableStations(@NonNull C coordinate) {
        return getNetworkFinder().findAvailableStations(coordinate);
    }

    default Optional<Stop> findPreferredStop(@NonNull C coordinate) {
        return getNetworkFinder().findPreferredStop(coordinate);
    }

    default Stream<Stop> findAvailableStops(@NonNull C coordinate) {
        return getNetworkFinder().findAvailableStops(coordinate);
    }

    default Optional<Route> findRoute(@NonNull Station station, @NonNull Stop stop) {
        return getRouteFinder().findRoute(station, stop);
    }

    default Optional<Route> findPreferredRoute(@NonNull C start, @NonNull C destination) {
        Station station = findPreferredStation(start).orElseThrow(() -> new UnreachableStationException(start, this));
        Stop stop = findPreferredStop(destination).orElseThrow(() -> new UnreachableStopException(start, this));
        return findRoute(station, stop);
    }

    default Stream<Route> findAvailableRoutes(@NonNull C start, @NonNull C destination) {
        Set<Station> stations = findAvailableStations(start).collect(ImmutableSet.toImmutableSet());
        if (stations.isEmpty()) {
            throw new UnreachableStationException(start, this);
        }
        Set<Stop> stops = findAvailableStops(destination).collect(ImmutableSet.toImmutableSet());
        if (stops.isEmpty()) {
            throw new UnreachableStationException(destination, this);
        }
        return stations.parallelStream()
                //mappers are always sequentially evaluated, so no use in using stops.parallelStream() as it'd be misleading
                .flatMap(station -> stops.stream().map(stop -> findRoute(station, stop)))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    interface Builder<C> {
        Builder<C> setNetworkInfo(NetworkInfo networkInfo);

        Builder<C> setNetworkCoverage(NetworkCoverage<C> networkCoverage);

        Builder<C> setNetworkGraph(NetworkGraph networkGraph);

        Builder<C> setNetworkFinderFactory(NetworkFinderFactory<C> networkFinderFactory);

        Builder<C> setRouteFinderFactory(RouteFinderFactory routeFinderFactory);

        NavigationNetwork<C> build();
    }
}
