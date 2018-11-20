package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.gson.Gson;
import org.immutables.value.Value.Immutable;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@Gson.TypeAdapters
@ImmutableNavigationNetworkStyle
public interface RouteOption {
    static Builder builder() {
        return ImmutableRouteOption.builder();
    }

    static RouteOption of(String id, String destination, double fare) {
        return ImmutableRouteOption.of(id, destination, fare);
    }

    String getId();

    String getDestination();

    double getFare();

    interface Builder {
        Builder setId(String id);

        Builder setDestination(String destination);

        Builder setFare(double fare);

        RouteOption build();
    }
}
