package io.navigation;

import org.immutables.gson.Gson;

/**
 * @author Ian Caffey
 * @since 1.0
 */
@Gson.ExpectedSubtypes({Station.class, Stop.class})
public interface NetworkNode {
    String getId();
}
