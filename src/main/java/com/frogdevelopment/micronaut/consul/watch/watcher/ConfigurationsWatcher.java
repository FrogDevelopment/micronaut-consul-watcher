package com.frogdevelopment.micronaut.consul.watch.watcher;

import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.frogdevelopment.micronaut.consul.watch.WatchConfiguration;
import com.frogdevelopment.micronaut.consul.watch.client.KeyValue;
import com.frogdevelopment.micronaut.consul.watch.client.WatchConsulClient;
import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.core.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                                 final WatchConsulClient consulClient,
                                 final WatchConfiguration watchConfiguration,
                                 final PropertiesChangeHandler propertiesChangeHandler,
                                 final PropertySourceReader propertySourceReader) {
        super(kvPaths, consulClient, watchConfiguration, propertiesChangeHandler);
        this.propertySourceReader = propertySourceReader;
    }

    @Override
    protected Mono<KeyValue> watchValue(String kvPath) {
        final var modifiedIndex = Optional.ofNullable(kvHolder.get(kvPath))
                .map(KeyValue::getModifyIndex)
                .orElse(NO_INDEX);
        log.debug("Watching kvPath={} with index={}", kvPath, modifiedIndex);
        return consulClient.watchValues(kvPath, false, modifiedIndex)
                .flatMapMany(Flux::fromIterable)
                .filter(kv -> kvPath.equals(kv.getKey()))
                .singleOrEmpty();
    }

    @Override
    protected boolean areEqual(final KeyValue previous, final KeyValue next) {
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
