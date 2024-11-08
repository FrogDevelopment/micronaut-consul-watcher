package com.frogdevelopment.micronaut.consul.watch.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.inject.Singleton;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;

/**
 * Handle properties' configuration changes to be notified into the Micronaut context.
 *
 * @author benoit.legall
 * @since 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class PropertiesChangeHandler {

    private final Environment environment;
    private final ApplicationEventPublisher<RefreshEvent> eventPublisher;

    private final Map<String, String> propertySourceNames = new ConcurrentHashMap<>();

    /**
     * @param kvPath   Path of the watched KV
     * @param previous Previous data before changes
     * @param next     New data after changes
     */
    public synchronized void handleChanges(@NonNull final String kvPath, @NonNull final Map<String, Object> previous,
                                           @NonNull final Map<String, Object> next) {
        try {
            final var difference = Maps.difference(previous, next);
            if (!difference.areEqual()) {
                checkClassesTypeOnDifference(difference);
                updatePropertySource(kvPath, next);
                publishDifferences(difference);
            }
        } catch (final Exception e) {
            log.error("Unable to apply configuration changes for kvPath={}, previous={} and next={}", kvPath, previous, next, e);
        }
    }

    private void checkClassesTypeOnDifference(final MapDifference<String, Object> difference) {
        for (final var entry : difference.entriesDiffering().entrySet()) {
            final var leftValue = entry.getValue().leftValue();
            final var rightValue = entry.getValue().rightValue();
            if (leftValue != null && rightValue != null) {
                final var leftClass = leftValue.getClass();
                final var rightClass = rightValue.getClass();
                if (areClassesTypeIncompatible(leftClass, rightClass)) {
                    throw new IllegalStateException(String.format("Incompatible type for %s: [%s] <-> [%s]", entry.getKey(), leftClass, rightClass));
                }
            }
        }
    }

    private boolean areClassesTypeIncompatible(final Class<?> leftClass, final Class<?> rightClass) {
        if (leftClass.equals(rightClass)) {
            return false;
        }

        // Micronaut will handle the conversion between then if needed
        return isNotNumber(leftClass) || isNotNumber(rightClass);

        // maybe later some other check will come
    }

    private static boolean isNotNumber(final Class<?> clazz) {
        return !Number.class.isAssignableFrom(clazz);
    }

    private void updatePropertySource(final String kvPath, final Map<String, Object> newProperties) {
        log.debug("Updating context with new configuration from [{}]", kvPath);

        final var propertySourceName = propertySourceNames.computeIfAbsent(kvPath, this::resolvePropertySourceName);
        final var updatedPropertySources = new ArrayList<PropertySource>();
        for (final var propertySource : environment.getPropertySources()) {
            if (propertySource.getName().equals(propertySourceName)) {
                // creating a new PropertySource with new values but keeping the order
                updatedPropertySources.add(PropertySource.of(propertySourceName, newProperties, propertySource.getOrder()));
            } else {
                updatedPropertySources.add(propertySource);
            }
        }

        updatedPropertySources.stream()
                // /!\ re-setting all the propertySources sorted by Order, to keep precedence
                .sorted(Comparator.comparing(PropertySource::getOrder))
                .forEach(environment::addPropertySource);
    }

    private String resolvePropertySourceName(final String kvPath) {
        final var propertySourceName = List.of(kvPath.split("/")).getLast();
        final var tokens = propertySourceName.split(",");
        if (tokens.length == 1) {
            return ConsulClient.SERVICE_ID + '-' + propertySourceName;
        }

        final var name = tokens[0];
        final var envName = tokens[1];

        return ConsulClient.SERVICE_ID + '-' + name + '[' + envName + ']';
    }

    private void publishDifferences(final MapDifference<String, Object> difference) {
        log.debug("Configuration has been updated, publishing RefreshEvent.");
        final var changes = new HashMap<String, Object>();
        // to accept null values, don't use the stream.collect()
        difference.entriesDiffering().forEach((key, value) -> changes.put(key, value.leftValue()));
        eventPublisher.publishEvent(new RefreshEvent(changes));
    }
}
