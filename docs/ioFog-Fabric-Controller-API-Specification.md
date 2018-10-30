# Fog Controller API V2 Specification

##### his is the 2nd version of the Fog Controller API. The first version remains active and unchanged.

Each ioFog instance can do very little without connecting to a fog controller. In fact, connecting to a fog controller is what makes a particular ioFog instance become an actual part of the I/O Compute Fog. Every fog controller will offer this API exactly as it is shown here. This allows an ioFog instance to connect to fog controller and operate properly.

The API endpoints are listed here with a short description and the actual inputs and outputs. The actual IP address or domain name of the fog controller will vary from deployment to deployment. It is mandatory that HTTPS be used, and both domain names and IP addresses are allowed for connecting to a fog controller. The placeholder address of 1.2.3.4 is used in this document for the location of the fog controller.

#### Get Server Status

This endpoint just gives you a response from the fog controller with its status. It can be used for simple "ping" purposes to see if the fog controller is online and operational.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/status
</pre>

##### Response

<pre>
	{
		“status”:”ok”,
		”timestamp”:1234567890123
	}
</pre>

##### Querystring Parameters

<pre>
	None
</pre>

##### POST Parameters

<pre>
	None
</pre>


#### Get ioFog UUID and Access Token

This endpoint registers the ioFog instance that is submitting the provisioning key and delivers the ioFog UUID along with an access token that must be submitted for any further API interaction. The access token remains valid until it is revoked. If it becomes invalid, the ioFog instance must be re-provisioned to re-establish access to the fog controller API.

The ioFog UUID provided by this endpoint is a 128-bit random ID formatted in base58. We use base58 for compactness, readability, portability, and transmission safety between systems.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/instance/provision/key/A8842h/fogtype/1
</pre>

##### Response

<pre>
	{
		“status”:”ok”,
		”timestamp”:1234567890123,
		“id”:”4sd9whcfh”,
		“token”:”3498wfesdhusdvkjh3refkjhsdpaohrg”
	}
</pre>

##### Querystring Parameters

<pre>
	key - the provisioning key provided via the command line (example shown here as a8842h)
	fogtype - an integer representing the system architecture of this ioFog instance
</pre>

##### POST Parameters

<pre>
	None
</pre>


#### Post ioFog Instance Status Information

This endpoint allows the ioFog instance to send its status information to the fog controller. This should be done regularly, but not so often as to waste bandwidth and CPU resources.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/instance/status/id/4sd9whcfh/token/3498wfesdhusdvkjh3refkjhsdpaohrg
</pre>

##### Response

<pre>
	{
		“status”:”ok”,
		”timestamp”:1234567890123
	}
</pre>

##### Querystring Parameters

<pre>
	UUID - the UUID held by the ioFog instance (example shown here as 4sd9whcfh)
    
    token - the access token given to the ioFog instance for accessing the API (example shown here as 3498wfesdhusdvkjh3refkjhsdpaohrg)
</pre>

##### POST Parameters

<pre>
    daemonstatus - ioFog daemon status string (example: running)
    
    daemonoperatingduration - ioFog daemon operating duration in milliseconds (example: 43473272)
    
    daemonlaststart - Timestamp of the ioFog daemon last start in milliseconds (example: 1234567890123)
    
    memoryusage - ioFog current memory usage in mebibytes MiB (example: 562.8)
    
    diskusage - ioFog current disk usage in gibibytes GiB (example: 2.79)
    
    cpuusage - ioFog current CPU usage in percent (example: 24.71)
    
    memoryviolation - Status indicating if the ioFog's current memory usage is in violation of the configured limit (example: yes)
    
    diskviolation - Status indicating if the ioFog's current disk usage is in violation of the configured limit (example: no)
    
    cpuviolation - Status indicating if the ioFog's current CPU usage is in violation of the configured limit (example: no)
    
    elementstatus - JSON string providing the status of all elements (example below)
    	
    	[{"id":"sdfkjhweSDDkjhwer8","status":"starting","starttime":1234567890123,"operatingduration":278421},{"id":"239y7dsDSFuihweiuhr32we","status":"stopped","starttime":1234567890123,"operatingduration":421900}]
        
    repositorycount - Number of Docker container registries being used by the ioFog instance (example: 5)

    repositorystatus - JSON string providing the status of all the repositories (example below)

    	[{"url":"hub.docker.com","linkstatus":"connected"},{"url":"188.65.2.81/containers","failed login"}]

    systemtime - Timestamp of the current ioFog system time in milliseconds (example: 1234567890123)
    
    laststatustime - Timestamp in milliseconds of the last time any status information on the ioFog instance was updated (example: 1234567890123)

    ipaddress - Current IP address of the network adapter configured for the ioFog instance (example: 10.0.15.13)

    processedmessages - Total number of messages processed by the ioFog instance (example: 4481)

    elementmessagecounts - JSON string providing the number of messages published per element (example below)

    	[{"id":"d9823y23rewfouhSDFkh","messagecount":428},{"id":"978yerwfiouhASDFkjh","messagecount":8321}]
    
    messagespeed - The average speed, in milliseconds, of messages moving through the ioFog instance (example: 84)

    lastcommandtime - Timestamp, in milliseconds, of the last update received by the ioFog instance (example: 1234567890123)
    
    version - String representing the current version of the ioFog software that is posting status (example: 1.24)
</pre>


#### Get ioFog Configuration

This endpoint provides the configuration for the ioFog instance. Note that some configuration items, such as the fog controller URL and certificate path, are not featured here. That's for security reasons. If someone gains control of a fog controller, we don't want them to be able to tell the ioFog instances to listen to a different fog controller. This also prevents accidental disconnection of ioFog instances from the fog controller.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/instance/config/id/4sd9whcfh/token/3498wfesdhusdvkjh3refkjhsdpaohrg
</pre>

##### Response

<pre>
	{
        “status”:”ok”,
        “timestamp”:1234567890123,
        “config”:
            {
                "networkinterface":"p2p1",
                "dockerurl":"unix:///var/run/docker.sock",
                "disklimit":12.0,
                "diskdirectory":"/var/lib/iofog/",
                "memorylimit":1024.0,
                "cpulimit":35.58,
                "loglimit":2.45,
                "logdirectory":"/var/log/iofog/",
                "logfilecount":10
            }
    }
</pre>

##### Querystring Parameters

<pre>
	UUID - the UUID held by the ioFog instance (example shown here as 4sd9whcfh)
    
    token - the access token given to the ioFog instance for accessing the API (example shown here as 3498wfesdhusdvkjh3refkjhsdpaohrg)
</pre>

##### POST Parameters

<pre>
	None
</pre>


#### Post ioFog Configuration

This endpoint allows the ioFog instance to send its configuration to the fog controller. It should send the updated configuration to this endpoint whenever a change is made locally.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/instance/config/changes/id/4sd9whcfh/token/3498wfesdhusdvkjh3refkjhsdpaohrg
</pre>

##### Response

<pre>
	{
		“status”:”ok”,
		”timestamp”:1234567890123
	}
</pre>

##### Querystring Parameters

<pre>
	UUID - the UUID held by the ioFog instance (example shown here as 4sd9whcfh)
    
    token - the access token given to the ioFog instance for accessing the API (example shown here as 3498wfesdhusdvkjh3refkjhsdpaohrg)
</pre>

##### POST Parameters

<pre>
    networkinterface - example: p2p1

    dockerurl - example: unix:///var/run/docker.sock

    disklimit - example: 12.0

    diskdirectory - example: /var/lib/iofog/

    memorylimit - example: 1024.0

    cpulimit - example: 35.58

    loglimit - example: 2.45

    logdirectory - example: /var/log/iofog/

    logfilecount - example: 10
</pre>


#### Get ioFog Changes List

This endpoint lists the current changes for the ioFog instance. Much of the time there will not be any changes. The ioFog instance should use this endpoint to check frequently, such as every 20 seconds. The changes are calculated based upon the timestamp that is sent in the querystring parameters. The timestamp must be stored locally in the ioFog instance and passed to this endpoint on every request. It should be updated whenever a successful call to this endpoint is completed, and should use the timestamp provided in the response.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/instance/changes/id/4sd9whcfh/token/3498wfesdhusdvkjh3refkjhsdpaohrg/timestamp/1234567890123
</pre>

##### Response

<pre>
	{
        “status”:”ok”,
        “timestamp”:1234567890123,
        “changes”:
            {
                “config”:false,
                “containerlist”:false,
                “containerconfig”:true,
                “routing”:false,
                "registries":true
            }
    }
</pre>

##### Querystring Parameters

<pre>
	UUID - the UUID held by the ioFog instance (example shown here as 4sd9whcfh)
    
    token - the access token given to the ioFog instance for accessing the API (example shown here as 3498wfesdhusdvkjh3refkjhsdpaohrg)

    timestamp - the timestamp from the last received results of this specific API call (example shown here as 1234567890123)
</pre>

##### POST Parameters

<pre>
	None
</pre>


#### Get ioFog Container List

This endpoint provides the current list of containers that should be running on the ioFog instance. Containers should be added, removed, and restarted based upon this list. A change in port mappings should result in a restart because containers can only have their port mappings updated when they are being started. When the "rebuild" flag is set to true, the Docker daemon should be asked to build the container again. If there is an updated image in the registry, Docker will see the change and flush its cache and build the container from the updated image. Triggering container updates is the purpose of this "rebuild" flag.

When the "roothostaccess" flag is set to true, the container should have its network mapped directly to the host network, which is done in Docker by the command "--net=host". Custom port mappings are not possible in this configuration, so any port mappings that are specified for that container should be ignored.

The container IDs provided by this endpoint are 128-bit random IDs formatted in base58. We use base58 for compactness, readability, portability, and transmission safety between systems.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/instance/containerlist/id/4sd9whcfh/token/3498wfesdhusdvkjh3refkjhsdpaohrg
</pre>

##### Response

<pre>
	{
        “status”:”ok”,
        “timestamp”:1234567890123,
        “containerlist”:
            [
                {
                    “id”:”sh23489gyrsdifuhw3iruedsifyasf”,
                    “imageid”:”iofog/open-weather-map”,
                    "registryurl":"hub.docker.com",
                    “lastmodified”:1234567890123,
                    "roothostaccess":false,
                    "rebuild":false,
                    “portmappings”:
                        [
                            {
                                “outsidecontainer”:”5500”,
                                “insidecontainer”:”80”
                            },
                            {
                                “outsidecontainer”:”5650”,
                                “insidecontainer”:”2040”
                            }
                        ]
                },
				{
                    “id”:”debug”,
                    “imageid”:”iofog/seismic-sensor-simulator”,
                    "registryurl":"24.158.9.17",
					“lastmodified”:1234567890123,
                    "roothostaccess":true,
					"rebuild":true,
                    “portmappings”:
                        [
                        ]
				}
            ]
    }
</pre>

##### Querystring Parameters

<pre>
	UUID - the UUID held by the ioFog instance (example shown here as 4sd9whcfh)
    
    token - the access token given to the ioFog instance for accessing the API (example shown here as 3498wfesdhusdvkjh3refkjhsdpaohrg)
</pre>

##### POST Parameters

<pre>
	None
</pre>


#### Get ioFog Container Configuration

This endpoint provides the JSON configuration strings for all of the containers that should be running on the ioFog instance. Note that the container configuration JSON strings are escaped. This is because they are being delivered inside a JSON response and we don't want these configuration strings to become part fo the actual response object. We want the strings to be unescaped and passed to the containers without being parsed.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/instance/containerconfig/id/4sd9whcfh/token/3498wfesdhusdvkjh3refkjhsdpaohrg
</pre>

##### Response

<pre>
	{
        “status”:”ok”,
        “timestamp”:1234567890123,
        “containerconfig”:
            [
                {
                    “id”:”sdguh34tkwjdhfsdkhfs”,
                    “lastupdatedtimestamp”:1234567890123,
                    “config”:”\{\”username\”:\”iokilton\”,\”password\”:\”abc123\”\}”
				},
				{
                    “id”:”345t9yergdskfhtwerwhuk”,
                    “lastupdatedtimestamp”:1234567890123,
                    “config”:”\{\”speed\”:40,\”sendphotos\”:true\}”
				},
				{
                    “id”:”viewer”,
                    “lastupdatedtimestamp”:1234567890123,
                    “config”:”\{\}”
				}
            ]
    }
</pre>

##### Querystring Parameters

<pre>
	UUID - the UUID held by the ioFog instance (example shown here as 4sd9whcfh)
    
    token - the access token given to the ioFog instance for accessing the API (example shown here as 3498wfesdhusdvkjh3refkjhsdpaohrg)
</pre>

##### POST Parameters

<pre>
	None
</pre>


#### Get ioFog Routing

This endpoint provides the routing plan for all containers. Note that no container ever specifies its inputs. It only specifies its outputs. This is because the vast majority of IoT data streams begins with a container that does not take in ioMessages. It just connects to some external device or external system. Then it publishes ioMessages and the routing chain begins as a sequence of outputs from container to container.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/instance/routing/id/4sd9whcfh/token/3498wfesdhusdvkjh3refkjhsdpaohrg
</pre>

##### Response

<pre>
	{
        “status”:”ok”,
        “timestamp”:1234567890123,
        “routing”:
            [
                {
                    “container”:”sdh4wte98yefsdouhdv”,
                    “receivers”:
						[
                            “349y8sdofshsdefh”,
                            “2398yrodsfkdshdsf”,
                            “viewer”,
                            “debug”
                        ]
				},
				{
                    “container”:”ou23uewds98tesdfjkhsed”,
                    “receivers”:
						[
                            “iwe32rlejsdfxkjhsdf”,
                            “2398yrodsfkdshdsf”
                        ]
				}
            ]
    }
</pre>

##### Querystring Parameters

<pre>
	UUID - the UUID held by the ioFog instance (example shown here as 4sd9whcfh)
    
    token - the access token given to the ioFog instance for accessing the API (example shown here as 3498wfesdhusdvkjh3refkjhsdpaohrg)
</pre>

##### POST Parameters

<pre>
	None
</pre>


#### Get ioFog Registries

This endpoint provides the list of Docker container registries that the ioFog instance needs to load container images from. Login credentials are provided for each registry. Information about whether or not a registry is secure is also provided. If a registry is not secure, it should be added to the Docker daemon "insecure" list. If a registry is secure, it may or may not require a certificate in order to access it. If it does, the certificate will be provided directly in the API response. Note that this field may contain intermediate certificates bundled into the certificate chain, making this a rather large amount of text. The certificate example shown in this documentation is merely a placeholder.

##### Endpoint

<pre>
	https://1.2.3.4/api/v2/instance/registries/id/4sd9whcfh/token/3498wfesdhusdvkjh3refkjhsdpaohrg
</pre>

##### Response

<pre>
	{
        “status”:”ok”,
        “timestamp”:1234567890123,
        "registries":
            [
                {
                	"url":"15.68.152.8",
                	"secure":true,
                	"certificate":"4wht9wdfsSkusdfhi234kwrwoeruawofjas=wetiuh4wefssdf...",
                	"requirescert":true,
                	"username":"foguser1",
                	"password":"abc123",
                	"useremail":"jim@themail.com"
            	},
            	{
                	"url":"hub.docker.com",
                	"secure":true,
                	"certificate":"",
                	"requirescert":false,
                	"username":"iofog",
                	"password":"abc123",
                	"useremail":"iofog_user@iofog.org"
            	}
            ]
    }
</pre>

##### Querystring Parameters

<pre>
	UUID - the UUID held by the ioFog instance (example shown here as 4sd9whcfh)
    
    token - the access token given to the ioFog instance for accessing the API (example shown here as 3498wfesdhusdvkjh3refkjhsdpaohrg)
</pre>

##### POST Parameters

<pre>
	None
</pre>


