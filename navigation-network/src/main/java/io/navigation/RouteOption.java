package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.value.Value.Immutable;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@ImmutableNavigationNetworkStyle
public interface RouteOption {
    static Builder builder() {
        return ImmutableRouteOption.builder();
    }

    static RouteOption of(String destination, double fare) {
        return ImmutableRouteOption.of(destination, fare);
    }

    String getDestination();

    double getFare();

    interface Builder {
        Builder setDestination(String destination);

        Builder setFare(double fare);

        RouteOption build();
    }
}
