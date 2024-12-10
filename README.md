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
    block-timeout: 10m # Sets the read timeout. Default value (10 minutes).
```
