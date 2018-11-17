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
public interface NetworkInfo {
    static Builder builder() {
        return ImmutableNetworkInfo.builder();
    }

    static NetworkInfo of(String name, long version, Instant creationTime) {
        return ImmutableNetworkInfo.of(name, version, creationTime);
    }

    String getName();

    long getVersion();

    Instant getCreationTime();

    interface Builder {
        Builder setName(String name);

        Builder setVersion(long version);

        Builder setCreationTime(Instant creationTime);

        NetworkInfo build();
    }
}
