package com.frogdevelopment.micronaut.consul.watch.context;

import com.frogdevelopment.micronaut.consul.watch.watcher.WatchResult;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle properties' configuration changes to be notified into the Micronaut context.
 *
 * @author LE GALL Benoît
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
     * Update Micronaut context with new properties, then notify the changes.
     * @param results Last Consul poll results
     */
    public void handleChanges(final List<WatchResult> results) {
        if (results.isEmpty()) {
            log.debug("Nothing to do");
            return;
        }

        try {
            // to accept null values, don't use the stream.collect()
            final var allChanges = new HashMap<String, Object>();
            final var newProperties = new HashMap<String, Map<String, Object>>();
            for (final var result : results) {
                final var difference = Maps.difference(result.previous(), result.next());
                if (!difference.areEqual()) {
                    newProperties.put(toPropertySourceName(result), result.next());
                    // updated properties
                    final var differing = checkClassesTypeOnDifference(difference.entriesDiffering());
                    differing.forEach((key, value) -> allChanges.put(key, value.leftValue()));
                    // deleted properties
                    allChanges.putAll(difference.entriesOnlyOnLeft());
                    // added properties
                    difference.entriesOnlyOnRight().forEach((key, value) -> allChanges.put(key, null));
                }
            }

            updatePropertySources(newProperties);

            publishDifferences(allChanges);
        } catch (final Exception e) {
            log.error("Unable to apply configuration changes", e);
        }
    }

    private Map<String, ValueDifference<Object>> checkClassesTypeOnDifference(Map<String, ValueDifference<Object>> differing) {
        for (final var entry : differing.entrySet()) {
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

        return differing;
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

    private void updatePropertySources(final Map<String, Map<String, Object>> mapNewProperties) {
        if (mapNewProperties.isEmpty()) {
            return;
        }

        log.debug("Updating context with new configurations");

        final var updatedPropertySources = new ArrayList<PropertySource>();
        for (final var propertySource : environment.getPropertySources()) {
            final var propertySourceName = propertySource.getName();
            if (mapNewProperties.containsKey(propertySourceName)) {
                final var newProperties = mapNewProperties.get(propertySourceName);
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

    private String toPropertySourceName(final WatchResult watchResult) {
        return propertySourceNames.computeIfAbsent(watchResult.kvPath(), this::resolvePropertySourceName);
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

    private void publishDifferences(final Map<String, Object> changes) {
        if (changes.isEmpty()) {
            return;
        }

        log.debug("Configuration has been updated, publishing RefreshEvent.");
        eventPublisher.publishEvent(new RefreshEvent(changes));
    }
}
