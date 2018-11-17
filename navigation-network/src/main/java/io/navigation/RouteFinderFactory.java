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
    interface Direct extends RouteFinderFactory {
        @Override
        default RouteFinder create(NetworkInfo networkInfo, Set<Station> stations, Set<Stop> stops) {
            return new RouteFinder(networkInfo, stations, stops);
        }

        class RouteFinder implements io.navigation.RouteFinder {
            private final NetworkInfo networkInfo;
            private final Map<Station, Map<Stop, Set<RouteOption>>> directRouteOptions;

            private RouteFinder(@NonNull NetworkInfo networkInfo, @NonNull Set<Station> stations, @NonNull Set<Stop> stops) {
                this.networkInfo = networkInfo;
                Map<String, Stop> stopsById = stops.stream().collect(ImmutableMap.toImmutableMap(Stop::getId, Function.identity()));
                this.directRouteOptions = stations.stream().collect(ImmutableMap.toImmutableMap(
                        Function.identity(),
                        station -> station.getDestinations().stream().collect(ImmutableMap.toImmutableMap(
                                option -> stopsById.get(option.getDestination()),
                                ImmutableSet::of,
                                Sets::intersection
                        ))
                ));
            }

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
        }
    }

    @Immutable
    interface Dijkstra extends RouteFinderFactory {
        @Override
        default RouteFinder create(NetworkInfo networkInfo, Set<Station> stations, Set<Stop> stops) {
            return new RouteFinder(networkInfo, stations, stops);
        }

        class RouteFinder implements io.navigation.RouteFinder {
            private final NetworkInfo networkInfo;
            private final Map<String, Station> stationsById;
            private final Map<String, Stop> stopsById;

            public RouteFinder(@NonNull NetworkInfo networkInfo, @NonNull Set<Station> stations, @NonNull Set<Stop> stops) {
                this.networkInfo = networkInfo;
                this.stationsById = stations.stream().collect(ImmutableMap.toImmutableMap(Station::getId, Function.identity()));
                this.stopsById = stops.stream().collect(ImmutableMap.toImmutableMap(Stop::getId, Function.identity()));
            }

            @Override
            public Optional<Route> findRoute(@NonNull Station station, @NonNull Stop stop) {
                //TODO: Do regular Dijkstra's across the Station network to find route with minimum fare to the stop
                return Optional.empty();
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
        }
    }
}
