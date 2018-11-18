package io.navigation;

import io.navigation.immutables.ImmutableNavigationNetworkStyle;
import org.immutables.value.Value.Immutable;

import java.util.Map;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Immutable
@ImmutableNavigationNetworkStyle
public interface NetworkCoverage<C> {
    static <C> Builder<C> builder() {
        return ImmutableNetworkCoverage.builder();
    }

    Map<String, ServiceArea<C>> getStationServiceAreas();

    Map<String, ServiceArea<C>> getStopServiceAreas();

    default boolean contains(Station station, C coordinate) {
        String id = station.getId();
        Map<String, ServiceArea<C>> serviceAreas = getStationServiceAreas();
        return serviceAreas.containsKey(id) && serviceAreas.get(id).contains(coordinate);
    }

    default boolean contains(Stop stop, C coordinate) {
        String id = stop.getId();
        Map<String, ServiceArea<C>> serviceAreas = getStopServiceAreas();
        return serviceAreas.containsKey(id) && serviceAreas.get(id).contains(coordinate);
    }

    interface Builder<C> {
        Builder<C> putStationServiceArea(String station, ServiceArea<C> serviceArea);

        Builder<C> putStationServiceArea(Map.Entry<String, ? extends ServiceArea<C>> entry);

        Builder<C> setStationServiceAreas(Map<String, ? extends ServiceArea<C>> entries);

        Builder<C> putAllStationServiceAreas(Map<String, ? extends ServiceArea<C>> entries);

        Builder<C> putStopServiceArea(String stop, ServiceArea<C> serviceArea);

        Builder<C> putStopServiceArea(Map.Entry<String, ? extends ServiceArea<C>> entry);

        Builder<C> setStopServiceAreas(Map<String, ? extends ServiceArea<C>> entries);

        Builder<C> putAllStopServiceAreas(Map<String, ? extends ServiceArea<C>> entries);

        NetworkCoverage<C> build();
    }
}
