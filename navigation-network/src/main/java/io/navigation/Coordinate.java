package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.value.Value.Immutable;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@ImmutableNavigationNetworkStyle
public interface Coordinate {
    static Builder builder() {
        return ImmutableCoordinate.builder();
    }

    static Coordinate of(double x, double y, double z) {
        return ImmutableCoordinate.of(x, y, z);
    }

    double getX();

    double getY();

    double getZ();

    interface Builder {
        Builder setX(double x);

        Builder setY(double y);

        Builder setZ(double z);

        Coordinate build();
    }
}
