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

Update Micronaut Context and publish a `RefreshEvent` when a change is detected in a KV used for configurations.  
Current format supported are

- `NATIVE`
- `JSON`
- `PROPERTIES`
- `YAML`

The watcher can be configured using

```yaml
consul:
  watch:
    disabled: false # to disable the watcher, during test for instance
    retry-count: 3 # The maximum number of retry attempts 
    retry-delay: 1s # The delay between retry attempts
    read-timeout: 10m # Sets the watch timeout
    watch-delay: 500ms # Sets the watch delay before each call to avoid flooding
```
