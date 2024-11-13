package com.frogdevelopment.micronaut.consul.watch;

import lombok.Data;

import java.time.Duration;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * @author LE GALL Benoît
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(WatchConfiguration.PREFIX)
public class WatchConfiguration {

    public static final String PREFIX = "consul.watch";

    /**
     * Initial Delay before starting the polling task. Default to 5ms
     */
    private Duration initialDelay = Duration.ofMillis(5);
    /**
     * Period between each configuration poll. Default to 30s.
     */
    private Duration period = Duration.ofSeconds(30);
}
