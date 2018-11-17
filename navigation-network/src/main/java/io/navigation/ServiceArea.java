package io.navigation;

import com.google.common.collect.ImmutableSet;
import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;

import java.util.Set;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Enclosing
@ImmutableNavigationNetworkStyle
public interface ServiceArea {
    static Global global() {
        return ImmutableServiceArea.Global.of();
    }

    static Empty empty() {
        return ImmutableServiceArea.Empty.of();
    }

    static Intersection intersection(ServiceArea... serviceAreas) {
        return ImmutableServiceArea.Intersection.of(ImmutableSet.copyOf(serviceAreas));
    }

    static Intersection intersection(Set<ServiceArea> serviceAreas) {
        return ImmutableServiceArea.Intersection.of(serviceAreas);
    }

    static Intersection intersection(Iterable<? extends ServiceArea> serviceAreas) {
        return ImmutableServiceArea.Intersection.of(serviceAreas);
    }

    static Union union(ServiceArea... serviceAreas) {
        return ImmutableServiceArea.Union.of(ImmutableSet.copyOf(serviceAreas));
    }

    static Union union(Set<ServiceArea> serviceAreas) {
        return ImmutableServiceArea.Union.of(serviceAreas);
    }

    static Union union(Iterable<? extends ServiceArea> serviceAreas) {
        return ImmutableServiceArea.Union.of(serviceAreas);
    }

    boolean canService(Coordinate coordinate);

    @Immutable
    interface Global extends ServiceArea {
        @Override
        default boolean canService(Coordinate coordinate) {
            return true;
        }
    }

    @Immutable
    interface Empty extends ServiceArea {
        @Override
        default boolean canService(Coordinate coordinate) {
            return false;
        }
    }

    @Immutable
    interface Intersection extends ServiceArea {
        Set<ServiceArea> getServiceAreas();

        @Override
        default boolean canService(Coordinate coordinate) {
            return getServiceAreas().stream().allMatch(serviceArea -> serviceArea.canService(coordinate));
        }
    }

    @Immutable
    interface Union extends ServiceArea {
        Set<ServiceArea> getServiceAreas();

        @Override
        default boolean canService(Coordinate coordinate) {
            return getServiceAreas().stream().anyMatch(serviceArea -> serviceArea.canService(coordinate));
        }
    }
}
