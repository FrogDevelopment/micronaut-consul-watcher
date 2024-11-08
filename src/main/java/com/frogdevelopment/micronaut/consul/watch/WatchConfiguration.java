package com.frogdevelopment.micronaut.consul.watch;

import lombok.Data;

import java.time.Duration;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * @author benoit.legall
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(WatchConfiguration.PREFIX)
public class WatchConfiguration {

    public static final String PREFIX = "consul.watch";

    /**
     * Initial Delay before starting the polling task. Default to 5ms
     */
    private final Duration initialDelay = Duration.ofMillis(5);
    /**
     * Period between each configuration poll. Default to 30s // fixme find better default value
     */
    private final Duration period = Duration.ofSeconds(30);
}
