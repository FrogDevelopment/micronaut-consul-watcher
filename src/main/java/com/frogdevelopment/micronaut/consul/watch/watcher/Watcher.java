package com.frogdevelopment.micronaut.consul.watch.watcher;

/**
 * @author LE GALL Benoît
 * @since 1.0.0
 */
public interface Watcher {

    /**
     * Call the Consul KV read endpoint for the configured KV paths.
     */
    void watchKVs();
}
