package com.frogdevelopment.micronaut.consul.watch.watcher;

import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.discovery.consul.client.v1.KeyValue;

/**
 * Watcher handling {@link Format#JSON}, {@link Format#PROPERTIES} and {@link Format#YAML} configurations.
 *
 * @author benoit.legall
 * @since 1.0.0
 */
@Slf4j
public final class ConfigurationsWatcher extends AbstractWatcher<KeyValue> {

    private final PropertySourceReader propertySourceReader;

    /**
     * Default constructor
     */
    public ConfigurationsWatcher(final String kvPath,
                                 final ConsulClient consulClient,
                                 final PropertiesChangeHandler propertiesChangeHandler,
                                 final PropertySourceReader propertySourceReader) {
        super(kvPath, consulClient, propertiesChangeHandler);
        this.propertySourceReader = propertySourceReader;
    }

    @Override
    protected void handleConfigurations(final List<KeyValue> nextKVs) {
        final var nextKV = nextKVs.getFirst();
        final var previousKV = getAndSetPrevious(nextKV);

        if (previousKV == null) {
            log.debug("Watcher initialisation for kv path={}", kvPath);
            return;
        }

        if (areEqual(previousKV, nextKV)) {
            handleNoChange();
        } else {
            final var previousValue = readValue(previousKV);
            final var nextValue = readValue(nextKV);

            handleSuccess(previousValue, nextValue);
        }
    }

    private Map<String, Object> readValue(final KeyValue keyValue) {
        if (keyValue == null || StringUtils.isEmpty(keyValue.getValue())) {
            return Collections.emptyMap();
        }

        return propertySourceReader.read(keyValue.getKey(), decodeValue(keyValue));
    }

}
