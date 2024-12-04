package com.frogdevelopment.micronaut.consul.watch.watcher;

/**
 * @author LE GALL Benoît
 * @since 1.0.0
 */
public interface Watcher {

    /**
     * Start the watching.
     */
    void start();

    /**
     * Stop the watching.
     */
    void stop();

}
