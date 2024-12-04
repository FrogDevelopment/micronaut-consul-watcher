package com.frogdevelopment.micronaut.consul.watch.watcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frogdevelopment.micronaut.consul.watch.client.IndexConsulClient;
import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;

import io.micronaut.context.env.PropertySourceReader;
import io.micronaut.context.env.yaml.YamlPropertySourceLoader;

@ExtendWith(MockitoExtension.class)
class WatcherTest {

    private Watcher watcher;
    @Mock
    private IndexConsulClient consulClient;
    @Mock
    private PropertiesChangeHandler propertiesChangeHandler;
    private final PropertySourceReader propertySourceReader = new YamlPropertySourceLoader();

    private final Base64.Encoder base64Encoder = Base64.getEncoder();

    @Captor
    private ArgumentCaptor<List<WatchResult>> changesCaptor;

//    @Test
//    void should_publish_data_change_yaml() {
//        // given
//        watcher = new ConfigurationsWatcher(List.of("path/to/yaml"), consulClient, propertiesChangeHandler, propertySourceReader);
//
//        final var keyValue = new KeyValue("path/to/yaml", base64Encoder.encodeToString("foo.bar: value".getBytes()));
//        final var newKeyValue = new KeyValue("path/to/yaml", base64Encoder.encodeToString("foo.bar: value_2".getBytes()));
////        given(consulClient.readValues("path/to/yaml"))
////                .willReturn(Mono.just(List.of(keyValue))) // init
////                .willReturn(Mono.just(List.of(keyValue))) // no change
////                .willReturn(Mono.just(List.of(newKeyValue))); // change
//
//        // when
//        watcher.watchKVs(); // init
//        watcher.watchKVs(); // no change
//        watcher.watchKVs(); // change
//
//        // then
//        then(propertiesChangeHandler).should().handleChanges(changesCaptor.capture());
//        final var results = changesCaptor.getValue();
//        assertThat(results).hasSize(1);
//        final var previousValue = results.getFirst().previous();
//        assertThat(previousValue)
//                .hasSize(1)
//                .containsEntry("foo.bar", "value");
//        final var nextValue = results.getFirst().next();
//        assertThat(nextValue)
//                .hasSize(1)
//                .containsEntry("foo.bar", "value_2");
//    }
//
////    @Test
////    void should_publish_data_change_native() {
////        // given
////        watcher = new NativeWatcher(List.of("path/to/"), consulClient, propertiesChangeHandler);
////
////        final var previousKeyValue1 = new KeyValue("path/to/foo.bar", base64Encoder.encodeToString("value_a".getBytes()));
////        final var previousKeyValue2 = new KeyValue("path/to/other.key", base64Encoder.encodeToString("value_b".getBytes()));
////        final var previousKvs = new ArrayList<>(List.of(previousKeyValue1, previousKeyValue2));
////
////        final var nextKeyValue1 = new KeyValue("path/to/foo.bar", base64Encoder.encodeToString("value_c".getBytes()));
////        final var nextKeyValue2 = new KeyValue("path/to/other.key", base64Encoder.encodeToString("value_b".getBytes()));
////        final var nextKvs = new ArrayList<>(List.of(nextKeyValue1, nextKeyValue2));
////        given(consulClient.readValues("path/to/"))
////                .willReturn(Mono.just(previousKvs)) // init
////                .willReturn(Mono.just(previousKvs)) // no change
////                .willReturn(Mono.just(nextKvs)); // change
////
////        // when
////        watcher.watchKVs(); // init
////        watcher.watchKVs(); // no change
////        watcher.watchKVs(); // change
////
////        // then
////        then(propertiesChangeHandler).should().handleChanges(changesCaptor.capture());
////        final var results = changesCaptor.getValue();
////        assertThat(results).hasSize(1);
////        final var previousValue = results.getFirst().previous();
////        assertThat(previousValue)
////                .hasSize(2)
////                .containsEntry("foo.bar", "value_a")
////                .containsEntry("other.key", "value_b");
////        final var nextValue = results.getFirst().next();
////        assertThat(nextValue)
////                .hasSize(2)
////                .containsEntry("foo.bar", "value_c")
////                .containsEntry("other.key", "value_b");
////    }
//
//    @Test
//    void should_log_global_error() {
//        // given
//        watcher = new ConfigurationsWatcher(List.of("path/to/yaml"), consulClient, propertiesChangeHandler, propertySourceReader);
//
//        final var keyValue = new KeyValue("path/to/yaml", "");
//        final var newKeyValue = new KeyValue("path/to/yaml", "incorrect data");
////        given(consulClient.readValues("path/to/yaml"))
////                .willReturn(Mono.just(List.of(keyValue))) // init
////                .willReturn(Mono.just(List.of(keyValue))) // no change
////                .willReturn(Mono.just(List.of(newKeyValue))); // change
//
//        // when
//        watcher.watchKVs(); // init
//        watcher.watchKVs(); // no change
//        watcher.watchKVs(); // change
//
//        // then
//        then(propertiesChangeHandler).shouldHaveNoInteractions();
//    }
//
//    @Test
//    void should_handle_client_error_NOT_FOUND() {
//        // given
//        watcher = new ConfigurationsWatcher(List.of("path/to/yaml"), consulClient, propertiesChangeHandler, propertySourceReader);
//
//        final var response = mock(HttpResponse.class);
//        final var exception = new HttpClientResponseException("boom", response);
////        given(consulClient.readValues("path/to/yaml")).willReturn(Mono.error(exception));
//        given(response.getStatus()).willReturn(HttpStatus.NOT_FOUND);
//
//        // when
//        watcher.watchKVs();
//
//        // then
//        then(propertiesChangeHandler).shouldHaveNoInteractions();
//    }
//
//    @Test
//    void should_handle_client_error_http_error() {
//        // given
//        watcher = new ConfigurationsWatcher(List.of("path/to/yaml"), consulClient, propertiesChangeHandler, propertySourceReader);
//
//        final var response = mock(HttpResponse.class);
//        final var exception = new HttpClientResponseException("boom", response);
////        given(consulClient.readValues("path/to/yaml")).willReturn(Mono.error(exception));
//        given(response.getStatus()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
//
//        // when
//        watcher.watchKVs();
//
//        // then
//        then(propertiesChangeHandler).shouldHaveNoInteractions();
//    }
//
//    @Test
//    void should_handle_client_error_other() {
//        // given
//        watcher = new ConfigurationsWatcher(List.of("path/to/yaml"), consulClient, propertiesChangeHandler, propertySourceReader);
//
//        final var exception = new RuntimeException("boom");
////        given(consulClient.readValues("path/to/yaml")).willReturn(Mono.error(exception));
//
//        // when
//        watcher.watchKVs();
//
//        // then
//        then(propertiesChangeHandler).shouldHaveNoInteractions();
//    }

}
