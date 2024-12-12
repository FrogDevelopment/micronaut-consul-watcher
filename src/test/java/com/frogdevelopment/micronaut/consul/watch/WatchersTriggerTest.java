package com.frogdevelopment.micronaut.consul.watch;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;

import io.micronaut.context.BeanContext;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;

@ExtendWith(MockitoExtension.class)
class WatchersTriggerTest {

    @InjectMocks
    private WatchersTrigger watchersTrigger;

    @Mock
    private BeanContext beanContext;
    @Mock
    private Watcher watcher;

    @Test
    void should_startWatcher() {
        // given
        final var startupEvent = new StartupEvent(beanContext);
        given(beanContext.getBean(Watcher.class)).willReturn(watcher);

        // when
        watchersTrigger.onStart(startupEvent);

        // then
        then(watcher).should().start();
        then(watcher).shouldHaveNoMoreInteractions();
    }

    @Test
    void should_stopWatcher() {
        // given
        final var shutdownEvent = new ShutdownEvent(beanContext);
        given(beanContext.getBean(Watcher.class)).willReturn(watcher);

        // when
        watchersTrigger.onShutdown(shutdownEvent);

        // then
        then(watcher).should().stop();
        then(watcher).shouldHaveNoMoreInteractions();
    }

}
