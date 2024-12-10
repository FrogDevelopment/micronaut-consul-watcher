package com.frogdevelopment.micronaut.consul.watch;

import java.time.Duration;
import java.util.Optional;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.discovery.consul.condition.RequiresConsul;
import io.micronaut.http.client.HttpClientConfiguration;

@RequiresConsul
@ConfigurationProperties(WatchConfiguration.PREFIX)
public class WatchConfiguration extends HttpClientConfiguration {

    public static final String EXPR_CONSUL_WATCH_RETRY_COUNT = "${" + WatchConfiguration.PREFIX + ".retry-count:3}";
    public static final String EXPR_CONSUL_WATCH_RETRY_DELAY = "${" + WatchConfiguration.PREFIX + ".retry-delay:1s}";

    /**
     * The default block timeout in minutes.
     */
    @SuppressWarnings("WeakerAccess")
    public static final long DEFAULT_BLOCK_TIMEOUT_MINUTES = 10;

    /**
     * The prefix to use for all Consul settings.
     */
    public static final String PREFIX = "consul.watch";

    private Duration readTimeout = Duration.ofMinutes(DEFAULT_BLOCK_TIMEOUT_MINUTES);

    private final ConsulConfiguration consulConfiguration;

    public WatchConfiguration(final ConsulConfiguration consulConfiguration) {
        super(consulConfiguration);
        this.consulConfiguration = consulConfiguration;
    }

    @Override
    public ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        return consulConfiguration.getConnectionPoolConfiguration();
    }

    @Override
    public Optional<Duration> getReadTimeout() {
        return Optional.ofNullable(readTimeout);
    }

    @Override
    public void setReadTimeout(@Nullable Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
