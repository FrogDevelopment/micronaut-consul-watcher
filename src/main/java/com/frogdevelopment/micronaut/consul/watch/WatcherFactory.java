package com.frogdevelopment.micronaut.consul.watch;

import static io.micronaut.context.env.Environment.DEFAULT_NAME;
import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.DEFAULT_PATH;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;
import com.frogdevelopment.micronaut.consul.watch.watcher.ConfigurationsWatcher;
import com.frogdevelopment.micronaut.consul.watch.watcher.NativeWatcher;
import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.jackson.core.env.JsonPropertySourceLoader;

/**
 * Create all needed {@link Watcher} based on configuration.
 *
 * @author benoit.legall
 * @since 1.0.0
 */
@Slf4j
@Context
@RequiredArgsConstructor
public class WatcherFactory {

    private static final String CONSUL_PATH_SEPARATOR = "/";

    private final Environment environment;
    private final BeanContext beanContext;
    private final ConsulConfiguration consulConfiguration;
    private final ConsulClient consulClient;
    private final PropertiesChangeHandler propertiesChangeHandler;

    private final Map<Format, PropertySourceLoader> loaderByFormatMap = new ConcurrentHashMap<>();

    @PostConstruct
    void createWatchers() {
        final var applicationName = consulConfiguration.getServiceId().orElseThrow();
        final var configurationPath = getConfigurationPath(consulConfiguration);
        final var format = consulConfiguration.getConfiguration().getFormat();

        // Configuration shared by all applications
        final var commonConfigPath = configurationPath + DEFAULT_NAME;
        createWatcher(commonConfigPath, format);

        // Application-specific configuration
        final var applicationSpecificPath = configurationPath + applicationName;
        createWatcher(applicationSpecificPath, format);

        for (final var activeName : environment.getActiveNames()) {
            // Configuration shared by all applications by active environments
            createWatcher(toProfiledPath(commonConfigPath, activeName), format);
            // Application-specific configuration by active environments
            createWatcher(toProfiledPath(applicationSpecificPath, activeName), format);
        }
    }

    private static String getConfigurationPath(final ConsulConfiguration consulConfiguration) {
        return consulConfiguration.getConfiguration().getPath()
                .map(path -> {
                    if (!path.endsWith(CONSUL_PATH_SEPARATOR)) {
                        path += CONSUL_PATH_SEPARATOR;
                    }

                    return path;
                })
                .orElse(DEFAULT_PATH);
    }

    private static String toProfiledPath(final String resource, final String activeName) {
        return resource + "," + activeName;
    }

    void createWatcher(final String kvPath, final Format format) {
        log.debug("Create Watcher for [{}]", kvPath);

        final var watcher = switch (format) {
            case NATIVE -> watchNative(kvPath);
            case JSON, YAML, PROPERTIES -> watchConfigurations(kvPath, resolveLoader(format));
            default -> throw new ConfigurationException("Unhandled configuration format: " + format);
        };

        beanContext.registerSingleton(Watcher.class, watcher);
    }

    private Watcher watchNative(final String keyPath) {
        // adding '/' at the end of the kvPath to distinct 'kvPath/value' from 'kvPath,profile/value'
        return new NativeWatcher(keyPath + CONSUL_PATH_SEPARATOR, consulClient, propertiesChangeHandler);
    }

    private Watcher watchConfigurations(final String keyPath, final PropertySourceLoader propertySourceLoader) {
        return new ConfigurationsWatcher(keyPath, consulClient, propertiesChangeHandler, propertySourceLoader);
    }

    private PropertySourceLoader resolveLoader(final Format formatName) {
        return loaderByFormatMap.computeIfAbsent(formatName, f -> defaultLoader(formatName));
    }

    private static PropertySourceLoader defaultLoader(final Format format) {
        return switch (format) {
            case JSON -> new JsonPropertySourceLoader();
            case PROPERTIES -> new PropertiesPropertySourceLoader();
            case YAML -> new YamlPropertySourceLoader();
            default -> throw new ConfigurationException("Unsupported properties file format: " + format);
        };
    }

}
