@Configuration
@RequiresConsul
@Requires(property = WatchConfiguration.PREFIX + "disabled", notEquals = "true", defaultValue = "false")
package com.frogdevelopment.micronaut.consul.watch;

import io.micronaut.context.annotation.Configuration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.discovery.consul.condition.RequiresConsul;
