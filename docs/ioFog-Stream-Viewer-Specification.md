# Stream Viewer Specification

The Stream Viewer system container receives configuration through the ioFog Local API just like every other container. The configuration is specified here. It hosts a REST API and a standard HTTP Web server that are also specified here.

#### Container Configuration Example
<pre>
	{"accesstoken":"fshkuewwre89ysdkSDFHKJwe9ywiuhfsdkhj","foldersizelimit":200.0}

	The "accesstoken" value is the current token that must be provided by anyone attempting to access the REST API

	The "foldersizelimit" value is the size limit of each output file storage folder that is created per publisher, in MiB
</pre>


### REST API Endpoints

All endpoints are hosted on port 80 as regular HTTP REST API that provide JSON outputs (MIME type of application/json). All endpoints require that the current access token be passed in the query otherwise the response should be a "404 not found" HTTP response code.

#### Get File List For Publisher Within Timeframe

This endpoint takes in the access token, publisher ID, timeframe start, and timeframe end parameters and gives out a JSON array of files. The files are all messages that have been received by the Stream Viewer container and converted into files and stored in a folder for this particular publisher.

##### Endpoint

<pre>
	http://1.2.3.4:80/v2/viewer/files/publisher/dfshigu4wedsuiohdsf/starttime/1234567890123/endtime/1234567890123/accesstoken/fshkuewwre89ysdkSDFHKJwe9ywiuhfsdkhj

	Note that the example IP address of 1.2.3.4 will be replaced by the real container IP address and the container itself does not need to know the address
</pre>

##### Response

<pre>
	{
		"status":"okay",
		"count":4,
		"files":
			[
				"files/dfshigu4wedsuiohdsf/1234567890123.txt",
				"files/dfshigu4wedsuiohdsf/1234567890128.txt",
				"files/dfshigu4wedsuiohdsf/1234567890149.txt",
				"files/dfshigu4wedsuiohdsf/1234567890202.png"
			]
	}
</pre>

##### Querystring Parameters

<pre>
	publisher - the Publisher ID from which to return files

	starttime - the timestamp representing the earliest file desired (inclusive)

	endtime - the timestamp representing the latest file desired (inclusive)

	accesstoken - the current access token for getting access to the REST API endpoints
</pre>

##### POST Parameters

<pre>
	None
</pre>


### HTTP Web Server

Standard HTTP Web server allow clients to request files that reside on the server. In the case of the Stream Viewer system container, the files are in the different publisher folders. The code for providing the files as output is the same as the code that would be used to provide Web content files as output. The URLs for the files will therefore look something like this:

<pre>
	http://1.2.3.4:80/files/dfshigu4wedsuiohdsf/1234567890202.png
</pre>



