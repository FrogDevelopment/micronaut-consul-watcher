package com.frogdevelopment.micronaut.consul.watch.watcher;

import java.util.Map;

/**
 * @param kvPath Path of the KV
 * @param previous Previous value of the KV
 * @param next New value of the KV
 * @author LE GALL Benoît
 * @since 1.0.0
 */
public record WatchResult(
        String kvPath,
        Map<String, Object> previous,
        Map<String, Object> next) {
}
