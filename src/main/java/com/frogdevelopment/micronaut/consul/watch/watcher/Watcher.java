package com.frogdevelopment.micronaut.consul.watch.watcher;

/**
 * @author benoit.legall
 * @since 1.0.0
 */
public interface Watcher {

    /**
     * Call the Consul KV read endpoint for the configured KV path.
     */
    void watchKvPath();
}
