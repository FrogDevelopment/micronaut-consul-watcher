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
    initial-delay: 1ms # duration before running 1st poll
    period: 30s # duration between each poll
```
