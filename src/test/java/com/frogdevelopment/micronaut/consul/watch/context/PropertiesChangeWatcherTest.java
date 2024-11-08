package com.frogdevelopment.micronaut.consul.watch.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;

@ExtendWith(MockitoExtension.class)
class PropertiesChangeWatcherTest {

    @InjectMocks
    private PropertiesChangeHandler propertiesChangeHandler;

    @Mock
    private Environment environment;
    @Mock
    private ApplicationEventPublisher<RefreshEvent> eventPublisher;

    @Captor
    ArgumentCaptor<PropertySource> propertySourceArgumentCaptor;
    @Captor
    ArgumentCaptor<RefreshEvent> refreshEventArgumentCaptor;

    @Test
    void should_updateContext_and_publishChanges() {
        // given
        final Map<String, Object> previous = new HashMap<>();
        previous.put("key_1", "value_1");
        previous.put("key_2", null);
        previous.put("key_3", "null");
        previous.put("key_4", null);

        final Map<String, Object> next = new HashMap<>();
        next.put("key_1", "value_2");
        next.put("key_2", "null");
        next.put("key_3", null);
        next.put("key_4", null);

        final var propertySources = new ArrayList<PropertySource>();
        propertySources.add(PropertySource.of("consul-consul-watcher[test]", previous, 99));
        propertySources.add(PropertySource.of("consul-application", Map.of("key_a", "value_a"), 66));
        given(environment.getPropertySources()).willReturn(propertySources);

        // when
        propertiesChangeHandler.handleChanges("config/consul-watcher,test", previous, next);

        // then
        then(environment).should(times(2)).addPropertySource(propertySourceArgumentCaptor.capture());
        var propertySource = propertySourceArgumentCaptor.getAllValues()
                .stream()
                .filter(ps -> ps.getName().equals("consul-consul-watcher[test]"))
                .findFirst();
        assertThat(propertySource).hasValueSatisfying(ps -> {
            assertThat(ps.get("key_1")).isEqualTo("value_2");
            assertThat(ps.get("key_2")).isEqualTo("null");
            assertThat(ps.get("key_3")).isNull();
            assertThat(ps.get("key_4")).isNull();
        });

        then(eventPublisher).should().publishEvent(refreshEventArgumentCaptor.capture());
        var refreshEvent = refreshEventArgumentCaptor.getValue();
        assertThat(refreshEvent.getSource()).containsEntry("key_1", "value_1");
    }

    @Test
    void should_doNothing_when_noDifference() {
        // given
        final Map<String, Object> previous = Map.of("key_1", "value_1");
        final Map<String, Object> next = Map.of("key_1", "value_1");

        // when
        propertiesChangeHandler.handleChanges("config/application", previous, next);

        // then
        then(environment).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void should_notUpdatePropertySourceNorPublishChanges_when_incompatibleTypeOnDifferences() {
        // given
        final Map<String, Object> previous = Map.of("key_int", 1);
        final Map<String, Object> next = Map.of("key_int", "1");

        // when
        propertiesChangeHandler.handleChanges("config/application", previous, next);

        // then
        then(environment).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void should_keepGoing_when_typeClassAreDifferentButNumbers() {
        // given
        final Map<String, Object> previous = Map.of("key_int", 1);
        final Map<String, Object> next = Map.of("key_int", 1.0);

        Collection<PropertySource> propertySources = new ArrayList<>();
        propertySources.add(PropertySource.of("consul-application", Map.of("key_int", 1), 66));
        given(environment.getPropertySources()).willReturn(propertySources);

        // when
        propertiesChangeHandler.handleChanges("config/application", previous, next);

        // then
        then(environment).should().addPropertySource(propertySourceArgumentCaptor.capture());
        var propertySource = propertySourceArgumentCaptor.getValue();
        assertThat(propertySource.get("key_int")).isEqualTo(1.0);

        then(eventPublisher).should().publishEvent(refreshEventArgumentCaptor.capture());
        var refreshEvent = refreshEventArgumentCaptor.getValue();
        assertThat(refreshEvent.getSource()).containsEntry("key_int", 1);
    }

}
