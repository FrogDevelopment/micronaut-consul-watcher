package com.frogdevelopment.micronaut.consul.watch.watcher;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.discovery.consul.client.v1.KeyValue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format;

/**
 * Watcher handling {@link Format#JSON}, {@link Format#PROPERTIES} and {@link Format#YAML} configurations.
 *
 * @author LE GALL Benoît
 * @since 1.0.0
 */
@Slf4j
public final class ConfigurationsWatcher extends AbstractWatcher<KeyValue> {

    private final PropertySourceReader propertySourceReader;

    /**
     * Default constructor
     */
    public ConfigurationsWatcher(final List<String> kvPaths,
                                 final ConsulClient consulClient,
                                 final PropertiesChangeHandler propertiesChangeHandler,
                                 final PropertySourceReader propertySourceReader) {
        super(kvPaths, consulClient, propertiesChangeHandler);
        this.propertySourceReader = propertySourceReader;
    }

    @Override
    protected Mono<KeyValue> mapToData(String kvPath, final List<KeyValue> kvs) {
        // todo: recurse parameter in ConsulOperations#readValues is always true
        //  => treating the key as a prefix instead of a literal match
        //  => matches also existing profiles
        return Flux.fromIterable(kvs)
                .filter(kv -> kvPath.equals(kv.getKey()))
                .singleOrEmpty();
    }

    @Override
    protected boolean areEqual(KeyValue previous, KeyValue next) {
        return KvUtils.areEqual(previous, next);
    }

    @Override
    protected Map<String, Object> readValue(final KeyValue keyValue) {
        if (keyValue == null || StringUtils.isEmpty(keyValue.getValue())) {
            return Collections.emptyMap();
        }

        return propertySourceReader.read(keyValue.getKey(), decodeValue(keyValue));
    }

}
