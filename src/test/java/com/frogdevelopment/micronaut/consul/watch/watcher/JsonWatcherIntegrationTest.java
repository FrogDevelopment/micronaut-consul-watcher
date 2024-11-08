package com.frogdevelopment.micronaut.consul.watch.watcher;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.intellij.lang.annotations.Language;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@Slf4j
@Property(name = "consul.client.config.format", value = "json")
@MicronautTest(contextBuilder = JsonWatcherIntegrationTest.CustomContextBuilder.class)
class JsonWatcherIntegrationTest extends BaseWatcherIntegrationTest {

    public static class CustomContextBuilder extends DefaultApplicationContextBuilder {

        public CustomContextBuilder() {
            log.info("Initializing consul...");
            doUpdateConsul("foo", "bar");
        }
    }

    @Language("JSON")
    private static final String APPLICATION_JSON = """
            {
                "my.key.to_be_updated" : "%s",
                "an.other.property" : "%s"
            }""";

    @Language("JSON")
    private static final String TEST_JSON = """
            {
                "something.else" : "test"
            }""";

    @Override
    protected void updateConsul(final String foo, final String bar) {
        log.info("Updating consul...");
        doUpdateConsul(foo, bar);
    }

    @SneakyThrows
    private static void doUpdateConsul(final String foo, final String bar) {
        consulKvPut(ROOT + "application", String.format(APPLICATION_JSON, foo, bar));
        consulKvPut(ROOT + "application,test", TEST_JSON);
    }
}


