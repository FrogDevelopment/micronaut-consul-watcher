package com.frogdevelopment.micronaut.consul.watch.watcher;

import java.util.Map;

public record WatchResult(
        String kvPath,
        Map<String, Object> previous,
        Map<String, Object> next) {
}
