package com.frogdevelopment.micronaut.consul.watch.watcher;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.frogdevelopment.micronaut.consul.watch.WatchConfiguration;
import com.frogdevelopment.micronaut.consul.watch.client.KeyValue;
import com.frogdevelopment.micronaut.consul.watch.client.WatchConsulClient;
import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class WatcherTest {

    private static final Logger CLASS_LOGGER = (Logger) LoggerFactory.getLogger(AbstractWatcher.class);

    @Mock
    private WatchConsulClient consulClient;
    @Mock
    private WatchConfiguration watchConfiguration;
    @Mock
    private PropertiesChangeHandler propertiesChangeHandler;
    private final PropertySourceReader propertySourceReader = new YamlPropertySourceLoader();

    private final Base64.Encoder base64Encoder = Base64.getEncoder();

    @Captor
    private ArgumentCaptor<WatchResult> changesCaptor;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach()
    void beforeEach() {
        listAppender = new ListAppender<>();
        listAppender.start();
        CLASS_LOGGER.addAppender(listAppender);
    }

    @AfterEach()
    void afterEach() {
        if (listAppender != null) {
            CLASS_LOGGER.detachAppender(listAppender);
        }
        CLASS_LOGGER.setLevel(Level.INFO);
    }

    @Test
    void should_publish_data_change_yaml() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of("path/to/yaml"), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);

        final var keyValue = new KeyValue(1234, "path/to/yaml", base64Encoder.encodeToString("foo.bar: value".getBytes()));
        final var newKeyValue = new KeyValue(4567, "path/to/yaml", base64Encoder.encodeToString("foo.bar: value_2".getBytes()));

        given(consulClient.watchValues("path/to/yaml", false, null))
                .willReturn(Mono.just(List.of(keyValue))); // init

        given(consulClient.watchValues("path/to/yaml", false, 1234))
                .willReturn(Mono.delay(Duration.ofMillis(200))
                        .thenReturn(List.of(keyValue))) // no change
                .willReturn(Mono.delay(Duration.ofMillis(200))
                        .thenReturn(List.of(newKeyValue))); // change

        given(consulClient.watchValues("path/to/yaml", false, 4567))
                .willReturn(Mono.delay(Duration.ofSeconds(10))
                        .thenReturn(List.of(newKeyValue))); // no change

        given(watchConfiguration.getWatchDelay()).willReturn(Duration.ZERO);

        // when
        watcher.start();

        // then
        Awaitility.with()
                .await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    then(propertiesChangeHandler).should().handleChanges(changesCaptor.capture());
                    final var results = changesCaptor.getAllValues();
                    softAssertions.assertThat(results).hasSize(1);
                    final var previousValue = results.getFirst().previous();
                    softAssertions.assertThat(previousValue)
                            .hasSize(1)
                            .containsEntry("foo.bar", "value");
                    final var nextValue = results.getFirst().next();
                    softAssertions.assertThat(nextValue)
                            .hasSize(1)
                            .containsEntry("foo.bar", "value_2");
                }));
    }

    @Test
    void configuration_should_handle_null_kv() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of("path/to/yaml"), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);

        // when
        final var value = watcher.readValue(null);

        // then
        assertThat(value).isEmpty();
    }

    @Test
    void should_publish_data_change_native() {
        // given
        final var watcher = new NativeWatcher(List.of("path/to/"), consulClient, watchConfiguration, propertiesChangeHandler);

        final var previousKeyValue1 = new KeyValue(12, "path/to/foo.bar", base64Encoder.encodeToString("value_a".getBytes()));
        final var previousKeyValue2 = new KeyValue(34, "path/to/other.key", base64Encoder.encodeToString("value_b".getBytes()));
        final var previousKvs = new ArrayList<>(List.of(previousKeyValue1, previousKeyValue2));

        final var nextKeyValue1 = new KeyValue(56, "path/to/foo.bar", base64Encoder.encodeToString("value_c".getBytes()));
        final var nextKeyValue2 = new KeyValue(78, "path/to/other.key", base64Encoder.encodeToString("value_b".getBytes()));
        final var nextKvs = new ArrayList<>(List.of(nextKeyValue1, nextKeyValue2));
        given(consulClient.watchValues("path/to/", true, null))
                .willReturn(Mono.just(previousKvs)); // init

        given(consulClient.watchValues("path/to/", true, 34))
                .willReturn(Mono.delay(Duration.ofMillis(200))
                        .thenReturn(previousKvs)) // no change
                .willReturn(Mono.delay(Duration.ofMillis(200))
                        .thenReturn(nextKvs)); // change

        given(consulClient.watchValues("path/to/", true, 78))
                .willReturn(Mono.delay(Duration.ofMillis(200))
                        .thenReturn(nextKvs)); // no change

        given(watchConfiguration.getWatchDelay()).willReturn(Duration.ZERO);

        // when
        watcher.start();

        // then
        Awaitility.with()
                .await()
                .atMost(10, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    then(propertiesChangeHandler).should().handleChanges(changesCaptor.capture());
                    final var results = changesCaptor.getAllValues();
                    softAssertions.assertThat(results).hasSize(1);
                    final var previousValue = results.getFirst().previous();
                    softAssertions.assertThat(previousValue)
                            .hasSize(2)
                            .containsEntry("foo.bar", "value_a")
                            .containsEntry("other.key", "value_b");
                    final var nextValue = results.getFirst().next();
                    softAssertions.assertThat(nextValue)
                            .hasSize(2)
                            .containsEntry("foo.bar", "value_c")
                            .containsEntry("other.key", "value_b");
                }));
    }

    @Test
    void native_should_handle_null_kvs() {
        // given
        final var watcher = new NativeWatcher(List.of(), consulClient, watchConfiguration, propertiesChangeHandler);

        // when
        final var value = watcher.readValue(null);

        // then
        assertThat(value).isEmpty();
    }

    @Test
    void should_log_global_error() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of("path/to/global_error"), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);

        final var keyValue = new KeyValue(0, "path/to/global_error", "");
        final var newKeyValue = new KeyValue(1, "path/to/global_error", "incorrect data");
        given(consulClient.watchValues("path/to/global_error", false, null))
                .willReturn(Mono.just(List.of(keyValue)));// init
        given(consulClient.watchValues("path/to/global_error", false, 0))
                .willReturn(Mono.just(List.of(newKeyValue))); // change

        given(watchConfiguration.getWatchDelay()).willReturn(Duration.ZERO);
        CLASS_LOGGER.setLevel(Level.ERROR);

        // when
        watcher.start();

        // then
        Awaitility.with()
                .await()
                .atMost(2, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    then(propertiesChangeHandler).shouldHaveNoInteractions();

                    softAssertions.assertThat(listAppender.list).hasSize(1);
                    final var loggingEvent = listAppender.list.getFirst();
                    softAssertions.assertThat(loggingEvent.getFormattedMessage()).isEqualTo("Watching kvPath=path/to/global_error failed");
                    softAssertions.assertThat(((ThrowableProxy) loggingEvent.getThrowableProxy()).getThrowable())
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("Illegal base64 character 20");
                }));
    }

    @Test
    void should_handle_client_error_NOT_FOUND() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of("path/to/NOT_FOUND"), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);

        final var response = mock(HttpResponse.class);
        given(response.getStatus()).willReturn(HttpStatus.NOT_FOUND);
        final var exception = new HttpClientResponseException("boom", response);
        given(consulClient.watchValues("path/to/NOT_FOUND", false, null)).willReturn(Mono.error(exception));
        given(watchConfiguration.getWatchDelay()).willReturn(Duration.ZERO);

        CLASS_LOGGER.setLevel(Level.TRACE);

        // when
        watcher.start();

        // then
        Awaitility.with()
                .await()
                .atMost(2, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    then(propertiesChangeHandler).shouldHaveNoInteractions();

                    final var logs = listAppender.list.stream().filter(event -> Level.TRACE == event.getLevel()).toList();
                    softAssertions.assertThat(logs).hasSize(1);
                    final var loggingEvent = logs.getFirst();
                    softAssertions.assertThat(loggingEvent.getFormattedMessage()).isEqualTo("No KV found with kvPath=path/to/NOT_FOUND");
                    softAssertions.assertThat(((ThrowableProxy) loggingEvent.getThrowableProxy())).isNull();
                }));
    }

    @Test
    void should_handle_client_error_http_error() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of("path/to/http_error"), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);

        final var response = mock(HttpResponse.class);
        given(response.getStatus()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        final var exception = new HttpClientResponseException("boom", response);
        given(consulClient.watchValues("path/to/http_error", false, null)).willReturn(Mono.error(exception));
        given(watchConfiguration.getWatchDelay()).willReturn(Duration.ZERO);

        // when
        watcher.start();

        // then
        Awaitility.with()
                .await()
                .atMost(2, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    then(propertiesChangeHandler).shouldHaveNoInteractions();

                    final var logs = listAppender.list.stream().filter(event -> Level.ERROR == event.getLevel()).toList();
                    softAssertions.assertThat(logs).hasSize(1);
                    final var loggingEvent = logs.getFirst();
                    softAssertions.assertThat(loggingEvent.getFormattedMessage()).isEqualTo("Watching kvPath=path/to/http_error failed");
                    softAssertions.assertThat(((ThrowableProxy) loggingEvent.getThrowableProxy()).getThrowable()).isEqualTo(exception);
                }));
    }

    @Test
    void should_handle_client_error_timeout() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of("path/to/timeout"), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);

        final var exception = ReadTimeoutException.TIMEOUT_EXCEPTION;
        given(consulClient.watchValues("path/to/timeout", false, null)).willReturn(Mono.error(exception));
        given(watchConfiguration.getWatchDelay()).willReturn(Duration.ofMillis(500));

        CLASS_LOGGER.setLevel(Level.WARN);

        // when
        watcher.start();

        // then
        Awaitility.with()
                .await()
                .atMost(2, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    then(propertiesChangeHandler).shouldHaveNoInteractions();

                    softAssertions.assertThat(listAppender.list)
                            .filteredOn(event -> event.getLevel().equals(Level.WARN))
                            .hasSize(1)
                            .extracting(ILoggingEvent::getFormattedMessage)
                            .contains("Timeout for kvPath=path/to/timeout");
                }));
    }

    @Test
    void should_handle_client_error_other() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of("path/to/error_other"), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);

        final var exception = new RuntimeException("boom");
        given(consulClient.watchValues("path/to/error_other", false, null)).willReturn(Mono.error(exception));
        given(watchConfiguration.getWatchDelay()).willReturn(Duration.ZERO);

        // when
        watcher.start();

        // then
        Awaitility.with()
                .await()
                .atMost(2, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    then(propertiesChangeHandler).shouldHaveNoInteractions();

                    final var logs = listAppender.list.stream().filter(event -> Level.ERROR == event.getLevel()).toList();
                    softAssertions.assertThat(logs).hasSize(1);
                    final var loggingEvent = logs.getFirst();
                    softAssertions.assertThat(loggingEvent.getFormattedMessage()).isEqualTo("Watching kvPath=path/to/error_other failed");
                    softAssertions.assertThat(((ThrowableProxy) loggingEvent.getThrowableProxy()).getThrowable()).isEqualTo(exception);
                }));
    }

    @Test
    void should_throwAnException_when_alreadyStarted() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of(), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);

        // when
        watcher.start();
        final var caughtException = catchException(watcher::start);

        // then
        assertThat(caughtException)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Watcher is already started");
        then(consulClient).shouldHaveNoInteractions();
        then(watchConfiguration).shouldHaveNoInteractions();
        then(propertiesChangeHandler).shouldHaveNoInteractions();
    }

    @Test
    void should_logErrorAndStop_when_exceptionDuringStart() {
        final var watcher = new ConfigurationsWatcher(List.of("path/to/yaml"), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);

        final var exception = new RuntimeException("boom");
        given(consulClient.watchValues("path/to/yaml", false, null))
                .willThrow(exception);

        given(watchConfiguration.getWatchDelay()).willReturn(Duration.ZERO);

        // when
        watcher.start();

        // then
        then(consulClient).shouldHaveNoMoreInteractions();
        then(watchConfiguration).shouldHaveNoMoreInteractions();
        then(propertiesChangeHandler).shouldHaveNoInteractions();

        final var logs = listAppender.list.stream().filter(event -> Level.ERROR == event.getLevel()).toList();
        assertThat(logs).hasSize(1);
        final var loggingEvent = logs.getFirst();
        assertThat(loggingEvent.getFormattedMessage()).isEqualTo("Error watching configurations");
        assertThat(((ThrowableProxy) loggingEvent.getThrowableProxy()).getThrowable()).isEqualTo(exception);
    }

    @Test
    void should_logWarning_when_stoppingNotStarted() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of(), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);
        CLASS_LOGGER.setLevel(Level.WARN);

        // when
        watcher.stop();

        // then
        final var logs = listAppender.list.stream().filter(event -> Level.WARN == event.getLevel()).toList();
        assertThat(logs).hasSize(1);
        final var loggingEvent = logs.getFirst();
        assertThat(loggingEvent.getFormattedMessage()).isEqualTo("You tried to stop an unstarted Watcher");

        then(consulClient).shouldHaveNoInteractions();
        then(watchConfiguration).shouldHaveNoInteractions();
        then(propertiesChangeHandler).shouldHaveNoInteractions();
    }

    @Test
    void should_stopWatcher() {
        // given
        final var watcher = new ConfigurationsWatcher(List.of("path/to/yaml"), consulClient, watchConfiguration, propertiesChangeHandler, propertySourceReader);
        final var keyValue = new KeyValue(1234, "path/to/yaml", base64Encoder.encodeToString("foo.bar: value".getBytes()));

        given(consulClient.watchValues("path/to/yaml", false, null))
                .willReturn(Mono.delay(Duration.ofMillis(200))
                        .thenReturn(List.of(keyValue)));

        given(watchConfiguration.getWatchDelay()).willReturn(Duration.ZERO);

        // when
        watcher.start();
        watcher.stop();

        // then
        Awaitility.with()
                .await()
                .atMost(1, SECONDS)
                .untilAsserted(() -> assertSoftly(softAssertions -> {
                    then(propertiesChangeHandler).shouldHaveNoInteractions();
                }));

    }

}
