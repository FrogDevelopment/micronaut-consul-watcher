package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.SneakyThrows;
import org.intellij.lang.annotations.Language;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(contextBuilder = JsonConsulKVWatcherIntegrationTest.CustomContextBuilder.class)
@Property(name = "consul.client.config.format", value = "json")
class JsonConsulKVWatcherIntegrationTest extends BaseConsulKVWatcherIntegrationTest {

    public static class CustomContextBuilder extends DefaultApplicationContextBuilder {

        public CustomContextBuilder() {
            doUpdateConsul("foo", "bar");
        }
    }

    @Language("JSON")
    private static final String APPLICATION_JSON = """
                                        {
                                            "my.key.to_be_updated" : "%s",
                                            "an.other.property" : "%s"
                                        }""";

    @Override
    protected void updateConsul(String foo, String bar) {
        doUpdateConsul(foo, bar);
    }

    @SneakyThrows
    private static void doUpdateConsul(String foo, String bar) {
        consulKvPut(ROOT + "application", String.format(APPLICATION_JSON, foo, bar));
    }

    @Override
    protected void assertInstanceOfWatcher(ConsulKVWatcher consulKVWatcher) {
        assertThat(consulKVWatcher).isExactlyInstanceOf(JsonConsulKVWatcher.class);
    }

}
