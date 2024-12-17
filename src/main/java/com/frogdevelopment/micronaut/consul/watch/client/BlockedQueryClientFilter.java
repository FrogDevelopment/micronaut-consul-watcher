package com.frogdevelopment.micronaut.consul.watch.client;

import lombok.RequiredArgsConstructor;

import jakarta.inject.Singleton;

import com.frogdevelopment.micronaut.consul.watch.WatchConfiguration;

import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;

@BlockedQuery
@Singleton
@ClientFilter
@RequiredArgsConstructor
public class BlockedQueryClientFilter {

    private final WatchConfiguration watchConfiguration;

    @RequestFilter
    public void filter(final MutableHttpRequest<?> request) {
        final var parameters = request.getParameters();
        if (parameters.contains("index")) {
            parameters.add("wait", watchConfiguration.getMaxWaitDuration());
        }
    }
}
