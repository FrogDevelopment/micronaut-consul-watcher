package com.frogdevelopment.micronaut.consul.watch.watcher;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.intellij.lang.annotations.Language;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@Slf4j
@Property(name = "consul.client.config.format", value = "yaml")
@MicronautTest(contextBuilder = YamlWatcherIntegrationTest.CustomContextBuilder.class)
class YamlWatcherIntegrationTest extends BaseWatcherIntegrationTest {

    public static class CustomContextBuilder extends DefaultApplicationContextBuilder {

        public CustomContextBuilder() {
            log.info("Initializing consul...");
            doUpdateConsul("foo", "bar");
        }
    }

    @Language("YAML")
    private static final String APPLICATION_YAML = """
            my:
              key:
                to_be_updated: %s
            
            an.other.property: %s""";

    @Language("YAML")
    private static final String TEST_YAML = """
            something:
              else: test""";

    @Override
    protected void updateConsul(final String foo, final String bar) {
        log.info("Updating consul...");
        doUpdateConsul(foo, bar);
    }

    @SneakyThrows
    private static void doUpdateConsul(final String foo, final String bar) {
        consulKvPut(ROOT + "application", String.format(APPLICATION_YAML, foo, bar));
        consulKvPut(ROOT + "application,test", TEST_YAML);
    }

}


