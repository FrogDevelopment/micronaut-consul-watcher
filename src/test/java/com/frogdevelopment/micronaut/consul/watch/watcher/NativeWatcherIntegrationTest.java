package com.frogdevelopment.micronaut.consul.watch.watcher;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@Slf4j
@Property(name = "consul.client.config.format", value = "native")
@MicronautTest(contextBuilder = NativeWatcherIntegrationTest.CustomContextBuilder.class)
class NativeWatcherIntegrationTest extends BaseWatcherIntegrationTest {

    public static class CustomContextBuilder extends DefaultApplicationContextBuilder {

        public CustomContextBuilder() {
            log.info("Initializing consul...");
            doUpdateConsul("foo", "bar");
        }
    }

    private static final String APPLICATION_PROPERTY_FOO = "my.key.to_be_updated";
    private static final String APPLICATION_PROPERTY_BAR = "an.other.property";

    @Override
    protected void updateConsul(final String foo, final String bar) {
        log.info("Updating consul...");
        doUpdateConsul(foo, bar);
    }

    @SneakyThrows
    private static void doUpdateConsul(final String foo, final String bar) {
        consulKvPut(ROOT + "application/" + APPLICATION_PROPERTY_FOO, foo);
        consulKvPut(ROOT + "application/" + APPLICATION_PROPERTY_BAR, bar);
        consulKvPut(ROOT + "application,test/something.else", "test");
    }

}


