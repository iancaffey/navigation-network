package io.navigation;

import lombok.NonNull;

import java.text.MessageFormat;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public class UnreachableStopException extends UnreachableCoordinateException {
    public UnreachableStopException(@NonNull Object coordinate, @NonNull NavigationNetwork network) {
        super(MessageFormat.format("Unable to find a stop in the network {}/{} which services {}.",
                network.getNetworkInfo().getName(), network.getNetworkInfo().getVersion(), coordinate),
                coordinate, network.getNetworkInfo());
    }
}
