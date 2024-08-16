package com.frogdevelopment.micronaut.consul.watcher;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.runtime.context.scope.Refreshable;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Testcontainers
@Tag("integrationTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseConsulKVWatcherIntegrationTest implements TestPropertyProvider {

    protected static final String ROOT = "test/";

    @Inject
    private BeanContext beanContext;

    @Inject
    private ConsulKVWatcher consulKVWatcher;

    @Container
    protected static final ConsulContainer CONSUL_CONTAINER = new ConsulContainer("hashicorp/consul:1.18.1");

    @Override
    public @NonNull Map<String, String> getProperties() {
        final var consulHost = CONSUL_CONTAINER.getHost();
        final var consulPort = CONSUL_CONTAINER.getMappedPort(8500);
        return Map.of(
                "consul.client.registration.enabled", "false",
                "micronaut.config-client.enabled", "true",
                "consul.client.host", consulHost,
                "consul.client.port", String.valueOf(consulPort),
                "consul.client.config.path", "test",
                "consul.watcher.enabled", "true"
        );
    }

    @SneakyThrows
    protected static void consulKvPut(String key, String data) {
        CONSUL_CONTAINER.execInContainer("consul", "kv", "put", key, data);
    }

    protected abstract void updateConsul(String foo, String bar) throws ExecutionException, InterruptedException;

    protected abstract void assertInstanceOfWatcher(ConsulKVWatcher consulKVWatcher);

    @BeforeEach
    void setUp() {
        consulKVWatcher.start();
    }

    @AfterEach
    void cleanUp() {
        consulKVWatcher.stop();
        CONSUL_CONTAINER.withConsulCommand("kv delete " + ROOT);
    }

    @Test
    void should_refresh_only_updated_property() throws ExecutionException, InterruptedException {
        // given
        assertInstanceOfWatcher(consulKVWatcher);

        assertSoftly(softAssertions -> {
            var refreshableBean = beanContext.createBean(RefreshableBean.class);
            softAssertions.assertThat(refreshableBean.keyToBeUpdated).isEqualTo("foo");
            softAssertions.assertThat(refreshableBean.otherKey).isEqualTo("bar");
        });

        // when
        var randomFoo = RandomStringUtils.randomAlphanumeric(10);
        updateConsul(randomFoo, "bar");

        // then
        Awaitility.with().pollDelay(1, TimeUnit.SECONDS).await().untilAsserted(() -> assertSoftly(softAssertions -> {
            var refreshedBean = beanContext.getBean(RefreshableBean.class);
            softAssertions.assertThat(refreshedBean.keyToBeUpdated).isEqualTo(randomFoo);
            softAssertions.assertThat(refreshedBean.otherKey).isEqualTo("bar");
        }));
    }

    @Refreshable
    public static class RefreshableBean {

        @Value("${my.key.to_be_updated}")
        public String keyToBeUpdated;

        @Value("${an.other.property}")
        public String otherKey;

    }
}
