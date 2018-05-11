# Debug Console Specification

The Debug Console system container receives configuration through the ioFog Local API just like every other container. The configuration is specified here. It hosts a REST API that is also specified here.

#### Container Configuration Example
<pre>
	{"accesstoken":"fshkuewwre89ysdkSDFHKJwe9ywiuhfsdkhj","filesizelimit":200.0}

	The "accesstoken" value is the current token that must be provided by anyone attempting to access the REST API

	The "filesizelimit" value is the size limit of each message storage file that is created per publisher, in MiB
</pre>


### REST API Endpoints

All endpoints are hosted on port 80 as regular HTTP REST API that provide JSON outputs (MIME type of application/json). All endpoints require that the current access token be passed in the query otherwise the response should be a "404 not found" HTTP response code.

#### Get Debug Messages For Publisher Within Timeframe

This endpoint takes in the access token, publisher ID, timeframe start, and timeframe end parameters and gives out a JSON array of messages. The messages are all messages that have been received by the Debug Console container and stored in a file for this particular publisher.

##### Endpoint

<pre>
	http://1.2.3.4:80/v2/debug/messages/publisher/dfshigu4wedsuiohdsf/starttime/1234567890123/endtime/1234567890123/accesstoken/fshkuewwre89ysdkSDFHKJwe9ywiuhfsdkhj

	Note that the example IP address of 1.2.3.4 will be replaced by the real container IP address and the container itself does not need to know the address
</pre>

##### Response

<pre>
	{
		"status":"okay",
		"count":2,
		"messages":
			[
				{
					"id":"ObJ5STY02PMLM4XKXM8oSuPlc7mUh5Ej",
					"tag":"",
					"groupid":"",
					"sequencenumber":1,
					"sequencetotal":1,
					"priority":0,
					"timestamp":1452214777495,
					"publisher":"dfshigu4wedsuiohdsf",
					"authid":"",
					"authgroup":"",
					"version":4,
					"chainposition":0,
					"hash":"",
					"previoushash":"",
					"nonce":"",
					"difficultytarget":0.0,
					"infotype":"text",
					"infoformat":"utf-8",
					"contextdata":"",
					"contentdata":"A New Message!"
				},
				{
					"id":"sd098wytfskduhdsfDSKfhjw4o8ytwesdoiuhsdf",
					"tag":"Bosch Camera 16",
					"groupid":"",
					"sequencenumber":1,
					"sequencetotal":1,
					"priority":0,
					"timestamp":1234567890123,
					"publisher":"dfshigu4wedsuiohdsf",
					"authid":"",
					"authgroup":"",
					"version":4,
					"chainposition":0,
					"hash":"",
					"previoushash":"",
					"nonce":"",
					"difficultytarget":0.0,
					"infotype":"image/jpeg",
					"infoformat":"base64",
					"contextdata":"",
					"contentdata":"sdkjhwrtiy8wrtgSDFOiuhsrgowh4touwsdhsDFDSKJhsdkljasjklweklfjwhefiauhw98p328946982weiusfhsdkufhaskldjfslkjdhfalsjdf=serg4towhr"
				}
			]
	}
</pre>

##### Querystring Parameters

<pre>
	publisher - the Publisher ID from which to return messages

	starttime - the timestamp representing the earliest message desired (inclusive)

	endtime - the timestamp representing the latest message desired (inclusive)

	accesstoken - the current access token for getting access to the REST API endpoints
</pre>

##### POST Parameters

<pre>
	None
</pre>

