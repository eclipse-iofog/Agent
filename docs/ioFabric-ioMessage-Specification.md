# ioMessage Specification version 4 (March 2nd, 2016)

The purpose of a message is move information along a path. No understanding of the contents of the messages should be required in order to help it to its correct destination. The header fields of each message, however, are intended to be read and understood by functional pieces of the iofog system. Because the data contents of the message format are open, that means each recipient will be required to determine for itself if it understands how to read the data. Recipients can check the information type and information format headers to determine this.

The ioMessage versions are integers, not decimals. This is because it is harder to parse a raw binary message with decimals across different computing platforms. So... ioMessage versions will be things like 4, 5, and 12. The version can be used to determine what fields will be present in the message and perhaps how the data will be arranged in those fields.

The ID for each message must be unique across the Earth for 20 years or longer. Depending on the volume of ioMessages across the globe, a 128-bit identifier may reach a 99.9%+ chance of collisions well before that timeframe ends. So a 256-bit identifier has been chosen and should suffice.

The fields listed here do not contain any formatting information except for the ID, which is strictly standardized. Each embodiment of the ioMessage standard will make use of the best features of the embodiment method. For example, when using JSON to create ioMessages, there is no need to include length information about the different fields. And there is no need to put any particular field in any particular position. XML is similar. But when encoding an ioMessage in raw bytes, the order of the information is very crucial for packing and parsing the messages. While JSON and XML offer some advantages, they also have more overhead than raw bytes. And while raw byte formatting requires parsing by the receiver, it also has very low overhead and is excellent for real-time transmission of media such as photos or video.

A listing for JSON, XML, and raw bytes is included in this document after the main field listing.

### Fields of an ioMessage

#### ID
| | |
|---|---|
|*Data Type*|Text|
|*Key*|ID|
|*Required*|Yes|
|*Description*|A 256-bit universally unique identifier per message allows for portability and globe-wide verification of events. The ID string is formatted in base58 for readability, transmission safety between systems, and compactness.|

#### Tag
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|Tag|
|*Required*|No|
|*Description*|This is an open field for associating a message with a particular device or any other interesting thing. It should be queryable later, making this a high-value field for some applications.|
</pre>

#### Message Group ID
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|GroupID|
|*Required*|No|
|*Description*|This is how messages can be allocated to a sequence or stream.|
</pre>

#### Sequence Number
|   |   |
|---|---|
|*Data Type*|Integer|
|*Key*|SequenceNumber|
|*Required*|No|
|*Description*|What number in the sequence is this current message?|
</pre>

#### Sequence Total
|   |   |
|---|---|
|*Data Type*|Integer|
|*Key*|SequenceTotal|
|*Required*|No|
|*Description*|How many total messages are in the sequence? Absence of a total count while sequence numbers and a message group ID are present may be used to indicate a stream with indeterminate length.|
</pre>

#### Priority
|   |   |
|---|---|
|*Data Type*|Integer|
|*Key*|Priority|
|*Required*|No|
|*Description*|The lower the number, the higher the priority. This is a simple quality of service (QoS) indicator. Emergency messages or system error logs might get the highest priority. Self-contained messages (such as a button push or a temperature reading) might get very high priority. Media stream messages (such as one second of audio) might get very low priority ranking in order to allow message slowing or dropping as needed in a busy system.|
</pre>

#### Timestamp
|   |   |
|---|---|
|*Data Type*|Integer|
|*Key*|Timestamp|
|*Required*|Yes|
|*Description*|Universal timecode including milliseconds. Milliseconds can be entered as zeroes if needed.|
</pre>

#### Publisher
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|Publisher|
|*Required*|Yes|
|*Description*|This is the identifier of the element that is sending the message. It can be used to determine routing or guarantee privacy and security. Because each element is assigned a UUID during configuration, even across ioFog instances no message should be received by an unintended entity.|
</pre>

#### Authentication Identifier
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|AuthID|
|*Required*|No|
|*Description*|This is an open field to pass along authentication information about the particular authorized entity generating the message, such as an employee ID number or a user ID in the application.|
</pre>

#### Authentication Group
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|AuthGroup|
|*Required*|No|
|*Description*|This is an open field to pass authentication group information. This allows pieces of the application to know they are dealing with a message from an authenticated user of a particular type (such as “employee” or “system admin”) without needing to know the actual identification information.|
</pre>

#### ioMessage Version
|   |   |
|---|---|
|*Data Type*|Integer|
|*Key*|Version|
|*Required*|Yes|
|*Description*|Which version of the ioMessage format does this particular message comply with?|
</pre>

#### Chain Position
|   |   |
|---|---|
|*Data Type*|Integer|
|*Key*|ChainPosition|
|*Required*|No|
|*Description*|When using cryptographic message chaining, this field represents the position in the message chain that this paricular message occupies. It is similar to the "block height" value found in blockchain technology.|
</pre>

#### Hash
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|Hash|
|*Required*|No|
|*Description*|When using cryptographic message chaining, a hash of this entire message can be included here.|
</pre>

#### Previous Message Hash
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|PreviousHash|
|*Required*|No|
|*Description*|When using cryptographic message chaining, the hash value of the previous message is included here. This forms the cryptographic link from the prior message to this one.|
</pre>

#### Nonce
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|Nonce|
|*Required*|No|
|*Description*|When using cryptographic message chaining, an open field is needed to achieve the correct hash value. The information in this field will not be meaningful, but will be necessary to produce the final hash of the message.|
</pre>

#### Difficulty Target
|   |   |
|---|---|
|*Data Type*|Integer|
|*Key*|DifficultyTarget|
|*Required*|No|
|*Description*|When using cryptographic message chaining, this field represents the hashing workload required to cryptographically seal the chain.|
</pre>

#### Information Type
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|InfoType|
|*Required*|Yes|
|*Description*|This is like a MIME type. It describes what type of information is contained in the content data field.|
</pre>

#### Information Format
|   |   |
|---|---|
|*Data Type*|Text|
|*Key*|InfoFormat|
|*Required*|Yes|
|*Description*|This is a sub-field of the Information Type. It defines the format of the data content in this message. If the information type is “Temperature”, for example, then the information format might be “Degrees Kelvin”.|
</pre>

#### Context Data
|   |   |
|---|---|
|*Data Type*|Any (including binary, text, integer, etc.)|
|*Key*|ContextData|
|*Required*|No|
|*Description*|Context data in raw bytes. This field can be used to embed any information desired and will likely be very different from one solution to the next. It is the responsibility of the receiving element(s) to understand the context data format and the meaning of the context information.|
</pre>

#### Data Content
|   |   |
|---|---|
|*Data Type*|Any (including binary, text, integer, etc.)|
|*Key*|ContentData|
|*Required*|Yes|
|*Description*|The actual data content of the message in its raw form. Having a raw format for this field allows for the greatest amount of flexibility in the system.|
</pre>


### JSON Embodiment of an ioMessage

The ContextData and ContentData fields of an ioMessage, when embodied in JSON, will always be base64 encoded. This is because these fields contain raw bytes and there is no other way to represent raw bytes in the utf-8 structure that JSON uses. Upon receiving a JSON ioMessage, you must base64 decode those two fields. Before sending a JSON ioMessage, you must base64 encode those two fields.

<pre>
	{
		"id":"sd098wytfskduhdsfDSKfhjw4o8ytwesdoiuhsdf",
		"tag":"Bosch Camera 16",
		"groupid":"",
		"sequencenumber":1,
		"sequencetotal":1,
		"priority":0,
		"timestamp":1234567890123,
		"publisher":"Ayew98wtosdhFSKdjhsdfkjhkjesdhg",
		"authid":"",
		"authgroup":"",
		"version":4,
		"chainposition":0,
		"hash":"",
		"previoushash":"",
		"nonce":"",
		"difficultytarget":0.0,
		"infotype":"image/jpeg",
		"infoformat":"file/.jpg",
		"contextdata":"",
		"contentdata":"sdkjhwrtiy8wrtgSDFOiuhsrgowh4touwsdhsDFDSKJhsdkljasjklweklfjwhefiauhw98p328946982weiusfhsdkufhaskldjfslkjdhfalsjdf=serg4towhr"
	}
</pre>


### XML Embodiment of an ioMessage

The ContextData and ContentData fields of an ioMessage, when embodied in XML, will always be base64 encoded. This is because these fields contain raw bytes and there is no other way to represent raw bytes in the text formats that XML uses. Upon receiving an XML ioMessage, you must base64 decode those two fields. Before sending an XML ioMessage, you must base64 encode those two fields.

<pre>
	&lt;iomessage&gt;
		&lt;id&gt;sd098wytfskduhdsfDSKfhjw4o8ytwesdoiuhsdf&lt;/id&gt;
		&lt;tag&gt;Bosch Camera 16&lt;/tag&gt;
		&lt;groupid&gt;&lt;/groupid&gt;
		&lt;sequencenumber&gt;1&lt;/sequencenumber&gt;
		&lt;sequencetotal&gt;1&lt;/sequencetotal&gt;
		&lt;priority&gt;0&lt;/priority&gt;
		&lt;timestamp&gt;1234567890123&lt;/timestamp&gt;
		&lt;publisher&gt;Ayew98wtosdhFSKdjhsdfkjhkjesdhg&lt;/publisher&gt;
		&lt;authid&gt;&lt;/authid&gt;
		&lt;authgroup&gt;&lt;/authgroup&gt;
		&lt;version&gt;4&lt;/version&gt;
		&lt;chainposition&gt;0&lt;/chainposition&gt;
		&lt;hash&gt;&lt;/hash&gt;
		&lt;previoushash&gt;&lt;/previoushash&gt;
		&lt;nonce&gt;&lt;/nonce&gt;
		&lt;difficultytarget&gt;0.0&lt;/difficultytarget&gt;
		&lt;infotype&gt;image/jpeg&lt;/infotype&gt;
		&lt;infoformat&gt;file/.jpg&lt;/infoformat&gt;
		&lt;contextdata&gt;&lt;/contextdata&gt;
		&lt;contentdata&gt;sDFDSKJhsdkljasjklweklfjwhefiauhw98p328946982weiusfhsdkufha&lt;/contentdata&gt;
	&lt;/iomessage&gt;
</pre>


### Binary Embodiment of an ioMessage

Bytes are octets here. No funny business. Just good old 8-bit bytes. The sequence of bytes here must be followed strictly so the message can be parsed by the receiver.

<pre>
	[2 bytes] - Version

	[1 bytes] - Length of ID field
	[2 bytes] - Length of Tag field
	[1 bytes] - Length of Group ID field
	[1 bytes] - Length of Sequence Number field
	[1 bytes] - Length of Sequence Total field
	[1 bytes] - Length of Priority field
	[1 bytes] - Length of Timestamp field
	[1 bytes] - Length of Publisher field
	[2 bytes] - Length of Auth ID field
	[2 bytes] - Length of Auth Group field
	[1 bytes] - Length of Chain Position field
	[2 bytes] - Length of Hash field
	[2 bytes] - Length of Previous Hash field
	[2 bytes] - Length of Nonce field
	[1 bytes] - Length of Difficulty Target field
	[1 bytes] - Length of Info Type field
	[1 bytes] - Length of Info Format field
	[4 bytes] - Length of Context Data field
	[4 bytes] - Length of Content Data field

	[n bytes] - ID value
	[n bytes] - Tag value
	[n bytes] - Group ID value
	[n bytes] - Sequence Number value
	[n bytes] - Sequence Total value
	[n bytes] - Priority value
	[n bytes] - Timestamp value
	[n bytes] - Publisher value
	[n bytes] - Auth ID value
	[n bytes] - Auth Group value
	[n bytes] - Chain Position value
	[n bytes] - Hash value
	[n bytes] - Previous Hash value
	[n bytes] - Nonce value
	[n bytes] - Difficulty Target value
	[n bytes] - Info Type value
	[n bytes] - Info Format value
	[n bytes] - Context Data value
	[n bytes] - Content Data value
</pre>

