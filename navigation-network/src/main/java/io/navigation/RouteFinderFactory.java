package io.navigation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

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

    static Competing competing(Executor executor, RouteFinderFactory... routeFinderFactories) {
        return ImmutableRouteFinderFactory.Competing.of(executor, ImmutableSet.copyOf(routeFinderFactories));
    }

    static Competing competing(Executor executor, Set<RouteFinderFactory> routeFinderFactories) {
        return ImmutableRouteFinderFactory.Competing.of(executor, routeFinderFactories);
    }

    static Competing competing(Executor executor, Iterable<? extends RouteFinderFactory> routeFinderFactories) {
        return ImmutableRouteFinderFactory.Competing.of(executor, routeFinderFactories);
    }

    RouteFinder create(NetworkInfo networkInfo, Set<Station> stations, Set<Stop> stops);

    @Immutable
    interface Cached extends RouteFinderFactory {
        RouteFinderFactory getRouteFinderFactory();

        long getTimeToLive();

        TimeUnit getTimeUnit();

        @Override
        default RouteFinder create(NetworkInfo networkInfo, Set<Station> stations, Set<Stop> stops) {
            io.navigation.RouteFinder delegate = getRouteFinderFactory().create(networkInfo, stations, stops);
            return new RouteFinder(delegate, getTimeUnit().toNanos(getTimeToLive()));
        }

        @RequiredArgsConstructor
        class RouteFinder implements io.navigation.RouteFinder {
            private final Map<CacheKey, Route> routes = new ConcurrentHashMap<>(); //storing an Optional/long pair would allow caching empty responses from the delegate
            private final io.navigation.RouteFinder delegate;
            private final long timeToLive;

            @Override
            public Optional<Route> findRoute(Station station, Stop stop) {
                long time = System.nanoTime();
                CacheKey key = CacheKey.of(station.getId(), stop.getId());
                Route cachedRoute = routes.get(key);
                if (cachedRoute != null && (time - cachedRoute.getRouteInfo().getCreationTime().getNano()) <= timeToLive) {
                    return Optional.of(cachedRoute);
                }
                Optional<Route> route = delegate.findRoute(station, stop);
                route.ifPresent(r -> routes.put(key, r));
                return route;
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
        default RouteFinder create(NetworkInfo networkInfo, Set<Station> stations, Set<Stop> stops) {
            Map<String, Stop> stopsById = stops.stream().collect(ImmutableMap.toImmutableMap(Stop::getId, Function.identity()));
            Map<Station, Map<Stop, Set<RouteOption>>> directRouteOptions = stations.stream().collect(ImmutableMap.toImmutableMap(
                    Function.identity(),
                    station -> station.getDestinations().stream().collect(ImmutableMap.toImmutableMap(
                            option -> stopsById.get(option.getDestination()),
                            ImmutableSet::of,
                            Sets::intersection
                    ))
            ));
            return new RouteFinder(networkInfo, directRouteOptions);
        }

        @RequiredArgsConstructor
        class RouteFinder implements io.navigation.RouteFinder {
            private final NetworkInfo networkInfo;
            private final Map<Station, Map<Stop, Set<RouteOption>>> directRouteOptions;

            @Override
            public Optional<Route> findRoute(@NonNull Station station, @NonNull Stop stop) {
                Map<Stop, Set<RouteOption>> directRoutesFromStation = directRouteOptions.get(station);
                if (directRoutesFromStation == null) {
                    throw new IllegalArgumentException("Unable to find direct routes from " + station + ".");
                }
                Set<RouteOption> directRouteOptions = directRoutesFromStation.get(stop);
                if (directRouteOptions == null) {
                    return Optional.empty();
                }
                RouteOption shortestRouteOption = directRouteOptions.stream().min(Comparator.comparingDouble(RouteOption::getFare))
                        .orElseThrow(() -> new IllegalArgumentException("Unable to find valid route option out of direct routes to " + stop + "."));
                return Optional.of(Route.builder()
                        .setRouteInfo(RouteInfo.of(networkInfo, Instant.now(), shortestRouteOption.getFare()))
                        .setStation(station)
                        .setStop(stop)
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
        default RouteFinder create(NetworkInfo networkInfo, Set<Station> stations, Set<Stop> stops) {
            Map<String, Station> stationsById = stations.stream().collect(ImmutableMap.toImmutableMap(Station::getId, Function.identity()));
            Map<String, Stop> stopsById = stops.stream().collect(ImmutableMap.toImmutableMap(Stop::getId, Function.identity()));
            return new RouteFinder(networkInfo, stationsById, stopsById);
        }

        @RequiredArgsConstructor
        class RouteFinder implements io.navigation.RouteFinder {
            private final NetworkInfo networkInfo;
            private final Map<String, Station> stationsById;
            private final Map<String, Stop> stopsById;

            @Override
            public Optional<Route> findRoute(@NonNull Station station, @NonNull Stop stop) {
                //TODO: Do regular Dijkstra's across the Station network to find route with minimum fare to the stop
                return Optional.empty();
            }

            @Override
            public String toString() {
                return "Dijkstra";
            }
        }
    }

    @Immutable
    interface Competing extends RouteFinderFactory {
        Executor getExecutor();

        Set<RouteFinderFactory> getRouteFinderFactories();

        @Override
        default RouteFinder create(NetworkInfo networkInfo, Set<Station> stations, Set<Stop> stops) {
            Set<io.navigation.RouteFinder> routeFinders = getRouteFinderFactories().stream()
                    .map(factory -> factory.create(networkInfo, stations, stops))
                    .collect(ImmutableSet.toImmutableSet());
            return new RouteFinder(getExecutor(), routeFinders);
        }

        @RequiredArgsConstructor
        class RouteFinder implements io.navigation.RouteFinder {
            private final Executor executor;
            private final Set<io.navigation.RouteFinder> routeFinders;

            @Override
            public Optional<Route> findRoute(Station station, Stop stop) {
                int routeFinderCount = routeFinders.size();
                List<Future<Optional<Route>>> workers = new ArrayList<>(routeFinderCount);
                CompletionService<Optional<Route>> completionService = new ExecutorCompletionService<>(executor);
                Optional<Route> route = Optional.empty();
                try {
                    routeFinders.forEach(routeFinder -> workers.add(completionService.submit(() -> routeFinder.findRoute(station, stop))));
                    //try up to routeFinderCount times to take a valid result from the service, otherwise assume no route possible
                    for (int i = 0; i < routeFinderCount; ++i) {
                        try {
                            Future<Optional<Route>> nextCompletedTask = completionService.take();
                            Optional<Route> result = nextCompletedTask.get();
                            if (result != null && result.isPresent()) {
                                route = result;
                                break;
                            }
                        } catch (ExecutionException | InterruptedException ignored) {
                            //ignore any exception from worker route finders (or while waiting) to give the next route finder a chance to return a valid route
                        }
                    }
                } finally {
                    //cleanup any remaining active workers
                    workers.parallelStream().forEach(worker -> worker.cancel(true));
                }
                return route;
            }

            @Override
            public String toString() {
                return "Competing" + routeFinders;
            }
        }
    }
}
