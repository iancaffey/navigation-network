package io.navigation;

import lombok.Getter;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Getter
public class UnreachableCoordinateException extends RuntimeException {
    private final Object coordinate;
    private final NetworkInfo networkInfo;

    public UnreachableCoordinateException(String message, Object coordinate, NetworkInfo networkInfo) {
        super(message);
        this.coordinate = coordinate;
        this.networkInfo = networkInfo;
    }
}
