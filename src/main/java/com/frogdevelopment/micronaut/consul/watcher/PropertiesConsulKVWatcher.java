package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class PropertiesConsulKVWatcher extends AbstractFileConsulKVWatcher {

    @Override
    protected Logger getLogger() {
        return log;
    }

    PropertiesConsulKVWatcher(final Environment environment,
                              final ApplicationEventPublisher<RefreshEvent> eventPublisher,
                              final ConsulConfiguration consulConfiguration,
                              final Vertx vertx,
                              final ConsulClientOptions consulClientOptions) {
        super(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);
    }

    @Nonnull
    @Override
    protected Map<String, Object> toProperties(final String value) {
        try {
            final var properties = new Properties();
            properties.load(new StringReader(value));
            return properties.entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
