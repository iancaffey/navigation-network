package io.navigation;

import lombok.Getter;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Getter
public class UnreachableCoordinateException extends RuntimeException {
    private final Coordinate coordinate;
    private final NetworkInfo networkInfo;

    public UnreachableCoordinateException(String message, Coordinate coordinate, NavigationNetwork network) {
        super(message);
        this.coordinate = coordinate;
        this.networkInfo = network.getNetworkInfo();
    }
}
