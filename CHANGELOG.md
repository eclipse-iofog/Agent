## v1.3.0
* Features
    * Report microservices lifecycle to Controller
    * Showing available disk percentage in `iofog-agent info` output
* Bug fixes
    * Keep previous GPS coordinates when getting new one fails
    * Remove microservices when Agent is moved to another ECN
## v2.0.0
* Features
    * Skupper integration
    * Agent docker pruning

* Bug fixes
    * Stop and delete microservices when deprovision and delete agent respectively
    * Enable deletion of other agents microservices
    * Fixed microservice move bug
    * Fixed deprovisioning and routing issues
    * Fixed message routing
    
## v2.0.1
* Bug fixes
    * Configured default available disk threshold to 20
    
## unreleased
* Features
    * Dev mode added
