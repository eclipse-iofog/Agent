# Test Message Generator

As developers build ioElement containers, they start by building the code in a non-container environment. They need to test the code before spending the time turning it into a published container... but they can't actually test the processing of messages or connection to the ioFog instance without going through the publishing and deployment process.

To facilitate development, we need to have a surrogate version of the ioFog Local API. It should mimic the API endpoints of the real ioFog Local API, including offering the control Websocket and message Websocket. It should run on "localhost" so it can be reached directly on the computer that is being used to build the ioElement container.

A developer can precisely mimic the production environment on their build machine by mapping a host. If they map "127.0.0.1" with the host name "iofog" then their local code will be able to operate with the same "http://iofog:54321/" endpoints found in the SDKs and described in the API specification.

#### Functional Requirements

* Allow the developer to set up a list of fully defined ioMessages that the Test Message Generator will output

* Read the list of defined ioMessages from a local file called "messages.xml"

* Randomly send ioMessages from the list as output

* Allow the developer to specify the rate of output messages

* Provide the full set of API endpoints specified for the production Local API module of ioFog (including both Websockets)

* Run as a local server listening on port 54321 just like the production ioFog Local API

* Log messages that are posted into the Test Message Generator so developers can verify that their message transmission is working properly

* Output the messages that are posted into the Test Message Generator in a local file called "receivedmessages.xml"

* Allow the developer to set up configuration JSON for their ioElement container that the Test Message Generator will give as output

* Read the configuration JSON that the developer has setup from a local file called "containerconfig.json"

* Allow the developer to specify the rate of transmission of "new configuration" control messages

* Send a "new configuration" control message to the ioElement container at the interval specified by the developer

* Send the complete list of defined ioMessages when the ioElement container makes a request on the "http://iofog:54321/v2/messages/query" endpoint. Do not send any "new configuration" control messages. No matter what publisher list and timeframe is submitted to this endpoint, only respond with the complete list of defined data messages.

* Read the Test Message Generator configuration from a local file called "configuration.xml"


#### Configuration Example

Developers can set the Test Message Generator to provide messages at a certain rate. The message that is sent will be randomly selected from the list of defined ioMessages. Setting up the behavior of the Test Message Generator is done with a local configuration XML file. Interval times are in milliseconds, allowing developers to test their container code with fast message rates.

Here is a sample of the configuration file:

<pre>
	&lt;configuration&gt;
		&lt;datamessageinterval&gt;500&lt;/datamessageinterval&gt;
		&lt;controlmessageinterval&gt;10000&lt;/controlmessageinterval&gt;
	&lt;/configuration&gt;
</pre>

