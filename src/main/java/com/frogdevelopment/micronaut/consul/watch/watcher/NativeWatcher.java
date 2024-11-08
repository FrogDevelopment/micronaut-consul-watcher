package com.frogdevelopment.micronaut.consul.watch.watcher;

import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.discovery.consul.client.v1.KeyValue;

/**
 * Watcher handling {@link Format#NATIVE} configurations.
 *
 * @author benoit.legall
 * @since 1.0.0
 */
@Slf4j
public final class NativeWatcher extends AbstractWatcher<List<KeyValue>> {

    /**
     * Default constructor
     */
    public NativeWatcher(final String kvPath,
                         final ConsulClient consulClient,
                         final PropertiesChangeHandler propertiesChangeHandler) {
        super(kvPath, consulClient, propertiesChangeHandler);
    }

    @Override
    protected void handleConfigurations(final List<KeyValue> nextKVs) {
        final var previousKVs = getAndSetPrevious(nextKVs);

        if (previousKVs == null) {
            log.debug("Watcher initialisation for kv path={}", kvPath);
            return;
        }

        if (areEqual(nextKVs, previousKVs)) {
            handleNoChange();
        } else {
            final var previousValue = readValues(previousKVs);
            final var nextValue = readValues(nextKVs);

            handleSuccess(previousValue, nextValue);
        }
    }

    private static boolean areEqual(final List<KeyValue> nextKVs, final List<KeyValue> previousKVs) {
        if (nextKVs.size() != previousKVs.size()) {
            return false;
        }

        nextKVs.sort(Comparator.comparing(KeyValue::getKey));
        previousKVs.sort(Comparator.comparing(KeyValue::getKey));

        for (int i = 0; i < nextKVs.size(); i++) {
            final var nextKV = nextKVs.get(i);
            final var previousKV = previousKVs.get(i);
            if (!areEqual(previousKV, nextKV)) {
                return false;
            }
        }

        return true;
    }

    private Map<String, Object> readValues(final List<KeyValue> keyValues) {
        if (keyValues == null) {
            return Collections.emptyMap();
        }

        return keyValues.stream()
                .filter(Objects::nonNull)
                .filter(kv -> StringUtils.isNotEmpty(kv.getValue()))
                .collect(Collectors.toMap(this::pathToPropertyKey, this::readValue));
    }

    private String pathToPropertyKey(final KeyValue kv) {
        return List.of(kv.getKey().split("/")).getLast();
    }

    private String readValue(final KeyValue keyValue) {
        return new String(decodeValue(keyValue));
    }

}
