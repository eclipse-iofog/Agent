# Changelog

## [unreleased]

## [v3.0.1] - 16- May 2022
* Declared Agent dependency i.e. java and docker. 

## [v3.0.0] - 09- May 2022
* A new config called TZ (timeZone) which is configurable is added to inject the same timezone as the device in the microservices.
* Removed java debugger.

## [v3.0.0-beta7] -29 Mar 2022
### Bugs
* Fixed the issue with adding extra-host while creating containers.
* Added a retry mechanism for fetching device ip address on creating containers.

## [v3.0.0-beta6] -22 Feb 2022
* Fixed the issue with updating yum package repository

## [v3.0.0-beta5] -17 Feb 2022
### Features
* Removed iofog-agent support for specific distros and Added support for any/any package which handles different distros.
### Bugs
* Fixed issue where microservice container was not spinning again if docker container was killed.
* Fixed the copyright to 2022

## [v3.0.0-beta4] -16 Dec 2021
### Features
* Added support for raspbian/bullseye
### Bugs
* Revert fix for jdk 11 remote debugging

## [v3.0.0-beta3] -1 Dec 2021
### Bugs
* Send error message back to controller on failure to pull docker image
* Fixed issue with remote debugging with jdk 11

## [v3.0.0-beta2] - 27 Oct 2021
#### Bugs
* Reset microservice container error message on successful microservice deployment

## [v3.0.0-beta1] - 31 Aug 2021
### Features
* Added support for Centos 8

## [v3.0.0-alpha2] - 16 July 2021

### Features
* Support trusted CAs for HTTPS to ioFog Controller

## [v3.0.0-alpha1] - 24 March 2021

### Features
* Support for EdgeResources
* Dev mode added for enabling and disabling sentry notification on error
* Log format updated to Json
* Docker pull stats with percentage completion
* Send error back to controller on failure of volume mount on microservice

## [v2.0.7] - 2021-06-30
* Fixed the bug when config.xml file gets truncated and results in agent crash on re-start.
* Fixed the bug of resetting logger on every configuration update. Now logger resets only on log configuration update.

## [v2.0.6] - 2021-04-28

### Bugs
* Removed configurable pruning scheduler

## [v2.0.5] - 2021-02-25

### Bugs
* Fix upgrade version issue

## [v2.0.4] - 2021-02-16

### Features
* Support for udp docker ports

### Bugs
* Fix pruning bug when containers are not alive and updated logs
* Logs updated
* Microservice update container bug fix (Rebuild flag issue)

## [v2.0.3] - 2020-11-24

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

## [v2.0.0] -2020-23-10

### Bug fixes
* Stop and delete microservices when deprovision and delete agent respectively
* Enable deletion of other agents microservices
* Fixed microservice move bug
* Fixed deprovisioning and routing issues
* Fixed message routing

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
* Report microservices lifecycle to Controller
* Showing available disk percentage in `iofog-agent info` output
* Volume mapping types
* Added extra hosts support
### Bug fixes
* Keep previous GPS coordinates when getting new one fails
* Remove microservices when Agent is moved to another ECN

### Bugs

* Create bound directories if do not exist
* Fixed incorrect content type error on cli commands
* Fixed NullPointer exception on microservice status
* Fixed the issue with microservice status

[Unreleased]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-beta3..HEAD
[v3.0.0]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-beta7..v3.0.0
[v3.0.0-beta7]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-beta6..v3.0.0-beta7
[v3.0.0-beta6]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-beta5..v3.0.0-beta6
[v3.0.0-beta5]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-beta4..v3.0.0-beta5
[v3.0.0-beta4]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-beta3..v3.0.0-beta4
[v3.0.0-beta3]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-beta2..v3.0.0-beta3
[v3.0.0-beta2]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-beta1..v3.0.0-beta2
[v3.0.0-beta1]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-alpha2..v3.0.0-beta1
[v3.0.0-alpha2]: https://github.com/eclipse-iofog/agent/compare/v3.0.0-alpha1..v3.0.0-alpha2
[v3.0.0-alpha1]: https://github.com/eclipse-iofog/agent/compare/v2.0.6..v3.0.0-alpha1
[v2.0.7]: https://github.com/eclipse-iofog/agent/compare/v2.0.7..v2.0.7
[v2.0.6]: https://github.com/eclipse-iofog/agent/compare/v2.0.5..v2.0.6
[v2.0.5]: https://github.com/eclipse-iofog/agent/compare/v2.0.4..v2.0.5
[v2.0.4]: https://github.com/eclipse-iofog/agent/compare/v2.0.3..v2.0.4
[v2.0.3]: https://github.com/eclipse-iofog/agent/compare/v2.0.2..v2.0.3
[v2.0.2]: https://github.com/eclipse-iofog/agent/compare/v2.0.1..v2.0.2
[v2.0.1]: https://github.com/eclipse-iofog/agent/compare/v2.0.0-beta2..v2.0.1
[v2.0.0-beta2]: https://github.com/eclipse-iofog/agent/compare/v2.0.0-beta..v2.0.0-beta2
[v2.0.0-beta]: https://github.com/eclipse-iofog/agent/compare/v1.3.0..v2.0.0-beta
[v1.3.0]: https://github.com/eclipse-iofog/agent/tree/v1.3.0