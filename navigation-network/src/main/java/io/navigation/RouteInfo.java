package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.value.Value.Immutable;

import java.time.Instant;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@ImmutableNavigationNetworkStyle
public interface RouteInfo {
    static Builder builder() {
        return ImmutableRouteInfo.builder();
    }

    static RouteInfo of(NetworkInfo networkInfo, Instant creationTime, double fare) {
        return ImmutableRouteInfo.of(networkInfo, creationTime, fare);
    }

    NetworkInfo getNetworkInfo();

    Instant getCreationTime();

    double getFare();

    interface Builder {
        Builder setNetworkInfo(NetworkInfo networkInfo);

        Builder setCreationTime(Instant creationTime);

        Builder setFare(double fare);

        RouteInfo build();
    }
}
