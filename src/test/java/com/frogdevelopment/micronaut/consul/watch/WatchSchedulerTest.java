package com.frogdevelopment.micronaut.consul.watch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;

import io.micronaut.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class WatchSchedulerTest {

    private WatchScheduler watchScheduler;

    @Mock
    private TaskScheduler taskScheduler;

    private final WatchConfiguration watchConfiguration = new WatchConfiguration();

    @Mock
    private Watcher watch_1;
    @Mock
    private Watcher watch_2;
    @Mock
    private Watcher watch_3;
    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @BeforeEach()
    void beforeEach() {
        final var watchers = List.of(watch_1, watch_2, watch_3);
        watchScheduler = new WatchScheduler(taskScheduler, watchConfiguration, watchers);
    }

    @Test
    void should_throwAnException_when_alreadyStarted() {
        // given
        given(taskScheduler.scheduleAtFixedRate(any(), any(), any())).willAnswer(invocation -> scheduledFuture);

        // when
        watchScheduler.start(null);
        final var caught = Assertions.catchThrowable(() -> watchScheduler.start(null));

        // then
        assertThat(caught)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Watcher scheduler already started");
    }

    @Test
    void should_start_all_watchers() {
        // given
        given(taskScheduler.scheduleAtFixedRate(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
        watchScheduler.start(null);
        final var runnable = runnableCaptor.getValue();

        // when
        runnable.run();

        // then
        then(watch_1).should().watchKvPath();
        then(watch_2).should().watchKvPath();
        then(watch_3).should().watchKvPath();
    }

    @Test
    void should_stopScheduler() {
        // given
        given(taskScheduler.scheduleAtFixedRate(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
        watchScheduler.start(null);
        given(scheduledFuture.cancel(true)).willReturn(true);

        // when
        watchScheduler.stop(null);

        // then
        then(taskScheduler).shouldHaveNoMoreInteractions();
    }

    @Test
    void should_logWarning_when_stoppingAnUnstartedWatcher() {
        // when
        watchScheduler.stop(null);

        // then
        then(taskScheduler).shouldHaveNoMoreInteractions();
        then(scheduledFuture).shouldHaveNoInteractions();
    }

    @Test
    void should_logWarning_when_alreadyStopped() {
        // given
        given(taskScheduler.scheduleAtFixedRate(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
        watchScheduler.start(null);
        given(scheduledFuture.cancel(true)).willReturn(true);
        watchScheduler.stop(null);

        // when
        watchScheduler.stop(null);

        // then
        then(taskScheduler).shouldHaveNoMoreInteractions();
        // fixme
    }

    @Test
    void should_logWarning_when_schedulerCanNotBeStopped() {
        // given
        given(taskScheduler.scheduleAtFixedRate(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
        watchScheduler.start(null);
        given(scheduledFuture.cancel(true)).willReturn(false);

        // when
        watchScheduler.stop(null);

        // then
        then(taskScheduler).shouldHaveNoMoreInteractions();
        // fixme
    }
}
