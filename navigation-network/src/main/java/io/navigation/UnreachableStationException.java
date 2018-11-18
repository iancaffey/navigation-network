package io.navigation;

import lombok.NonNull;

import java.text.MessageFormat;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public class UnreachableStationException extends UnreachableCoordinateException {
    public UnreachableStationException(@NonNull Object coordinate, @NonNull NavigationNetwork network) {
        super(MessageFormat.format("Unable to find a station in the network {} (v.{} which services {}.",
                network.getNetworkInfo().getName(), network.getNetworkInfo().getVersion(), coordinate),
                coordinate, network);
    }
}
