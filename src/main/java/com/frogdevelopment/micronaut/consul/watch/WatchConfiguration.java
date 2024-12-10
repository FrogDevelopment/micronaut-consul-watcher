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
    public static final long DEFAULT_BLOCK_TIMEOUT_MINUTES = 10;

    public static final long DEFAULT_WATCH_DELAY_MILLISECONDS = 500;

    /**
     * The prefix to use for all Consul settings.
     */
    public static final String PREFIX = "consul.watch";

    private Duration readTimeout = Duration.ofMinutes(DEFAULT_BLOCK_TIMEOUT_MINUTES);
    private Duration watchDelay = Duration.ofSeconds(DEFAULT_WATCH_DELAY_MILLISECONDS);

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

    public Duration getWatchDelay() {
        return watchDelay;
    }

    /**
     * Sets the watch delay before each call to avoid flooding. Default value ({@value #DEFAULT_WATCH_DELAY_MILLISECONDS} milliseconds).
     *
     * @param watchDelay The read timeout
     */
    public void setWatchDelay(Duration watchDelay) {
        this.watchDelay = watchDelay;
    }
}
