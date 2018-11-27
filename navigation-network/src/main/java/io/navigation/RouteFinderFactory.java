package io.navigation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Enclosing
@ImmutableNavigationNetworkStyle
public interface RouteFinderFactory {
    static Cached cached(RouteFinderFactory factory, long timeToLive, TimeUnit timeUnit) {
        return ImmutableRouteFinderFactory.Cached.of(factory, timeToLive, timeUnit);
    }

    static Direct direct() {
        return ImmutableRouteFinderFactory.Direct.of();
    }

    static Dijkstra dijkstra() {
        return ImmutableRouteFinderFactory.Dijkstra.of();
    }

    static MinimumFare minimumFare(RouteFinderFactory... routeFinderFactories) {
        return ImmutableRouteFinderFactory.MinimumFare.of(ImmutableSet.copyOf(routeFinderFactories));
    }

    static MinimumFare minimumFare(Set<RouteFinderFactory> routeFinderFactories) {
        return ImmutableRouteFinderFactory.MinimumFare.of(routeFinderFactories);
    }

    static MinimumFare minimumFare(Iterable<? extends RouteFinderFactory> routeFinderFactories) {
        return ImmutableRouteFinderFactory.MinimumFare.of(routeFinderFactories);
    }

    static QuickSelect quickSelect(RouteFinderFactory... routeFinderFactories) {
        return ImmutableRouteFinderFactory.QuickSelect.of(ImmutableSet.copyOf(routeFinderFactories));
    }

    static QuickSelect quickSelect(Set<RouteFinderFactory> routeFinderFactories) {
        return ImmutableRouteFinderFactory.QuickSelect.of(routeFinderFactories);
    }

    static QuickSelect quickSelect(Iterable<? extends RouteFinderFactory> routeFinderFactories) {
        return ImmutableRouteFinderFactory.QuickSelect.of(routeFinderFactories);
    }

    static FirstOption firstOption(RouteFinderFactory... routeFinderFactories) {
        return ImmutableRouteFinderFactory.FirstOption.of(ImmutableList.copyOf(routeFinderFactories));
    }

    static FirstOption firstOption(List<RouteFinderFactory> routeFinderFactories) {
        return ImmutableRouteFinderFactory.FirstOption.of(routeFinderFactories);
    }

    static FirstOption firstOption(Iterable<? extends RouteFinderFactory> routeFinderFactories) {
        return ImmutableRouteFinderFactory.FirstOption.of(routeFinderFactories);
    }

    RouteFinder create(NetworkGraph networkGraph);

    @Immutable
    interface Cached extends RouteFinderFactory {
        RouteFinderFactory getRouteFinderFactory();

        long getTimeToLive();

        TimeUnit getTimeUnit();

        @Override
        default RouteFinder create(NetworkGraph networkGraph) {
            io.navigation.RouteFinder delegate = getRouteFinderFactory().create(networkGraph);
            return new RouteFinder(delegate, getTimeUnit().toMillis(getTimeToLive()));
        }

        @RequiredArgsConstructor
        class RouteFinder implements io.navigation.RouteFinder {
            private final Map<CacheKey, Route> routes = new ConcurrentHashMap<>();
            private final io.navigation.RouteFinder delegate;
            private final long timeToLive;

            @Override
            public Optional<Route> findRoute(Station station, Stop stop) {
                long time = System.currentTimeMillis();
                CacheKey key = CacheKey.of(station.getId(), stop.getId());
                Route cachedRoute = routes.get(key);
                if (cachedRoute != null && (time - cachedRoute.getRouteInfo().getCreationTime().toEpochMilli()) <= timeToLive) {
                    return Optional.of(cachedRoute);
                }
                Optional<Route> route = delegate.findRoute(station, stop);
                route.ifPresent(r -> routes.put(key, r));
                return route;
            }

            @Override
            public String toString() {
                return "Cached{delegate=" + delegate + ", ttl=" + timeToLive + "ms}";
            }

            @Immutable
            interface CacheKey {
                static CacheKey of(String station, String stop) {
                    return ImmutableRouteFinderFactory.CacheKey.of(station, stop);
                }

                String getStation();

                String getStop();
            }
        }
    }

    @Immutable
    interface Direct extends RouteFinderFactory {
        @Override
        default RouteFinder create(NetworkGraph networkGraph) {
            Map<String, Stop> stopsById = networkGraph.getStops().stream().collect(ImmutableMap.toImmutableMap(Stop::getId, Function.identity()));
            Map<Station, Map<Stop, Set<RouteOption>>> directRouteOptions = networkGraph.getStations().stream().collect(ImmutableMap.toImmutableMap(
                    Function.identity(),
                    station -> station.getDestinations().stream().collect(ImmutableMap.toImmutableMap(
                            option -> stopsById.get(option.getDestination()),
                            ImmutableSet::of,
                            (left, right) -> Stream.of(left, right)
                                    .flatMap(Set::stream)
                                    .collect(ImmutableSet.toImmutableSet())
                    ))
            ));
            return new RouteFinder(directRouteOptions);
        }

        @RequiredArgsConstructor
        class RouteFinder implements io.navigation.RouteFinder {
            private final Map<Station, Map<Stop, Set<RouteOption>>> directRouteOptions;

            @Override
            public Optional<Route> findRoute(@NonNull Station station, @NonNull Stop stop) {
                if (directRouteOptions.isEmpty()) {
                    return Optional.empty();
                }
                Map<Stop, Set<RouteOption>> directRoutesFromStation = directRouteOptions.get(station);
                if (directRoutesFromStation == null) {
                    throw new IllegalArgumentException("Unable to find direct routes from " + station.getId() + ".");
                }
                Set<RouteOption> directRouteOptions = directRoutesFromStation.get(stop);
                if (directRouteOptions == null || directRouteOptions.isEmpty()) {
                    return Optional.empty();
                }
                RouteOption shortestRouteOption = directRouteOptions.stream()
                        .min(Comparator.comparingDouble(RouteOption::getFare))
                        .orElseThrow(() -> new IllegalArgumentException("Unable to find valid route option out of direct routes to " + stop.getId() + "."));
                return Optional.of(Route.builder()
                        .setRouteInfo(RouteInfo.of(Instant.now(), shortestRouteOption.getFare()))
                        .setStation(station.getId())
                        .setStop(stop.getId())
                        .build());
            }

            @Override
            public String toString() {
                return "Direct";
            }
        }
    }

    @Immutable
    interface Dijkstra extends RouteFinderFactory {
        @Override
        default RouteFinder create(NetworkGraph networkGraph) {
            Map<String, Station> stationsById = networkGraph.getStations().stream().collect(ImmutableMap.toImmutableMap(Station::getId, Function.identity()));
            Map<String, Map<String, Double>> minimumCostForDirectRoutes = new HashMap<>();
            networkGraph.getStations().forEach(station ->
                    station.getDestinations().forEach(routeOption -> {
                        double fare = routeOption.getFare();
                        Map<String, Double> minimumCosts = minimumCostForDirectRoutes.computeIfAbsent(routeOption.getDestination(), stop -> new HashMap<>());
                        Double existingMinimumCost = minimumCosts.get(station.getId());
                        if (existingMinimumCost == null || existingMinimumCost > fare) {
                            minimumCosts.put(station.getId(), fare);
                        }
                    })
            );
            return new RouteFinder(stationsById, minimumCostForDirectRoutes);
        }

        @RequiredArgsConstructor
        class RouteFinder implements io.navigation.RouteFinder {
            private final Map<String, Station> stationsById;
            private final Map<String, Map<String, Double>> minimumCostForDirectRoutes;

            @Override
            public Optional<Route> findRoute(@NonNull Station station, @NonNull Stop stop) {
                if (stationsById.isEmpty()) {
                    return Optional.empty();
                }
                Map<String, Double> directRouteCosts = minimumCostForDirectRoutes.get(stop.getId());
                if (directRouteCosts == null) {
                    throw new IllegalStateException("Unable to find direct route costs for " + stop.getId() + " when finding route.");
                }
                //No direct route costs present, indicating the stop is not serviced by any station
                if (directRouteCosts.isEmpty()) {
                    return Optional.empty();
                }
                //Calculate all the shortest paths to other stations from the specified station
                Set<String> visited = new HashSet<>();
                Map<String, String> parents = new HashMap<>();
                Map<String, Double> fares = new HashMap<>();
                fares.put(station.getId(), 0.0);
                Deque<Station> queue = new LinkedList<>();
                queue.push(station);
                while (!queue.isEmpty()) {
                    Station current = queue.removeFirst();
                    Double currentFare = fares.get(current.getId());
                    if (currentFare == null) {
                        throw new IllegalStateException("Unable to find fare for " + current.getId() + " when finding a route.");
                    }
                    current.getConnections().forEach(connection -> {
                        Station connectingStation = stationsById.get(connection.getDestination());
                        if (connectingStation == null) {
                            throw new IllegalStateException("Found connection for " + station.getId() + " that leads outside the network.");
                        }
                        double newFareToConnection = currentFare + connection.getFare();
                        Double previousFareToConnection = fares.get(connectingStation.getId());
                        if (previousFareToConnection == null || previousFareToConnection > newFareToConnection) {
                            fares.put(connectingStation.getId(), newFareToConnection);
                            parents.put(connectingStation.getId(), current.getId());
                        }
                        //only add the destination to be processed if it hasn't been seen yet
                        if (visited.contains(connection.getDestination())) {
                            return;
                        }
                        queue.push(connectingStation);
                    });
                    visited.add(current.getId());
                }
                //Calculate the true minimum route which takes into account the cost of the last leg (station -> stop)
                AtomicReference<String> minimumLastLeg = new AtomicReference<>();
                AtomicReference<Double> minimumFare = new AtomicReference<>(Double.MAX_VALUE);
                directRouteCosts.forEach((parent, cost) -> {
                    Double fareToParent = fares.get(parent);
                    if (fareToParent == null) {
                        throw new IllegalArgumentException("Unable to find the minimum fare to " + parent + ".");
                    }
                    Double existingMinimum = minimumFare.get();
                    double costToStop = fareToParent + cost;
                    if (existingMinimum == null || existingMinimum > costToStop) {
                        minimumLastLeg.set(parent);
                        minimumFare.set(costToStop);
                    }
                });
                //Add the connections in reverse order (we trace the shortest path back by each leg from the last station)
                List<String> inverseConnections = new ArrayList<>();
                String currentLeg = minimumLastLeg.get();
                while (currentLeg != null && !currentLeg.equals(station.getId())) {
                    inverseConnections.add(currentLeg);
                    currentLeg = parents.get(currentLeg);
                }
                double routeCost = minimumFare.get();
                Route.Builder builder = Route.builder()
                        .setRouteInfo(RouteInfo.of(Instant.now(), routeCost))
                        .setStation(station.getId())
                        .setStop(stop.getId());
                for (int i = inverseConnections.size() - 1; i >= 0; --i) {
                    builder.addConnection(inverseConnections.get(i));
                }
                return Optional.of(builder.build());
            }

            @Override
            public String toString() {
                return "Dijkstra";
            }
        }
    }

    @Immutable
    interface MinimumFare extends RouteFinderFactory {
        Set<RouteFinderFactory> getRouteFinderFactories();

        @Override
        default RouteFinder create(NetworkGraph networkGraph) {
            Set<io.navigation.RouteFinder> routeFinders = getRouteFinderFactories().stream()
                    .map(factory -> factory.create(networkGraph))
                    .collect(ImmutableSet.toImmutableSet());
            return RouteMultiFinder.parallel("MinimumFare", routeFinders,
                    routes -> routes.min(Comparator.comparingDouble(route -> route.getRouteInfo().getFare()))
            );
        }
    }

    @Immutable
    interface QuickSelect extends RouteFinderFactory {
        Set<RouteFinderFactory> getRouteFinderFactories();

        @Override
        default RouteFinder create(NetworkGraph networkGraph) {
            Set<io.navigation.RouteFinder> routeFinders = getRouteFinderFactories().stream()
                    .map(factory -> factory.create(networkGraph))
                    .collect(ImmutableSet.toImmutableSet());
            return RouteMultiFinder.parallel("QuickSelect", routeFinders, Stream::findAny);
        }
    }

    @Immutable
    interface FirstOption extends RouteFinderFactory {
        List<RouteFinderFactory> getRouteFinderFactories();

        @Override
        default RouteFinder create(NetworkGraph networkGraph) {
            List<io.navigation.RouteFinder> routeFinders = getRouteFinderFactories().stream()
                    .map(factory -> factory.create(networkGraph))
                    .collect(ImmutableList.toImmutableList());
            return RouteMultiFinder.sequential("FirstOption", routeFinders, Stream::findFirst);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class RouteMultiFinder implements RouteFinder {
        private final String name;
        private final Collection<RouteFinder> routeFinders;
        private final Function<Stream<Route>, Optional<Route>> routeSelector;
        private final boolean parallel;

        static RouteMultiFinder sequential(String name, Collection<RouteFinder> routeFinders, Function<Stream<Route>, Optional<Route>> routeSelector) {
            return new RouteMultiFinder(name, routeFinders, routeSelector, false);
        }

        static RouteMultiFinder parallel(String name, Collection<RouteFinder> routeFinders, Function<Stream<Route>, Optional<Route>> routeSelector) {
            return new RouteMultiFinder(name, routeFinders, routeSelector, true);
        }

        @Override
        public Optional<Route> findRoute(Station station, Stop stop) {
            Stream<Route> validOptions = (parallel ? routeFinders.parallelStream() : routeFinders.stream())
                    .map(routeFinder -> routeFinder.findRoute(station, stop))
                    .filter(Optional::isPresent)
                    .map(Optional::get);
            return routeSelector.apply(validOptions);
        }

        @Override
        public String toString() {
            return name + routeFinders;
        }
    }
}
