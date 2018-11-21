package io.navigation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

    RouteFinder create(NetworkView<?> networkView);

    @Immutable
    interface Cached extends RouteFinderFactory {
        RouteFinderFactory getRouteFinderFactory();

        long getTimeToLive();

        TimeUnit getTimeUnit();

        @Override
        default RouteFinder create(NetworkView<?> networkView) {
            io.navigation.RouteFinder delegate = getRouteFinderFactory().create(networkView);
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
        default RouteFinder create(NetworkView<?> networkView) {
            Map<String, Stop> stopsById = networkView.getStops().stream().collect(ImmutableMap.toImmutableMap(Stop::getId, Function.identity()));
            Map<Station, Map<Stop, Set<RouteOption>>> directRouteOptions = networkView.getStations().stream().collect(ImmutableMap.toImmutableMap(
                    Function.identity(),
                    station -> station.getDestinations().stream().collect(ImmutableMap.toImmutableMap(
                            option -> stopsById.get(option.getDestination()),
                            ImmutableSet::of,
                            (left, right) -> Stream.of(left, right)
                                    .flatMap(Set::stream)
                                    .collect(ImmutableSet.toImmutableSet())
                    ))
            ));
            return new RouteFinder(networkView.getNetworkInfo(), directRouteOptions);
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
                RouteOption shortestRouteOption = directRouteOptions.stream()
                        .min(Comparator.comparingDouble(RouteOption::getFare))
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
        default RouteFinder create(NetworkView<?> networkView) {
            Map<String, Station> stationsById = networkView.getStations().stream().collect(ImmutableMap.toImmutableMap(Station::getId, Function.identity()));
            Map<String, Stop> stopsById = networkView.getStops().stream().collect(ImmutableMap.toImmutableMap(Stop::getId, Function.identity()));
            return new RouteFinder(networkView.getNetworkInfo(), stationsById, stopsById);
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
    interface MinimumFare extends RouteFinderFactory {
        Set<RouteFinderFactory> getRouteFinderFactories();

        @Override
        default RouteFinder create(NetworkView<?> networkView) {
            Set<io.navigation.RouteFinder> routeFinders = getRouteFinderFactories().stream()
                    .map(factory -> factory.create(networkView))
                    .collect(ImmutableSet.toImmutableSet());
            return new RouteMultiFinder("MinimumFare", routeFinders,
                    routes -> routes.min(Comparator.comparingDouble(route -> route.getRouteInfo().getFare()))
            );
        }
    }

    @Immutable
    interface QuickSelect extends RouteFinderFactory {
        Set<RouteFinderFactory> getRouteFinderFactories();

        @Override
        default RouteFinder create(NetworkView<?> networkView) {
            Set<io.navigation.RouteFinder> routeFinders = getRouteFinderFactories().stream()
                    .map(factory -> factory.create(networkView))
                    .collect(ImmutableSet.toImmutableSet());
            return new RouteMultiFinder("QuickSelect", routeFinders, Stream::findAny);
        }
    }

    @RequiredArgsConstructor
    class RouteMultiFinder implements RouteFinder {
        private final String name;
        private final Set<RouteFinder> routeFinders;
        private final Function<Stream<Route>, Optional<Route>> routeSelector;

        @Override
        public Optional<Route> findRoute(Station station, Stop stop) {
            Stream<Route> validOptions = routeFinders.parallelStream()
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
