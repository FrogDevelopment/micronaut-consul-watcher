package com.frogdevelopment.micronaut.consul.watch;

import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frogdevelopment.micronaut.consul.watch.watcher.Watcher;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class WatchTriggerTest {

    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private Watcher watcher;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    private ListAppender<ILoggingEvent> listAppender;

//    @BeforeEach()
//    void beforeEach() {
//        listAppender = new ListAppender<>();
//        listAppender.start();
//        watcherLogger.addAppender(listAppender);
//    }
//
//    @AfterEach
//    void afterEach() {
//        listAppender.stop();
//        watcherLogger.detachAppender(listAppender);
//        listAppender = null;
//    }

//    @Test
//    void should_throwAnException_when_alreadyStarted() {
//        // given
//        given(taskScheduler.scheduleWithFixedDelay(any(), any(), any())).willAnswer(invocation -> scheduledFuture);
//
//        // when
//        watchTrigger.start(null);
//        final var caught = Assertions.catchThrowable(() -> watchTrigger.start(null));
//
//        // then
//        assertThat(caught)
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessage("Watcher scheduler already started");
//    }
//
//    @Test
//    void should_start_all_watchers() {
//        // given
//        given(taskScheduler.scheduleWithFixedDelay(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
//        watchTrigger.start(null);
//        final var runnable = runnableCaptor.getValue();
//
//        // when
//        runnable.run();
//
//        // then
//        then(watcher).should().start();
//    }
//
//    @Test
//    void should_stopScheduler() {
//        // given
//        given(taskScheduler.scheduleWithFixedDelay(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
//        watchTrigger.start(null);
//        given(scheduledFuture.cancel(true)).willReturn(true);
//
//        // when
//        watchTrigger.stop(null);
//
//        // then
//        then(taskScheduler).shouldHaveNoMoreInteractions();
//    }
//
//    @Test
//    void should_logWarning_when_stoppingAnUnstartedWatcher() {
//        // when
//        watchTrigger.stop(null);
//
//        // then
//        then(taskScheduler).shouldHaveNoMoreInteractions();
//        then(scheduledFuture).shouldHaveNoInteractions();
//    }
//
//    @Test
//    void should_logWarning_when_alreadyStopped() {
//        // given
//        given(taskScheduler.scheduleWithFixedDelay(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
//        watchTrigger.start(null);
//        given(scheduledFuture.cancel(true)).willReturn(true);
//        watchTrigger.stop(null);
//
//        // when
//        watchTrigger.stop(null);
//
//        // then
//        then(taskScheduler).shouldHaveNoMoreInteractions();
//
//        assertThat(listAppender.list)
//                .filteredOn(iLoggingEvent -> Level.WARN.equals(iLoggingEvent.getLevel()))
//                .hasSize(1)
//                .first()
//                .extracting(ILoggingEvent::getFormattedMessage)
//                .isEqualTo("Watcher scheduler is already stopped");
//    }
//
//    @Test
//    void should_logWarning_when_schedulerCanNotBeStopped() {
//        // given
//        given(taskScheduler.scheduleWithFixedDelay(any(), any(), runnableCaptor.capture())).willAnswer(invocation -> scheduledFuture);
//        watchTrigger.start(null);
//        given(scheduledFuture.cancel(true)).willReturn(false);
//
//        // when
//        watchTrigger.stop(null);
//
//        // then
//        then(taskScheduler).shouldHaveNoMoreInteractions();
//
//        assertThat(listAppender.list)
//                .filteredOn(iLoggingEvent -> Level.WARN.equals(iLoggingEvent.getLevel()))
//                .hasSize(1)
//                .first()
//                .extracting(ILoggingEvent::getFormattedMessage)
//                .isEqualTo("Watcher scheduler could not be stopped");
//    }
}
