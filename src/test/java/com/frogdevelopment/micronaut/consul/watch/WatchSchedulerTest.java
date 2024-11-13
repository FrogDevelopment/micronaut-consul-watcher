package com.frogdevelopment.micronaut.consul.watch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.concurrent.ScheduledFuture;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class WatchSchedulerTest {

    @SuppressWarnings("LoggerInitializedWithForeignClass")
    private final Logger watcherLogger = (Logger) LoggerFactory.getLogger(WatchScheduler.class);

    @InjectMocks
    private WatchScheduler watchScheduler;

    @Mock
    private TaskScheduler taskScheduler;
    @Spy
    private final WatchConfiguration watchConfiguration = new WatchConfiguration();
    @Mock
    private Watcher watcher;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach()
    void beforeEach() {
        listAppender = new ListAppender<>();
        listAppender.start();
        watcherLogger.addAppender(listAppender);
    }

    @AfterEach
    void afterEach() {
        listAppender.stop();
        watcherLogger.detachAppender(listAppender);
        listAppender = null;
    }

    @Test
    void should_throwAnException_when_alreadyStarted() {
        // given
        given(taskScheduler.scheduleWithFixedDelay(any(), any(), any())).willAnswer(invocation -> scheduledFuture);

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
        given(taskScheduler.scheduleWithFixedDelay(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
        watchScheduler.start(null);
        final var runnable = runnableCaptor.getValue();

        // when
        runnable.run();

        // then
        then(watcher).should().watchKVs();
    }

    @Test
    void should_stopScheduler() {
        // given
        given(taskScheduler.scheduleWithFixedDelay(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
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
        given(taskScheduler.scheduleWithFixedDelay(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
        watchScheduler.start(null);
        given(scheduledFuture.cancel(true)).willReturn(true);
        watchScheduler.stop(null);

        // when
        watchScheduler.stop(null);

        // then
        then(taskScheduler).shouldHaveNoMoreInteractions();

        assertThat(listAppender.list)
                .filteredOn(iLoggingEvent -> Level.WARN.equals(iLoggingEvent.getLevel()))
                .hasSize(1)
                .first()
                .extracting(ILoggingEvent::getFormattedMessage)
                .isEqualTo("Watcher scheduler is already stopped");
    }

    @Test
    void should_logWarning_when_schedulerCanNotBeStopped() {
        // given
        given(taskScheduler.scheduleWithFixedDelay(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
        watchScheduler.start(null);
        given(scheduledFuture.cancel(true)).willReturn(false);

        // when
        watchScheduler.stop(null);

        // then
        then(taskScheduler).shouldHaveNoMoreInteractions();

        assertThat(listAppender.list)
                .filteredOn(iLoggingEvent -> Level.WARN.equals(iLoggingEvent.getLevel()))
                .hasSize(1)
                .first()
                .extracting(ILoggingEvent::getFormattedMessage)
                .isEqualTo("Watcher scheduler could not be stopped");
    }
}
