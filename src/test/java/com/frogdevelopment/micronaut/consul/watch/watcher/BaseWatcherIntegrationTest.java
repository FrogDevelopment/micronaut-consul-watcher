package com.frogdevelopment.micronaut.consul.watch.watcher;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.runtime.context.scope.Refreshable;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.test.support.TestPropertyProvider;

@Slf4j
@Testcontainers
@Tag("integrationTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseWatcherIntegrationTest implements TestPropertyProvider {

    protected static final String ROOT = "test/";

    @Inject
    private BeanContext beanContext;
    @Inject
    private TestEventListener testEventListener;
    @Inject
    private RefreshableBean refreshableBean;
    @Inject
    private RefreshableProperty refreshableProperty;
    @Inject
    private RefreshableInnerProperty refreshableInnerProperty;

    @Container
    protected static final ConsulContainer CONSUL_CONTAINER = new ConsulContainer("hashicorp/consul:1.18.1");

    @Override
    public @NonNull Map<String, String> getProperties() {
        final var consulHost = CONSUL_CONTAINER.getHost();
        final var consulPort = CONSUL_CONTAINER.getMappedPort(8500);
        return Map.of(
                "micronaut.application.name", "consul-watcher",
                "micronaut.config-client.enabled", "true",
                "consul.client.host", consulHost,
                "consul.client.port", String.valueOf(consulPort),
                "consul.client.config.path", "test",
                "consul.watch.disabled", "false",
                "consul.watch.initialDelay", "0",
                "consul.watch.period", "2s"
        );
    }

    @SneakyThrows
    protected static void consulKvPut(final String key, final String data) {
        CONSUL_CONTAINER.execInContainer("consul", "kv", "put", key, data);
    }

    protected abstract void updateConsul(final String foo, final String bar);

    @AfterEach
    void cleanUp() {
        CONSUL_CONTAINER.withConsulCommand("kv delete " + ROOT);
    }

    @Test
    void should_refresh_only_updated_property() {
        // given
        Awaitility.with()
                .await()
                .pollDelay(5, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    softAssertions.assertThat(refreshableProperty.getToBeUpdated()).isEqualTo("foo");
                    softAssertions.assertThat(refreshableInnerProperty.getKey().getToBeUpdated()).isEqualTo("foo");
                    softAssertions.assertThat(refreshableBean.keyToBeUpdated).isEqualTo("foo");
                    softAssertions.assertThat(refreshableBean.otherKey).isEqualTo("bar");
                }));

        // when
        final var randomFoo = RandomStringUtils.secure().nextAlphanumeric(10);
        updateConsul(randomFoo, "bar");

        // then
        Awaitility.with()
                .await()
                .atMost(30, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    softAssertions.assertThat(testEventListener.isEventReceived).as("isEventReceived").isTrue();
                    // refreshableProperty return the new value
                    softAssertions.assertThat(refreshableProperty.getToBeUpdated()).as("refreshableProperty").isEqualTo(randomFoo);
                    softAssertions.assertThat(refreshableInnerProperty.getKey().getToBeUpdated()).as("refreshableInnerProperty").isEqualTo(randomFoo);
                    // while current refreshableBean is not refreshed
                    softAssertions.assertThat(refreshableBean.keyToBeUpdated).as("refreshedBean").isEqualTo("foo");
                    softAssertions.assertThat(refreshableBean.otherKey).as("refreshedBean").isEqualTo("bar");
                    // but if we re-retrieve from the context, it has been recreated
                    final var refreshedBean = beanContext.getBean(RefreshableBean.class);
                    softAssertions.assertThat(refreshedBean.keyToBeUpdated).as("refreshedBean").isEqualTo(randomFoo);
                    softAssertions.assertThat(refreshedBean.otherKey).as("refreshedBean").isEqualTo("bar");
                }));
    }

    @Singleton
    public static class TestEventListener implements ApplicationEventListener<RefreshEvent> {

        private final AtomicBoolean isEventReceived = new AtomicBoolean(false);

        @Override
        public void onApplicationEvent(final RefreshEvent event) {
            log.info("Received refresh event: {}", event);
            isEventReceived.set(true);
        }
    }

    @ConfigurationProperties("my.key")
    public interface RefreshableProperty {

        @NotBlank
        String getToBeUpdated();
    }

    @ConfigurationProperties("my")
    public interface RefreshableInnerProperty {

        @NotNull
        InnerRefreshableProperty getKey();

        @ConfigurationProperties("key")
        interface InnerRefreshableProperty {

            @NotBlank
            String getToBeUpdated();
        }
    }

    @Refreshable
    public static class RefreshableBean {

        @Value("${my.key.to_be_updated}")
        public String keyToBeUpdated;

        @Value("${an.other.property}")
        public String otherKey;

    }
}
