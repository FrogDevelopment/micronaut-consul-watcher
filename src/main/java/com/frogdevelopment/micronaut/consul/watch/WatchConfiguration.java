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
     * The prefix to use for all Consul settings.
     */
    public static final String PREFIX = "consul.watch";

    /**
     * The default max wait duration in minutes.
     */
    public static final String DEFAULT_MAX_WAIT_DURATION_MINUTES = "10m";

    /**
     * The default delay duration in milliseconds.
     */
    public static final long DEFAULT_DELAY_DURATION_MILLISECONDS = 50;

    private String maxWaitDuration = DEFAULT_MAX_WAIT_DURATION_MINUTES;
    private Duration delayDuration = Duration.ofSeconds(DEFAULT_DELAY_DURATION_MILLISECONDS);
    private Duration readTimeout = null;

    private final ConsulConfiguration consulConfiguration;
    private final ConversionService conversionService;

    /**
     * Default constructor
     *
     * @param consulConfiguration {@link ConsulConfiguration} use as base.
     * @param conversionService   Use to calculate the {@link #readTimeout} from the {@link #maxWaitDuration}.
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
     * @return The read timeout, depending on the {@link #maxWaitDuration} value.
     */
    @Override
    public Optional<Duration> getReadTimeout() {
        if (this.readTimeout == null) {
            this.readTimeout = calculateReadTimeout();
        }
        return Optional.of(this.readTimeout);
    }

    private Duration calculateReadTimeout() {
        final var waitValue = Optional.ofNullable(getMaxWaitDuration())
                .orElse(DEFAULT_MAX_WAIT_DURATION_MINUTES);

        final var duration = conversionService.convertRequired(waitValue, Duration.class);
        // to have the client timeout greater than the wait of the Blocked Query
        return duration.plusMillis(duration.toMillis() / 16);
    }

    /**
     * @return The max wait duration. Defaults to {@value DEFAULT_MAX_WAIT_DURATION_MINUTES}.
     */
    public String getMaxWaitDuration() {
        return this.maxWaitDuration;
    }

    /**
     * Specify the maximum duration for the blocking request. Default value ({@value #DEFAULT_MAX_WAIT_DURATION_MINUTES}).
     *
     * @param maxWaitDuration The wait timeout
     */
    public void setMaxWaitDuration(@Nullable final String maxWaitDuration) {
        this.maxWaitDuration = maxWaitDuration;
        this.readTimeout = calculateReadTimeout();
    }

    /**
     * @return The delay duration. Defaults to {@value DEFAULT_DELAY_DURATION_MILLISECONDS} milliseconds.
     */
    public Duration getDelayDuration() {
        return this.delayDuration;
    }

    /**
     * Sets the delay before each call to avoid flooding. Default value ({@value #DEFAULT_DELAY_DURATION_MILLISECONDS} milliseconds).
     *
     * @param delayDuration The watch delay
     */
    public void setDelayDuration(final Duration delayDuration) {
        this.delayDuration = delayDuration;
    }
}
