package com.frogdevelopment.micronaut.consul.watch.client;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frogdevelopment.micronaut.consul.watch.WatchConfiguration;

import io.micronaut.http.MutableHttpParameters;
import io.micronaut.http.MutableHttpRequest;

@ExtendWith(MockitoExtension.class)
class BlockedQueryClientFilterTest {

    @InjectMocks
    private BlockedQueryClientFilter blockedQueryClientFilter;

    @Mock
    private WatchConfiguration watchConfiguration;
    @Mock
    private MutableHttpRequest<?> request;
    @Mock
    private MutableHttpParameters parameters;

    @Test
    void should_doNothing_when_indexIsNotPresent() {
        // given
        given(request.getParameters()).willReturn(parameters);
        given(parameters.contains("index")).willReturn(false);

        // when
        blockedQueryClientFilter.filter(request);

        // then
        then(parameters).shouldHaveNoMoreInteractions();
        then(watchConfiguration).shouldHaveNoInteractions();
    }

    @Test
    void should_addWait_when_indexIsPresent() {
        // given
        given(request.getParameters()).willReturn(parameters);
        given(parameters.contains("index")).willReturn(true);
        given(watchConfiguration.getWaitTimeout()).willReturn("666s");

        // when
        blockedQueryClientFilter.filter(request);

        // then
        then(parameters).should().add("wait", "666s");
    }


}
