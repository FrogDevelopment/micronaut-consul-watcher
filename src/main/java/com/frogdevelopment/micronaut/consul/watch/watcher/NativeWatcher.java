package com.frogdevelopment.micronaut.consul.watch.watcher;

import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.frogdevelopment.micronaut.consul.watch.WatchConfiguration;
import com.frogdevelopment.micronaut.consul.watch.client.KeyValue;
import com.frogdevelopment.micronaut.consul.watch.client.WatchConsulClient;
import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.core.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Watcher handling {@link Format#NATIVE} configurations.
 *
 * @author LE GALL Benoît
 * @since 1.0.0
 */
@Slf4j
public final class NativeWatcher extends AbstractWatcher<List<KeyValue>> {

    private final Map<String, String> keysMap = new HashMap<>();

    /**
     * Default constructor
     */
    public NativeWatcher(final List<String> kvPaths,
                         final WatchConsulClient consulClient,
                         final WatchConfiguration watchConfiguration,
                         final PropertiesChangeHandler propertiesChangeHandler) {
        super(kvPaths, consulClient, watchConfiguration, propertiesChangeHandler);
    }

    @Override
    protected Mono<List<KeyValue>> watchValue(final String kvPath) {
        final var modifiedIndex = Optional.ofNullable(kvHolder.get(kvPath))
                .stream()
                .flatMap(List::stream)
                .map(KeyValue::getModifyIndex)
                .max(Integer::compareTo)
                .orElse(NO_INDEX);
        log.debug("Watching kvPath={} with index={}", kvPath, modifiedIndex);
        return consulClient.watchValues(kvPath, true, modifiedIndex);
    }

    @Override
    protected boolean areEqual(final List<KeyValue> previous, final List<KeyValue> next) {
        return KvUtils.areEqual(previous, next);
    }

    @Override
    protected Map<String, Object> readValue(final List<KeyValue> keyValues) {
        if (keyValues == null) {
            return Collections.emptyMap();
        }

        return keyValues.stream()
                .filter(Objects::nonNull)
                .filter(kv -> StringUtils.isNotEmpty(kv.getValue()))
                .collect(Collectors.toMap(this::pathToPropertyKey, keyValue -> new String(decodeValue(keyValue))));
    }

    private String pathToPropertyKey(final KeyValue kv) {
        return keysMap.computeIfAbsent(kv.getKey(), key -> List.of(key.split("/")).getLast());
    }

}
