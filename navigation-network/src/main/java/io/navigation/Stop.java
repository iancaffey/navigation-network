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
public interface Stop extends NetworkNode {
    static Builder builder() {
        return ImmutableStop.builder();
    }

    static Stop of(String id) {
        return ImmutableStop.of(id);
    }

    interface Builder {
        Builder setId(String id);

        Stop build();
    }
}
