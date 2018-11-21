package io.navigation;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public interface NetworkView<C> {
    NetworkInfo getNetworkInfo();

    NetworkCoverage<C> getNetworkCoverage();

    NetworkGraph getNetworkGraph();
}
