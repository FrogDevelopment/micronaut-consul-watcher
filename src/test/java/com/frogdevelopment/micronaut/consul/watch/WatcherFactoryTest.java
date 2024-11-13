package com.frogdevelopment.micronaut.consul.watch;

import com.frogdevelopment.micronaut.consul.watch.context.PropertiesChangeHandler;
import com.frogdevelopment.micronaut.consul.watch.watcher.ConfigurationsWatcher;
import com.frogdevelopment.micronaut.consul.watch.watcher.NativeWatcher;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format;
import io.micronaut.discovery.consul.ConsulConfiguration;
import io.micronaut.discovery.consul.client.v1.ConsulClient;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format.NATIVE;
import static io.micronaut.discovery.config.ConfigDiscoveryConfiguration.Format.YAML;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WatcherFactoryTest {

    @InjectMocks
    private WatcherFactory watcherFactory;

    @Mock
    private Environment environment;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConsulConfiguration consulConfiguration;
    @Mock
    private ConsulClient consulClient;
    @Mock
    private PropertiesChangeHandler propertiesChangeHandler;

    @Test
    void should_create_required_watchers() {
        // given
        given(consulConfiguration.getServiceId()).willReturn(Optional.of("my_application"));
        given(consulConfiguration.getConfiguration().getPath()).willReturn(Optional.of("path/to/config"));
        given(consulConfiguration.getConfiguration().getFormat()).willReturn(YAML);
        given(environment.getActiveNames()).willReturn(Set.of("cloud", "test"));

        // when
        final var watcher = watcherFactory.createWatcher(consulConfiguration);

        // then
        assertThat(watcher)
                .extracting("kvPaths", as(InstanceOfAssertFactories.list(String.class)))
                .hasSize(6)
                .contains(
                        "path/to/config/application",
                        "path/to/config/application,cloud",
                        "path/to/config/application,test",
                        "path/to/config/my_application",
                        "path/to/config/my_application,cloud",
                        "path/to/config/my_application,test"
                );
    }

    @ParameterizedTest
    @EnumSource(value = Format.class,
                mode = EnumSource.Mode.INCLUDE,
                names = {"FILE"})
    void should_throwException_when_formatNotSupported(final Format format) {
        // given
        given(consulConfiguration.getServiceId()).willReturn(Optional.of("my_application"));
        given(consulConfiguration.getConfiguration().getPath()).willReturn(Optional.of("path/to/config"));
        given(consulConfiguration.getConfiguration().getFormat()).willReturn(format);

        // when
        final var caught = catchThrowableOfType(ConfigurationException.class, () -> watcherFactory.createWatcher(consulConfiguration));

        // then
        assertThat(caught).isNotNull();
    }

    @Test
    void should_watch_native() {
        // given
        given(consulConfiguration.getServiceId()).willReturn(Optional.of("my_application"));
        given(consulConfiguration.getConfiguration().getPath()).willReturn(Optional.of("path/to/config"));
        given(consulConfiguration.getConfiguration().getFormat()).willReturn(NATIVE);

        // when
        final var watcher = watcherFactory.createWatcher(consulConfiguration);

        // then
        assertThat(watcher).isInstanceOf(NativeWatcher.class);
        assertThat(watcher)
                .extracting("kvPaths", as(InstanceOfAssertFactories.list(String.class)))
                .hasSize(2)
                .contains(
                        "path/to/config/application/",
                        "path/to/config/my_application/"
                );
    }

    @ParameterizedTest
    @EnumSource(value = Format.class,
                mode = EnumSource.Mode.INCLUDE,
                names = {"JSON", "YAML", "PROPERTIES"})
    void should_watch_configurations(final Format format) {
        // given
        given(consulConfiguration.getServiceId()).willReturn(Optional.of("my_application"));
        given(consulConfiguration.getConfiguration().getPath()).willReturn(Optional.of("path/to/config"));
        given(consulConfiguration.getConfiguration().getFormat()).willReturn(format);

        // when
        final var watcher = watcherFactory.createWatcher(consulConfiguration);

        // then
        assertThat(watcher).isInstanceOf(ConfigurationsWatcher.class);
        assertThat(watcher)
                .extracting("kvPaths", as(InstanceOfAssertFactories.list(String.class)))
                .hasSize(2)
                .contains(
                        "path/to/config/application",
                        "path/to/config/my_application"
                );
    }

}
