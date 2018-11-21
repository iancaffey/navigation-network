package io.navigation;

/**
 * @author Ian Caffey
 * @since 1.0
 */
public interface NetworkView<C> extends NetworkGraphView {
    NetworkCoverage<C> getNetworkCoverage();
}
