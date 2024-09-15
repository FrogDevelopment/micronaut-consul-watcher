package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.Watch;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

@Slf4j
public abstract class AbstractFileConsulKVWatcher extends AbstractConsulKVWatcher<KeyValue> {

    AbstractFileConsulKVWatcher(final Environment environment,
                                final ApplicationEventPublisher<RefreshEvent> eventPublisher,
                                final ConsulConfiguration consulConfiguration,
                                final Vertx vertx,
                                final ConsulClientOptions consulClientOptions) {
        super(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);
    }

    @Override
    protected Watch<KeyValue> getWatcher(final String key, final Vertx vertx, final ConsulClientOptions consulClientOptions) {
        return Watch.key(key, vertx, consulClientOptions);
    }

    @Nonnull
    @Override
    protected Map<String, Object> toProperties(final String key, @Nullable final KeyValue value) {
        if (value == null || !value.isPresent()) {
            return Collections.emptyMap();
        }
        return toProperties(value.getValue());
    }

    @Nonnull
    protected abstract Map<String, Object> toProperties(@Nullable final String value);

}
