package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.value.Value.Immutable;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@ImmutableNavigationNetworkStyle
public interface Stop extends NetworkNode {
    static Builder builder() {
        return ImmutableStop.builder();
    }

    static Stop of(String id, ServiceArea serviceArea) {
        return ImmutableStop.of(id, serviceArea);
    }

    interface Builder {
        Builder setId(String id);

        Builder setServiceArea(ServiceArea serviceArea);

        Stop build();
    }
}
