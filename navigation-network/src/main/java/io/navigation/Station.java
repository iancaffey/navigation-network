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
public interface Station extends NetworkNode {
    static Builder builder() {
        return ImmutableStation.builder();
    }

    static Station of(String id, Set<RouteOption> connections, Set<RouteOption> destinations) {
        return ImmutableStation.of(id, connections, destinations);
    }

    static Station of(String id, Iterable<? extends RouteOption> connections, Iterable<? extends RouteOption> destinations) {
        return ImmutableStation.of(id, connections, destinations);
    }

    @Override
    String getId();

    /**
     * Represents the set of {@link Station} which can be traveled to within the network.
     * <p>
     * Each {@link RouteOption} in the set corresponds to a {@link Station#getId()}.
     *
     * @return the set of route options for connecting stations
     */
    Set<RouteOption> getConnections();

    /**
     * Represents the set of {@link Stop} which can be destinations of travelers from the station.
     * <p>
     * Each {@link RouteOption} in the set corresponds to a {@link Stop#getId()}.
     *
     * @return the set of route options for destination stops
     */
    Set<RouteOption> getDestinations();

    interface Builder {
        Builder setId(String id);

        Builder addConnection(RouteOption connection);

        Builder addConnections(RouteOption... connections);

        Builder addAllConnections(Iterable<? extends RouteOption> connections);

        Builder setConnections(Iterable<? extends RouteOption> connections);

        Builder addDestination(RouteOption destination);

        Builder addDestinations(RouteOption... destinations);

        Builder addAllDestinations(Iterable<? extends RouteOption> destinations);

        Builder setDestinations(Iterable<? extends RouteOption> destinations);

        Station build();
    }
}
