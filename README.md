![Build](https://github.com/FrogDevelopment/micronaut-consul-watcher/actions/workflows/build.yml/badge.svg)
![GitHub Release](https://img.shields.io/github/v/release/FrogDevelopment/micronaut-consul-watcher)
[![Release date](https://img.shields.io/github/release-date/FrogDevelopment/micronaut-consul-watcher)](https://packagist.org/packages/FrogDevelopment/micronaut-consul-watcher)  
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=FrogDevelopment_micronaut-consul-watcher&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=FrogDevelopment_micronaut-consul-watcher)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=FrogDevelopment_micronaut-consul-watcher&metric=bugs)](https://sonarcloud.io/summary/new_code?id=FrogDevelopment_micronaut-consul-watcher)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=FrogDevelopment_micronaut-consul-watcher&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=FrogDevelopment_micronaut-consul-watcher)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=FrogDevelopment_micronaut-consul-watcher&metric=coverage)](https://sonarcloud.io/summary/new_code?id=FrogDevelopment_micronaut-consul-watcher)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=FrogDevelopment_micronaut-consul-watcher&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=FrogDevelopment_micronaut-consul-watcher)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=FrogDevelopment_micronaut-consul-watcher&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=FrogDevelopment_micronaut-consul-watcher)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=FrogDevelopment_micronaut-consul-watcher&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=FrogDevelopment_micronaut-consul-watcher)

# Micronaut Consul Watcher

The watcher calls the [KV Store API](https://developer.hashicorp.com/consul/api-docs/kv) to watch all keys used for the distributed configurations,
using [Blocking Queries](https://developer.hashicorp.com/consul/api-docs/features/blocking)
to wait for any changes made on those keys.  
If no change occurred during the `max-wait-duration`, the query will be re-executed after the `delay-duration`.  
When a change is detected in a KV used for configurations,
the corresponding `PropertySource` will be updated and a `RefreshEvent` published.  

See [Micronaut > Refreshable Scope](https://docs.micronaut.io/latest/guide/index.html#refreshable) for more details

## Configuration

The watcher can be configured using

```yaml
micronaut:
  application:
    name: hello-world
  config-client:
    enabled: true

consul:
  client:
    defaultZone: "${CONSUL_HOST:localhost}:${CONSUL_PORT:8500}"
    config:
      format: YAML
      path: /config

  watch:
    disabled: false # to disable the watcher, during test for instance
    max-wait-duration: 10m # Sets the maximum duration for the blocking request
    delay-duration: 50ms # Sets the watch delay before each call to avoid flooding
```

Formats supported are

- `NATIVE`
- `JSON`
- `PROPERTIES`
- `YAML`

Read [Micronaut > Distributed Configuration > HashiCorp Consul Support](https://docs.micronaut.io/latest/guide/index.html#distributedConfigurationConsul)
for more details.
