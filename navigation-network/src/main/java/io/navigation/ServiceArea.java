package io.navigation;

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
public interface ServiceArea<C> {
    static <C> Global<C> global() {
        return ImmutableServiceArea.Global.<C>builder().build();
    }

    static <C> Empty<C> empty() {
        return ImmutableServiceArea.Empty.<C>builder().build();
    }

    static <C> Outside<C> outside(ServiceArea<? super C> serviceArea) {
        return ImmutableServiceArea.Outside.of(serviceArea);
    }

    static <C> Intersection<C> intersection(Set<ServiceArea<? super C>> serviceAreas) {
        return ImmutableServiceArea.Intersection.of(serviceAreas);
    }

    static <C> Intersection<C> intersection(Iterable<? extends ServiceArea<? super C>> serviceAreas) {
        return ImmutableServiceArea.Intersection.of(serviceAreas);
    }

    static <C> Union<C> union(Set<ServiceArea<? super C>> serviceAreas) {
        return ImmutableServiceArea.Union.of(serviceAreas);
    }

    static <C> Union<C> union(Iterable<? extends ServiceArea<? super C>> serviceAreas) {
        return ImmutableServiceArea.Union.of(serviceAreas);
    }

    boolean contains(C coordinate);

    @Immutable
    interface Global<C> extends ServiceArea<C> {
        @Override
        default boolean contains(C coordinate) {
            return true;
        }
    }

    @Immutable
    interface Empty<C> extends ServiceArea<C> {
        @Override
        default boolean contains(C coordinate) {
            return false;
        }
    }

    @Immutable
    interface Outside<C> extends ServiceArea<C> {
        ServiceArea<? super C> getServiceArea();

        @Override
        default boolean contains(C coordinate) {
            return !getServiceArea().contains(coordinate);
        }
    }

    @Immutable
    interface Intersection<C> extends ServiceArea<C> {
        Set<ServiceArea<? super C>> getServiceAreas();

        @Override
        default boolean contains(C coordinate) {
            return getServiceAreas().stream().allMatch(serviceArea -> serviceArea.contains(coordinate));
        }
    }

    @Immutable
    interface Union<C> extends ServiceArea<C> {
        Set<ServiceArea<? super C>> getServiceAreas();

        @Override
        default boolean contains(C coordinate) {
            return getServiceAreas().stream().anyMatch(serviceArea -> serviceArea.contains(coordinate));
        }
    }
}
