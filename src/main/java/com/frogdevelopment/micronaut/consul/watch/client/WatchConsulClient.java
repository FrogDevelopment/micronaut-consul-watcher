package com.frogdevelopment.micronaut.consul.watch.client;

import java.util.List;

import com.frogdevelopment.micronaut.consul.watch.WatchConfiguration;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import reactor.core.publisher.Mono;

@Requires(beans = WatchConfiguration.class)
@Client(id = ConsulClient.SERVICE_ID, path = "/v1", configuration = WatchConfiguration.class)
public interface WatchConsulClient {

    @BlockedQuery
    @Get(uri = "/kv/{+key}?{&recurse}{&index}", single = true)
    Mono<List<KeyValue>> watchValues(String key,
                                     @Nullable @QueryValue Boolean recurse,
                                     @Nullable @QueryValue Integer index);

}
