package com.frogdevelopment.micronaut.consul.watch;

import static io.micronaut.context.env.Environment.DEFAULT_NAME;
import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.DEFAULT_PATH;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Named;

import com.frogdevelopment.micronaut.consul.watch.client.IndexConsulClient;
import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;
import com.frogdevelopment.micronaut.consul.watch.watcher.ConfigurationsWatcher;
import com.frogdevelopment.micronaut.consul.watch.watcher.NativeWatcher;
import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.jackson.core.env.JsonPropertySourceLoader;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;

/**
 * Create all needed {@link Watcher} based on configuration.
 *
 * @author LE GALL Benoît
 * @since 1.0.0
 */
@Factory
@RequiredArgsConstructor
public class WatcherFactory {

    private static final String CONSUL_PATH_SEPARATOR = "/";

    private final Environment environment;
    @Named(TaskExecutors.SCHEDULED)
    private final TaskScheduler taskScheduler;
    private final IndexConsulClient consulClient;
    private final PropertiesChangeHandler propertiesChangeHandler;

    @Context
    @Bean(preDestroy = "stop")
    Watcher createWatcher(final ConsulConfiguration consulConfiguration) {
        final var kvPaths = computeKvPaths(consulConfiguration);

        final var format = consulConfiguration.getConfiguration().getFormat();
        final var watcher = switch (format) {
            case NATIVE -> watchNative(kvPaths);
            case JSON -> watchConfigurations(kvPaths, new JsonPropertySourceLoader());
            case YAML -> watchConfigurations(kvPaths, new YamlPropertySourceLoader());
            case PROPERTIES -> watchConfigurations(kvPaths, new PropertiesPropertySourceLoader());
            default -> throw new ConfigurationException("Unhandled configuration format: " + format);
        };

        watcher.start();
        return watcher;
    }

    private List<String> computeKvPaths(final ConsulConfiguration consulConfiguration) {
        final var applicationName = consulConfiguration.getServiceId().orElseThrow();
        final var configurationPath = getConfigurationPath(consulConfiguration);

        final var kvPaths = new ArrayList<String>();
        // Configuration shared by all applications
        final var commonConfigPath = configurationPath + DEFAULT_NAME;
        kvPaths.add(commonConfigPath);

        // Application-specific configuration
        final var applicationSpecificPath = configurationPath + applicationName;
        kvPaths.add(applicationSpecificPath);

        for (final var activeName : environment.getActiveNames()) {
            // Configuration shared by all applications by active environments
            kvPaths.add(toProfiledPath(commonConfigPath, activeName));
            // Application-specific configuration by active environments
            kvPaths.add(toProfiledPath(applicationSpecificPath, activeName));
        }

        return kvPaths;
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

    private Watcher watchNative(final List<String> keyPaths) {
        // adding '/' at the end of the kvPath to distinct 'kvPath/' from 'kvPath,profile/'
        final var kvPaths = keyPaths.stream().map(path -> path + CONSUL_PATH_SEPARATOR).toList();
        return new NativeWatcher(kvPaths, taskScheduler, consulClient, propertiesChangeHandler);
    }

    private Watcher watchConfigurations(final List<String> kvPaths, final PropertySourceLoader propertySourceLoader) {
        return new ConfigurationsWatcher(kvPaths, taskScheduler, consulClient, propertiesChangeHandler, propertySourceLoader);
    }

}
