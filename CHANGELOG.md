# Changelog

## [Unreleased]

## [v2.0.4] - 2021-02-16

### Features
* Support for udp docker ports

### Bugs
* Fix pruning bug when containers are not alive and updated logs
* Logs updated
* Microservice update container bug fix (Rebuild flag issue)

## [v2.0.3] - 2020-11-16

### Bugs
* Fix bug of pruning live images on agent reboot

## [v2.0.2] - 2020-10-23

### Features
* Added dir /var/log/iofog-microservices on install for microservice logs

### Bugs
* Fixed docker pruning dead loop
* Fixed Gps mode null pointer
* Fixed rollback version issue

## [v2.0.1] - 2020-09-10

### Bugs
* Default available disk threshold updated to 20

## [v2.0.0-beta2] - 2020-04-06

### Features

* Changed remote debugger port to 54322
* Added extra hosts support

## [v2.0.0-beta] - 2020-03-24

### Features

* Skupper integration 
* Agent docker pruning

### Bug fixes

* Stop and delete microservices when deprovision and delete agent respectively
* Enable deletion of other agents microservices
* Fixed microservice move bug
* Fixed deprovisioning and routing issues
* Fixed message routing

## [v1.3.0] - 2020-11-21

### Features
* Volume mapping types
* Added extra hosts support

### Bugs

* Create bound directories if do not exist
* Fixed incorrect content type error on cli commands
* Fixed NullPointer exception on microservice status
* Fixed the issue with microservice status
    
[Unreleased]: https://github.com/eclipse-iofog/agent/compare/v2.0.4..HEAD
[v2.0.4]: https://github.com/eclipse-iofog/agent/compare/v2.0.3..v2.0.4
[v2.0.3]: https://github.com/eclipse-iofog/agent/compare/v2.0.2..v2.0.3
[v2.0.2]: https://github.com/eclipse-iofog/agent/compare/v2.0.1..v2.0.2
[v2.0.1]: https://github.com/eclipse-iofog/agent/compare/v2.0.0-beta2..v2.0.1
[v2.0.0-beta2]: https://github.com/eclipse-iofog/agent/compare/v2.0.0-beta..v2.0.0-beta2
[v2.0.0-beta]: https://github.com/eclipse-iofog/agent/compare/v1.3.0..v2.0.0-beta
[v1.3.0]: https://github.com/eclipse-iofog/agent/tree/v1.3.0
