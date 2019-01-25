/*******************************************************************************
 * Copyright (c) 2018 Edgeworx, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Saeid Baghbidi
 * Kilton Hopkins
 *  Ashita Nagar
 *******************************************************************************/
package org.eclipse.iofog.message_bus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.json.Json;
import javax.json.JsonObject;

import org.eclipse.iofog.utils.BytesUtil;
import org.eclipse.iofog.utils.logging.LoggingService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.iofog.utils.logging.LoggingService.logError;
import static org.eclipse.iofog.utils.logging.LoggingService.logWarning;

/**
 * represents IOMessage
 * 
 * @author saeid
 *
 */
public class Message {
	private static final short VERSION = 4;
	private static final String MODULE_NAME = "Message";

	private String id;
	private String tag;
	private String messageGroupId;
	private int sequenceNumber;
	private int sequenceTotal;
	private byte priority;
	private long timestamp;
	private String publisher;
	private String authIdentifier;
	private String authGroup;
	private short version;
	private long chainPosition;
	private String hash;
	private String previousHash;
	private String nonce;
	private int difficultyTarget;
	private String infoType;
	private String infoFormat;
	private byte[] contextData;
	private byte[] contentData;

	public Message() {
		version = VERSION;
		id = null;
		tag = null;
		messageGroupId = null;
		sequenceNumber = 0;
		sequenceTotal = 0;
		priority = 0;
		timestamp = 0;
		publisher = null;
		authIdentifier = null;
		authGroup = null;
		chainPosition = 0;
		hash = null;
		previousHash = null;
		nonce = null;
		difficultyTarget = 0;
		infoType = null;
		infoFormat = null;
		contentData = null;
		contextData = null;
	}

	public Message(String publisher) {
		super();
		this.publisher = publisher;
	}

	public Message(JsonObject json) {
		super();
		if (json.containsKey("id"))
			setId(json.getString("id"));
		if (json.containsKey("tag"))
			setTag(json.getString("tag"));
		if (json.containsKey("groupid"))
			setMessageGroupId(json.getString("groupid"));
		if (json.containsKey("sequencenumber"))
			setSequenceNumber(json.getInt("sequencenumber"));
		if (json.containsKey("sequencetotal"))
			setSequenceTotal(json.getInt("sequencetotal"));
		if (json.containsKey("priority"))
			setPriority((byte) json.getInt("priority"));
		if (json.containsKey("timestamp"))
			setTimestamp(json.getJsonNumber("timestamp").longValue());
		if (json.containsKey("publisher"))
			setPublisher(json.getString("publisher"));
		if (json.containsKey("authid"))
			setAuthIdentifier(json.getString("authid"));
		if (json.containsKey("authgroup"))
			setAuthGroup(json.getString("authgroup"));
		if (json.containsKey("chainposition"))
			setChainPosition(json.getJsonNumber("chainposition").longValue());
		if (json.containsKey("hash"))
			setHash(json.getString("hash"));
		if (json.containsKey("previoushash"))
			setPreviousHash(json.getString("previoushash"));
		if (json.containsKey("nonce"))
			setNonce(json.getString("nonce"));
		if (json.containsKey("difficultytarget"))
			setDifficultyTarget(json.getInt("difficultytarget"));
		if (json.containsKey("infotype"))
			setInfoType(json.getString("infotype"));
		if (json.containsKey("infoformat"))
			setInfoFormat(json.getString("infoformat"));
		if (json.containsKey("contextdata")){
			String contextData = json.getString("contextdata");
			setContextData(Base64.getDecoder().decode(contextData.getBytes(UTF_8)));
		}
		if (json.containsKey("contentdata")){
			String contentData = json.getString("contentdata");
			setContentData(Base64.getDecoder().decode(contentData.getBytes(UTF_8)));
		}
	}

	public Message(byte[] rawBytes) {
		super();

		version = BytesUtil.bytesToShort(BytesUtil.copyOfRange(rawBytes, 0, 2));
		if (version != VERSION) {
			// TODO: incompatible version
			return;
		}

		int pos = 33;

		int size = rawBytes[2];
		if (size > 0) {
			id = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(rawBytes, 3, 5));
		if (size > 0) {
			tag = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = rawBytes[5];
		if (size > 0) {
			messageGroupId = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = rawBytes[6];
		if (size > 0) {
			sequenceNumber = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = rawBytes[7];
		if (size > 0) {
			sequenceTotal = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = rawBytes[8];
		if (size > 0) {
			priority = rawBytes[pos];
			pos += size;
		}

		size = rawBytes[9];
		if (size > 0) {
			timestamp = BytesUtil.bytesToLong(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = rawBytes[10];
		if (size > 0) {
			publisher = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(rawBytes, 11, 13));
		if (size > 0) {
			authIdentifier = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(rawBytes, 13, 15));
		if (size > 0) {
			authGroup = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = rawBytes[15];
		if (size > 0) {
			chainPosition = BytesUtil.bytesToLong(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(rawBytes, 16, 18));
		if (size > 0) {
			hash = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(rawBytes, 18, 20));
		if (size > 0) {
			previousHash = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(rawBytes, 20, 22));
		if (size > 0) {
			nonce = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = rawBytes[22];
		if (size > 0) {
			difficultyTarget = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = rawBytes[23];
		if (size > 0) {
			infoType = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = rawBytes[24];
		if (size > 0) {
			infoFormat = BytesUtil.bytesToString(BytesUtil.copyOfRange(rawBytes, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(rawBytes, 25, 29));
		if (size > 0) {
			contextData = BytesUtil.copyOfRange(rawBytes, pos, pos + size);
			pos += size;
		}

		size = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(rawBytes, 29, 33));
		if (size > 0) {
			contentData = BytesUtil.copyOfRange(rawBytes, pos, pos + size);
		}
	}

	public Message(byte[] header, byte[] data) {
		super();

		version = BytesUtil.bytesToShort(BytesUtil.copyOfRange(header, 0, 2));
		if (version != VERSION) {
			// TODO: incompatible version
			return;
		}

		int pos = 0;

		int size = header[2];
		if (size > 0) {
			id = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(header, 3, 5));
		if (size > 0) {
			tag = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = header[5];
		if (size > 0) {
			messageGroupId = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = header[6];
		if (size > 0) {
			sequenceNumber = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = header[7];
		if (size > 0) {
			sequenceTotal = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = header[8];
		if (size > 0) {
			priority = data[pos];
			pos += size;
		}

		size = header[9];
		if (size > 0) {
			timestamp = BytesUtil.bytesToLong(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = header[10];
		if (size > 0) {
			publisher = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(header, 11, 13));
		if (size > 0) {
			authIdentifier = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(header, 13, 15));
		if (size > 0) {
			authGroup = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = header[15];
		if (size > 0) {
			chainPosition = BytesUtil.bytesToLong(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(header, 16, 18));
		if (size > 0) {
			hash = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(header, 18, 20));
		if (size > 0) {
			previousHash = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToShort(BytesUtil.copyOfRange(header, 20, 22));
		if (size > 0) {
			nonce = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = header[22];
		if (size > 0) {
			difficultyTarget = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = header[23];
		if (size > 0) {
			infoType = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = header[24];
		if (size > 0) {
			infoFormat = BytesUtil.bytesToString(BytesUtil.copyOfRange(data, pos, pos + size));
			pos += size;
		}

		size = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(header, 25, 29));
		if (size > 0) {
			contextData = BytesUtil.copyOfRange(data, pos, pos + size);
			pos += size;
		}

		size = BytesUtil.bytesToInteger(BytesUtil.copyOfRange(header, 29, 33));
		if (size > 0) {
			contentData = BytesUtil.copyOfRange(data, pos, pos + size);
		}
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getMessageGroupId() {
		return messageGroupId;
	}
	public void setMessageGroupId(String messageGroupId) {
		this.messageGroupId = messageGroupId;
	}
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	public int getSequenceTotal() {
		return sequenceTotal;
	}
	public void setSequenceTotal(int sequenceTotal) {
		this.sequenceTotal = sequenceTotal;
	}
	public byte getPriority() {
		return priority;
	}
	public void setPriority(byte priority) {
		this.priority = priority;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public String getAuthIdentifier() {
		return authIdentifier;
	}
	public void setAuthIdentifier(String authIdentifier) {
		this.authIdentifier = authIdentifier;
	}
	public String getAuthGroup() {
		return authGroup;
	}
	public void setAuthGroup(String authGroup) {
		this.authGroup = authGroup;
	}
	public short getVersion() {
		return version;
	}
	public long getChainPosition() {
		return chainPosition;
	}
	public void setChainPosition(long chainPosition) {
		this.chainPosition = chainPosition;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public String getPreviousHash() {
		return previousHash;
	}
	public void setPreviousHash(String previousHash) {
		this.previousHash = previousHash;
	}
	public String getNonce() {
		return nonce;
	}
	public void setNonce(String nonce) {
		this.nonce = nonce;
	}
	public int getDifficultyTarget() {
		return difficultyTarget;
	}
	public void setDifficultyTarget(int difficultyTarget) {
		this.difficultyTarget = difficultyTarget;
	}
	public String getInfoType() {
		return infoType;
	}
	public void setInfoType(String infoType) {
		this.infoType = infoType;
	}
	public String getInfoFormat() {
		return infoFormat;
	}
	public void setInfoFormat(String infoFormat) {
		this.infoFormat = infoFormat;
	}
	public byte[] getContextData() {
		return contextData;
	}
	public void setContextData(byte[] contextData) {
		this.contextData = contextData;
	}
	public byte[] getContentData() {
		return contentData;
	}
	public void setContentData(byte[] contentData) {
		this.contentData = contentData;
	}

	private int getLength(String str) {
		if (str == null)
			return 0;
		else
			return str.length();
	}

	public byte[] getBytes() {
		try (ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();
			 ByteArrayOutputStream dataBaos = new ByteArrayOutputStream()){
			//version
			headerBaos.write(BytesUtil.shortToBytes(VERSION));

			// id
			int len = getLength(getId());
			headerBaos.write((byte) (len & 0xff));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getId()));

			// tag
			len = getLength(getTag());
			headerBaos.write(BytesUtil.shortToBytes((short) (len & 0xffff)));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getTag()));

			//groupid
			len = getLength(getMessageGroupId());
			headerBaos.write((byte) (len & 0xff));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getMessageGroupId()));

			// seq no
			if (getSequenceNumber() == 0)
				headerBaos.write(0);
			else {
				dataBaos.write(BytesUtil.integerToBytes(getSequenceNumber()));
				headerBaos.write(4);
			}

			// seq total
			if (getSequenceTotal() == 0)
				headerBaos.write(0);
			else {
				dataBaos.write(BytesUtil.integerToBytes(getSequenceTotal()));
				headerBaos.write(4);
			}


			// priority
			if (getPriority() == 0)
				headerBaos.write(0);
			else {
				headerBaos.write(1);
				dataBaos.write(getPriority());
			}

			//timestamp
			if (getTimestamp() == 0)
				headerBaos.write(0);
			else {
				headerBaos.write(8);
				dataBaos.write(BytesUtil.longToBytes(getTimestamp()));
			}

			// publisher
			len = getLength(getPublisher());
			headerBaos.write((byte) (len & 0xff));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getPublisher()));

			// authIdentifier
			len = getLength(getAuthIdentifier());
			headerBaos.write(BytesUtil.shortToBytes((short) (len & 0xffff)));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getAuthIdentifier()));

			// authGroup
			len = getLength(getAuthGroup());
			headerBaos.write(BytesUtil.shortToBytes((short) (len & 0xffff)));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getAuthGroup()));

			// chainPosition
			if (getChainPosition() == 0)
				headerBaos.write(0);
			else {
				headerBaos.write(8);
				dataBaos.write(BytesUtil.longToBytes(getChainPosition()));
			}

			// hash
			len = getLength(getHash());
			headerBaos.write(BytesUtil.shortToBytes((short) (len & 0xffff)));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getHash()));

			// previousHash
			len = getLength(getPreviousHash());
			headerBaos.write(BytesUtil.shortToBytes((short) (len & 0xffff)));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getPreviousHash()));

			// nonce
			len = getLength(getNonce());
			headerBaos.write(BytesUtil.shortToBytes((short) (len & 0xffff)));
			if (len > 0) 
				dataBaos.write(BytesUtil.stringToBytes(getNonce()));

			// difficultyTarget
			if (getDifficultyTarget() == 0)
				headerBaos.write(0);
			else {
				headerBaos.write(4);
				dataBaos.write(BytesUtil.integerToBytes(getDifficultyTarget()));
			}

			// infoType
			len = getLength(getInfoType());
			headerBaos.write((byte) (len & 0xff));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getInfoType()));

			// infoFormat
			len = getLength(getInfoFormat());
			headerBaos.write((byte) (len & 0xff));
			if (len > 0)
				dataBaos.write(BytesUtil.stringToBytes(getInfoFormat()));

			// contextData
			if (getContextData() == null)
				headerBaos.write(BytesUtil.integerToBytes(0));
			else {
				headerBaos.write(BytesUtil.integerToBytes(getContextData().length));
				dataBaos.write(getContextData());
			}

			// contentData
			if (getContentData() == null)
				headerBaos.write(BytesUtil.integerToBytes(0));
			else {
				headerBaos.write(BytesUtil.integerToBytes(getContentData().length));
				dataBaos.write(getContentData());
			}

			ByteArrayOutputStream result = new ByteArrayOutputStream();
			headerBaos.writeTo(result);
			dataBaos.writeTo(result);
			return result.toByteArray();
		} catch (IOException exc) {
			logError(MODULE_NAME, exc.getMessage(), exc);
		}

		return new byte[] {};
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public void decodeBase64(byte[] bytes) {
		Message result;
		try {
			result = new Message(Base64.getDecoder().decode(bytes));
			id = result.id;
			tag = result.tag;
			messageGroupId = result.messageGroupId;
			sequenceNumber = result.sequenceNumber;
			sequenceTotal = result.sequenceTotal;
			priority = result.priority;
			timestamp = result.timestamp;
			publisher = result.publisher;
			authIdentifier = result.authIdentifier;
			authGroup = result.authGroup;
			version = result.version;
			chainPosition = result.chainPosition;
			hash = result.hash;
			previousHash = result.previousHash;
			nonce = result.nonce;
			difficultyTarget = result.difficultyTarget;
			infoType = result.infoType;
			infoFormat = result.infoFormat;
			contextData = result.contextData;
			contentData = result.contentData;
		} catch (Exception exp) {
			logError(MODULE_NAME, exp.getMessage(), exp);
		}
	}

	public JsonObject toJson() {
		return Json.createObjectBuilder()
				.add("id", id == null ? "" : id)
				.add("tag", tag == null ? "" : tag)
				.add("groupid", messageGroupId == null ? "" : messageGroupId)
				.add("sequencenumber", sequenceNumber)
				.add("sequencetotal", sequenceTotal)
				.add("priority", priority)
				.add("timestamp", timestamp)
				.add("publisher", publisher == null ? "" : publisher)
				.add("authid", authIdentifier == null ? "" : authIdentifier)
				.add("authgroup", authGroup == null ? "" : authGroup)
				.add("version", version)
				.add("chainposition", chainPosition)
				.add("hash", hash == null ? "" : hash)
				.add("previoushash", previousHash == null ? "" : previousHash)
				.add("nonce", nonce == null ? "" : nonce)
				.add("difficultytarget", difficultyTarget)
				.add("infotype", infoType == null ? "" : infoType)
				.add("infoformat", infoFormat == null ? "" : infoFormat)
				.add("contextdata", contextData == null ? "" : new String(Base64.getEncoder().encode(contextData)))
				.add("contentdata", contentData == null ? "" : new String(Base64.getEncoder().encode(contentData)))
				.build();
	}

	public byte[] encodeBase64() {
		try {
			return Base64.getEncoder().encode(this.getBytes());
		} catch (Exception exp) {
			logError(MODULE_NAME, exp.getMessage(), exp);
			return new byte[] {};
		}
	}
}
