package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.serde.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class JsonConsulKVWatcher extends AbstractFileConsulKVWatcher {

    private final ObjectMapper objectMapper;

    @Override
    protected Logger getLogger() {
        return log;
    }

    JsonConsulKVWatcher(final Environment environment,
                        final ApplicationEventPublisher<RefreshEvent> eventPublisher,
                        final ConsulConfiguration consulConfiguration,
                        final Vertx vertx,
                        final ConsulClientOptions consulClientOptions,
                        ObjectMapper objectMapper) {
        super(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);
        this.objectMapper = objectMapper;
    }

    @Nonnull
    @Override
    protected Map<String, Object> toProperties(final String value) {
        try {
            //noinspection unchecked
            return objectMapper.readValue(value, Map.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
