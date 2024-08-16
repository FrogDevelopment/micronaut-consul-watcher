package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.SneakyThrows;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(contextBuilder = YamlConsulKVWatcherIntegrationTest.CustomContextBuilder.class)
@Property(name = "consul.client.config.format", value = "yaml")
class YamlConsulKVWatcherIntegrationTest extends BaseConsulKVWatcherIntegrationTest {

    public static class CustomContextBuilder extends DefaultApplicationContextBuilder {

        public CustomContextBuilder() {
            doUpdateConsul("foo", "bar");
        }
    }

    @Language("YAML")
    private static final String APPLICATION_YAML = """
                                        my:
                                          key:
                                            to_be_updated: %s
                                        
                                        an.other.property: %s""";

    @Override
    protected void updateConsul(String foo, String bar) {
        doUpdateConsul(foo, bar);
    }

    @SneakyThrows
    private static void doUpdateConsul(String foo, String bar) {
        consulKvPut(ROOT + "application", String.format(APPLICATION_YAML, foo, bar));
    }

    @Override
    protected void assertInstanceOfWatcher(ConsulKVWatcher consulKVWatcher) {
        assertThat(consulKVWatcher).isExactlyInstanceOf(YamlConsulKVWatcher.class);
    }

}
