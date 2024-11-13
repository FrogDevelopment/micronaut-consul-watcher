package com.frogdevelopment.micronaut.consul.watch.watcher;

import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.discovery.consul.client.v1.KeyValue;
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
                         final ConsulClient consulClient,
                         final PropertiesChangeHandler propertiesChangeHandler) {
        super(kvPaths, consulClient, propertiesChangeHandler);
    }

    @Override
    protected Mono<List<KeyValue>> mapToData(String kvPath, List<KeyValue> kvs) {
        return Mono.just(kvs);
    }

    @Override
    protected boolean areEqual(final List<KeyValue> next, final List<KeyValue> previous) {
        return KvUtils.areEqual(next, previous);
    }

    @Override
    protected Map<String, Object> readValue(final List<KeyValue> keyValues) {
        if (keyValues == null) {
            return Collections.emptyMap();
        }

        return keyValues.stream()
                .filter(Objects::nonNull)
                .filter(kv -> StringUtils.isNotEmpty(kv.getValue()))
                .collect(Collectors.toMap(this::pathToPropertyKey, this::readValue));
    }

    private String pathToPropertyKey(final KeyValue kv) {
        return keysMap.computeIfAbsent(kv.getKey(), key -> List.of(key.split("/")).getLast());
    }

    private String readValue(final KeyValue keyValue) {
        return new String(decodeValue(keyValue));
    }

}
