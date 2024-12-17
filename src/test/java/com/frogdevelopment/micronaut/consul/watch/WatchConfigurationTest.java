package com.frogdevelopment.micronaut.consul.watch;

import static com.frogdevelopment.micronaut.consul.watch.WatchConfiguration.DEFAULT_WAIT_TIMEOUT_MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.discovery.consul.ConsulConfiguration;

@ExtendWith(MockitoExtension.class)
class WatchConfigurationTest {

    @InjectMocks
    private WatchConfiguration watchConfiguration;

    @Mock
    private  ConsulConfiguration consulConfiguration;
    @Mock
    private  ConversionService conversionService;

    @Test
    void should_useDefaultWatchTimeout() {
        // given
        given(conversionService.convertRequired(DEFAULT_WAIT_TIMEOUT_MINUTES, Duration.class)).willReturn(Duration.ofMinutes(10));

        // when
        final var readTimeout1 = watchConfiguration.getReadTimeout();
        final var readTimeout2 = watchConfiguration.getReadTimeout();

        // then
        assertThat(readTimeout1)
                .hasValueSatisfying(value -> assertThat(value).hasSeconds(637));
        assertThat(readTimeout2).isEqualTo(readTimeout1);
        then(conversionService).should(times(1)).convertRequired(DEFAULT_WAIT_TIMEOUT_MINUTES, Duration.class);
    }

    @Test
    void should_calculateReadTimeout_when_settingWaitTimeout() {
        // given
        assertThat(getReadTimeoutValue()).isEmpty();
        given(conversionService.convertRequired("16s", Duration.class)).willReturn(Duration.ofSeconds(16));

        // when
        watchConfiguration.setWaitTimeout("16s");

        // then
        assertThat(getReadTimeoutValue()).hasValue(Duration.ofSeconds(17));
    }

    private Optional<Object> getReadTimeoutValue() {
        return ReflectionUtils.getFieldValue(WatchConfiguration.class, "readTimeout", watchConfiguration);
    }

}
