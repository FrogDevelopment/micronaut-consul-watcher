package com.frogdevelopment.micronaut.consul.watch.client;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.micronaut.http.annotation.FilterMatcher;

@FilterMatcher
@Documented
@Retention(RUNTIME)
@Target({METHOD, TYPE})
public @interface BlockedQuery {
}
