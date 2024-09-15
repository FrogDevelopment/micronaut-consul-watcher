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
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

@Slf4j
public class YamlConsulKVWatcher extends AbstractFileConsulKVWatcher {

    private final Yaml yaml = new Yaml();

    @Override
    protected Logger getLogger() {
        return log;
    }

    YamlConsulKVWatcher(final Environment environment,
            final ApplicationEventPublisher<RefreshEvent> eventPublisher,
            final ConsulConfiguration consulConfiguration,
            final Vertx vertx,
            final ConsulClientOptions consulClientOptions) {
        super(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);
    }

    @Nonnull
    @Override
    protected Map<String, Object> toProperties(final String value) {
        return yaml.load(value);
    }

}
