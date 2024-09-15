package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.SneakyThrows;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(contextBuilder = NativeConsulKVWatcherIntegrationTest.CustomContextBuilder.class)
@Property(name = "consul.client.config.format", value = "native")
class NativeConsulKVWatcherIntegrationTest extends BaseConsulKVWatcherIntegrationTest {

    public static class CustomContextBuilder extends DefaultApplicationContextBuilder {

        public CustomContextBuilder() {
            doUpdateConsul("foo", "bar");
        }
    }

    private static final String APPLICATION_PROPERTY_FOO = "my.key.to_be_updated";
    private static final String APPLICATION_PROPERTY_BAR = "an.other.property";

    @Override
    protected void updateConsul(String foo, String bar) {
        doUpdateConsul(foo, bar);
    }

    @SneakyThrows
    protected static void doUpdateConsul(String foo, String bar) {
        consulKvPut(ROOT + "application/" + APPLICATION_PROPERTY_FOO, foo);
        consulKvPut(ROOT + "application/" + APPLICATION_PROPERTY_BAR, bar);
    }

    @Override
    protected void assertInstanceOfWatcher(ConsulKVWatcher consulKVWatcher) {
        assertThat(consulKVWatcher).isExactlyInstanceOf(NativeConsulKVWatcher.class);
    }

}
