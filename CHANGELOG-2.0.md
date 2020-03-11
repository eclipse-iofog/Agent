## v2.0.0-beta
* Features
    * Skupper integration 
    * Agent docker pruning
    * Enabling remote debugger
    * Report generator integration with azure pipeline
    * Added lock to avoid deprovisioning when provisioning is in progress
    * Updated router host/port config
    * docker-java version updated to 3.1.5
* Bug fixes
    * Stop and delete microservices when deprovision and delete agent respectively
    * Fixed routing issue
    * Enable deletion of other agents microservices
    * Fixed microservice move bug
    * Fixed deprovisioning and routing issues
    * Fixed message routing