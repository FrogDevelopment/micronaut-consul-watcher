package com.frogdevelopment.micronaut.consul.watch.client;

import java.io.Closeable;
import java.util.List;

import org.reactivestreams.Publisher;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;

@Client(id = io.micronaut.discovery.consul.client.v1.ConsulClient.SERVICE_ID, path = "/v1",
        configuration = ConsulConfiguration.class)
@Requires(beans = ConsulConfiguration.class)
public interface IndexConsulClient extends Closeable, AutoCloseable {

    default Publisher<List<KeyValue>> readValues(String key) {
        return readValues(key, true);
    }

    default Publisher<List<KeyValue>> readValues(String key, boolean recurse) {
        return readValues(key, recurse, null);
    }

    @Get(uri = "/kv/{+key}?{&recurse}{&index}", single = true)
    Publisher<List<KeyValue>> readValues(String key, @QueryValue boolean recurse, @Nullable @QueryValue Integer index);

}
