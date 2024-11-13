package com.frogdevelopment.micronaut.consul.watch;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;
import com.frogdevelopment.micronaut.consul.watch.watcher.ConfigurationsWatcher;
import com.frogdevelopment.micronaut.consul.watch.watcher.NativeWatcher;
import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import io.micronaut.jackson.core.env.JsonPropertySourceLoader;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static io.micronaut.context.env.Environment.DEFAULT_NAME;
import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.DEFAULT_PATH;

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
    private final ConsulClient consulClient;
    private final PropertiesChangeHandler propertiesChangeHandler;

    @Bean
    Watcher createWatcher(final ConsulConfiguration consulConfiguration) {
        final var kvPaths = computeKvPaths(consulConfiguration);

        final var format = consulConfiguration.getConfiguration().getFormat();
        return switch (format) {
            case NATIVE -> watchNative(kvPaths);
            case JSON, YAML, PROPERTIES -> watchConfigurations(kvPaths, resolveLoader(format));
            default -> throw new ConfigurationException("Unhandled configuration format: " + format);
        };
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
        // adding '/' at the end of the kvPath to distinct 'kvPath/value' from 'kvPath,profile/value'
        final var kvPaths = keyPaths.stream().map(path -> path + CONSUL_PATH_SEPARATOR).toList();
        return new NativeWatcher(kvPaths, consulClient, propertiesChangeHandler);
    }

    private Watcher watchConfigurations(final List<String> kvPaths, final PropertySourceLoader propertySourceLoader) {
        return new ConfigurationsWatcher(kvPaths, consulClient, propertiesChangeHandler, propertySourceLoader);
    }

    private static PropertySourceLoader resolveLoader(final Format format) {
        return switch (format) {
            case JSON -> new JsonPropertySourceLoader();
            case PROPERTIES -> new PropertiesPropertySourceLoader();
            case YAML -> new YamlPropertySourceLoader();
            default -> throw new ConfigurationException("Unsupported properties file format: " + format);
        };
    }

}
