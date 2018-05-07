# Container SDK Requirements

The ioFog Local API allows developers to use any language and framework to build ioElement containers. It uses standard REST API endpoints that speak JSON for stateless communication and it uses Websockets for real-time message and control communication.

But while the API is easy to work with, it is always nice to skip some coding effort and jump right into the application development. To help developers get started faster and experience fewer issues, we need to provide SDK libraries for popular languages.

The number of SDKs will grow over time as new languages are added. And developers across the world are welcome to create their own SDKs if we don't yet offer one for their favorite language. The requirements here are for the starting SDKs that we will provide for everyone. They reflect the most popular languages for building IoT components.

#### Java

* Provide a class for messages according to the ioMessage Specification document
* Provide a standard listener (Observer) pattern for developers to receive incoming real-time control and message communication
* Provide methods for performing all of the functionality listed in the Local API Specification document
* Publish the SDK so it can be included in any developer's build without manual installation
* Provide convenience functions for changing base64 encoded data to raw bytes
* Decode (from base64 to raw bytes) the ContextData and ContentData fields of messages arriving via the REST API
* Encode (from raw bytes to base64) the ContextData and ContentData fields of messages being send to the REST API


#### Node.js

* Treat messages as standard JavaScript objects according to the ioMessage Specification document
* Provide standard callbacks for developers to receive incoming real-time control and message communication
* Provide methods for performing all of the functionality listed in the Local API Specification document
* Publish the SDK with NPM so it can be included in any developer's build without manual installation
* Provide convenience functions for changing base64 encoded data to raw bytes
* Decode (from base64 to raw bytes) the ContextData and ContentData fields of messages arriving via the REST API
* Encode (from raw bytes to base64) the ContextData and ContentData fields of messages being send to the REST API


#### Python

* Provide a class for messages according to the ioMessage Specification document
* Provide standard callbacks for developers to receive incoming real-time control and message communication
* Provide methods for performing all of the functionality listed in the Local API Specification document
* Publish the SDK with the Python foundation so it can be included (using pip) in any developer's build without manual installation
* Provide convenience functions for changing base64 encoded data to raw bytes
* Decode (from base64 to raw bytes) the ContextData and ContentData fields of messages arriving via the REST API
* Encode (from raw bytes to base64) the ContextData and ContentData fields of messages being send to the REST API


#### C

* Provide a struct for messages according to the ioMessage Specification document
* Use the 'curl' library to avoid re-writing low-level networking
* Provide function pointer openings or direct socket access for developers to receive incoming real-time control and message communication
* Provide functions for performing all of the functionality listed in the Local API Specification document
* Provide convenience functions for changing base64 encoded data to raw bytes
* Decode (from base64 to raw bytes) the ContextData and ContentData fields of messages arriving via the REST API
* Encode (from raw bytes to base64) the ContextData and ContentData fields of messages being send to the REST API


#### C++

* Provide a class for messages according to the ioMessage Specification document
* Use the 'curl' library to avoid re-writing low-level networking
* Provide a standard observer pattern implementation for developers to receive incoming real-time control and message communication
* Provide methods for performing all of the functionality listed in the Local API Specification document
* Provide convenience functions for changing base64 encoded data to raw bytes
* Decode (from base64 to raw bytes) the ContextData and ContentData fields of messages arriving via the REST API
* Encode (from raw bytes to base64) the ContextData and ContentData fields of messages being send to the REST API


#### C&#35;

* Provide a class for messages according to the ioMessage Specification document
* Provide a standard observer pattern implementation for developers to receive incoming real-time control and message communication
* Provide methods for performing all of the functionality listed in the Local API Specification document
* Provide convenience functions for changing base64 encoded data to raw bytes
* Decode (from base64 to raw bytes) the ContextData and ContentData fields of messages arriving via the REST API
* Encode (from raw bytes to base64) the ContextData and ContentData fields of messages being send to the REST API

