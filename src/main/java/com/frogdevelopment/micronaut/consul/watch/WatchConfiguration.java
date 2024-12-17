package com.frogdevelopment.micronaut.consul.watch;

import java.time.Duration;
import java.util.Optional;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.discovery.consul.condition.RequiresConsul;
import io.micronaut.http.client.HttpClientConfiguration;

@RequiresConsul
@ConfigurationProperties(WatchConfiguration.PREFIX)
public class WatchConfiguration extends HttpClientConfiguration {

    /**
     * The default wait timeout in minutes.
     */
    public static final String DEFAULT_WAIT_TIMEOUT_MINUTES = "10m";

    /**
     * The default watch delay in milliseconds.
     */
    public static final long DEFAULT_WATCH_DELAY_MILLISECONDS = 500;

    /**
     * The prefix to use for all Consul settings.
     */
    public static final String PREFIX = "consul.watch";

    private String waitTimeout = DEFAULT_WAIT_TIMEOUT_MINUTES;
    private Duration watchDelay = Duration.ofSeconds(DEFAULT_WATCH_DELAY_MILLISECONDS);
    private Duration readTimeout = null;

    private final ConsulConfiguration consulConfiguration;
    private final ConversionService conversionService;

    /**
     * Default constructor
     *
     * @param consulConfiguration {@link ConsulConfiguration} use as base.
     * @param conversionService   Use to calculate the {@link #readTimeout} from the {@link #waitTimeout}.
     * @see ConsulConfiguration
     */
    public WatchConfiguration(final ConsulConfiguration consulConfiguration,
                              final ConversionService conversionService) {
        super(consulConfiguration);
        this.consulConfiguration = consulConfiguration;
        this.conversionService = conversionService;
    }

    @Override
    public ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        return consulConfiguration.getConnectionPoolConfiguration();
    }

    /**
     * @return The read timeout, depending on the {@link #waitTimeout} value.
     */
    @Override
    public Optional<Duration> getReadTimeout() {
        if (this.readTimeout == null) {
            this.readTimeout = calculateReadTimeout();
        }
        return Optional.of(this.readTimeout);
    }

    private Duration calculateReadTimeout() {
        final var waitValue = Optional.ofNullable(getWaitTimeout())
                .orElse(DEFAULT_WAIT_TIMEOUT_MINUTES);

        final var duration = conversionService.convertRequired(waitValue, Duration.class);
        // to have the client timeout greater than the wait of the Blocked Query
        return duration.plusMillis(duration.toMillis() / 16);
    }

    /**
     * @return The wait timeout. Defaults to {@value DEFAULT_WAIT_TIMEOUT_MINUTES}.
     */
    public String getWaitTimeout() {
        return this.waitTimeout;
    }

    /**
     * Specify the maximum duration for the blocking request. Default value ({@value #DEFAULT_WAIT_TIMEOUT_MINUTES}).
     *
     * @param waitTimeout The wait timeout
     */
    public void setWaitTimeout(@Nullable final String waitTimeout) {
        this.waitTimeout = waitTimeout;
        this.readTimeout = calculateReadTimeout();
    }

    /**
     * @return The watch delay. Defaults to {@value DEFAULT_WATCH_DELAY_MILLISECONDS} milliseconds.
     */
    public Duration getWatchDelay() {
        return this.watchDelay;
    }

    /**
     * Sets the watch delay before each call to avoid flooding. Default value ({@value #DEFAULT_WATCH_DELAY_MILLISECONDS} milliseconds).
     *
     * @param watchDelay The watch delay
     */
    public void setWatchDelay(final Duration watchDelay) {
        this.watchDelay = watchDelay;
    }
}
