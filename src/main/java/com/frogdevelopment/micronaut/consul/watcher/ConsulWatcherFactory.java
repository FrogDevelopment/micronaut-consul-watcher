package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.serde.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;

@Factory
public class ConsulWatcherFactory {

    @Context
    @Primary
    @Bean(preDestroy = "close")
    Vertx vertx() {
        return Vertx.vertx();
    }

    @Context
    ConsulKVWatcher consulKVWatcher(final Environment environment,
                                    final ApplicationEventPublisher<RefreshEvent> eventPublisher,
                                    final ConsulConfiguration consulConfiguration,
                                    final Vertx vertx,
                                    final BeanProvider<ObjectMapper> objectMapperBeanProvider) {

        final var consulClientOptions = new ConsulClientOptions()
                .setHost(consulConfiguration.getHost())
                .setPort(consulConfiguration.getPort())
                .setSsl(consulConfiguration.isSecure());
        consulConfiguration.getAslToken().ifPresent(consulClientOptions::setAclToken);
        consulConfiguration.getConfiguration().getDatacenter().ifPresent(consulClientOptions::setDc);
        consulConfiguration.getConnectTimeout().ifPresent(connectTimeout -> consulClientOptions.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis())));

        final var format = consulConfiguration.getConfiguration().getFormat();
        return switch (format) {
            case NATIVE -> new NativeConsulKVWatcher(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);
            case YAML -> new YamlConsulKVWatcher(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);
            case PROPERTIES -> new PropertiesConsulKVWatcher(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions);
            case JSON -> new JsonConsulKVWatcher(environment, eventPublisher, consulConfiguration, vertx, consulClientOptions, objectMapperBeanProvider.get());
            default -> throw new UnsupportedOperationException("Unhandled configuration format: " + format);
        };
    }

}
