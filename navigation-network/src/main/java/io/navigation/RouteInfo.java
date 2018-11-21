package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.gson.Gson;
import org.immutables.value.Value.Immutable;

import java.time.Instant;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@Gson.TypeAdapters
@ImmutableNavigationNetworkStyle
public interface RouteInfo {
    static Builder builder() {
        return ImmutableRouteInfo.builder();
    }

    static RouteInfo of(Instant creationTime, double fare) {
        return ImmutableRouteInfo.of(creationTime, fare);
    }

    Instant getCreationTime();

    double getFare();

    interface Builder {
        Builder setCreationTime(Instant creationTime);

        Builder setFare(double fare);

        RouteInfo build();
    }
}
