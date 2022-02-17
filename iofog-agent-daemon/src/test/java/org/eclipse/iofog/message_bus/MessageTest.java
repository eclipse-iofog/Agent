/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2022 Edgeworx, Inc.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.message_bus;

import org.eclipse.iofog.utils.logging.LoggingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author nehanaithani
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Message.class, Base64.class, LoggingService.class, ByteArrayOutputStream.class})
public class MessageTest {
    private short VERSION;
    private String MODULE_NAME;
    private Message message;
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
    private JsonObject jsonObject;
    private JsonObjectBuilder jsonObjectBuilder = null;


    @Before
    public void setUp() throws Exception {
        mockStatic(LoggingService.class);
        MODULE_NAME = "Message";
        VERSION = 4;
        byte[] byteArray = new byte[] { (byte)0xe0, (byte)0xf4 };
        version = 0;
        id = "id";
        tag = "tag";
        messageGroupId = "messageGroupId";
        sequenceNumber = 1;
        sequenceTotal = 10;
        priority = 1;
        timestamp = currentTimeMillis();
        publisher = "publisher";
        authIdentifier = "authIdentifier";
        authGroup = "authGroup";
        chainPosition = 5;
        hash = "hash";
        previousHash = "previousHash";
        nonce = "nonce";
        difficultyTarget = 2;
        infoType = "infoType";
        infoFormat = "infoFormat";
        message = spy(new Message());
        String content = "contentData";
        String context = "contextData";
        contentData = Base64.getDecoder().decode(content.getBytes(UTF_8));
        contextData = Base64.getDecoder().decode(context.getBytes(UTF_8));
        jsonObjectBuilder = Json.createObjectBuilder();
        jsonObject = jsonObjectBuilder.add("id", id)
                .add("tag",tag )
                .add("groupid", messageGroupId)
                .add("sequencenumber", sequenceNumber)
                .add("sequencetotal", sequenceTotal)
                .add("priority", priority)
                .add("timestamp", timestamp)
                .add("publisher", publisher)
                .add("authid", authIdentifier)
                .add("authgroup", authGroup)
                .add("chainposition", chainPosition)
                .add("hash", hash)
                .add("previoushash", previousHash)
                .add("nonce", nonce)
                .add("difficultytarget", difficultyTarget)
                .add("infotype", infoType)
                .add("infoformat", infoFormat)
                .add("contentdata", content)
                .add("contextdata", context).build();

    }

    @After
    public void tearDown() throws Exception {
        MODULE_NAME = null;
        VERSION = 0;
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

    /**
     * Test Message constructor with Json parameter
     */
    @Test
    public void constructorWithJsonArgument() {
        message = spy(new Message(jsonObject));
        assertEquals(id, message.getId());
        assertEquals(tag, message.getTag());
        assertEquals(messageGroupId, message.getMessageGroupId());
        assertEquals(sequenceNumber, message.getSequenceNumber());
        assertEquals(sequenceTotal, message.getSequenceTotal());
        assertEquals(priority, message.getPriority());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals(publisher, message.getPublisher());
        assertEquals(authIdentifier, message.getAuthIdentifier());
        assertEquals(authGroup, message.getAuthGroup());
        assertEquals(chainPosition, message.getChainPosition());
        assertEquals(hash, message.getHash());
        assertEquals(previousHash, message.getPreviousHash());
        assertEquals(nonce, message.getNonce());
        assertEquals(difficultyTarget, message.getDifficultyTarget());
        assertEquals(infoType, message.getInfoType());
        assertEquals(infoFormat, message.getInfoFormat());
    }

    /**
     * Test getter And Setter of Id
     */
    @Test
    public void testGetterAndSetterId() {
        message.setId(id);
        assertEquals(id, message.getId());
    }

    /**
     * Test getter And Setter of tag
     */
    @Test
    public void testGetterAndSetterTag() {
        message.setTag(tag);
        assertEquals(tag, message.getTag());
    }

    /**
     * Test getter And Setter of MessageGroupId
     */
    @Test
    public void testGetterAndSetterMessageGroupId() {
        message.setMessageGroupId(messageGroupId);
        assertEquals(messageGroupId, message.getMessageGroupId());
    }

    /**
     * Test getter and setter of sequenceNumber
     */
    @Test
    public void testGetterAndSetterSequenceNumber() {
        message.setSequenceNumber(sequenceNumber);
        assertEquals(sequenceNumber, message.getSequenceNumber());
    }

    /**
     * Test getter and setter of sequenceTotal
     */
    @Test
    public void testGetterAndSetterSequenceTotal() {
        message.setSequenceTotal(sequenceTotal);
        assertEquals(sequenceTotal, message.getSequenceTotal());
    }

    /**
     * Test getter and setter of priority
     */
    @Test
    public void testGetterAndSetterPriority() {
        message.setPriority(priority);
        assertEquals(priority, message.getPriority());
    }

    /**
     * Test getter and setter of timeStamp
     */
    @Test
    public void testGetterAndSetterTimestamp() {
        message.setTimestamp(timestamp);
        assertEquals(timestamp, message.getTimestamp());
    }

    /**
     * Test getter and setter of Publisher
     */
    @Test
    public void testGetterAndSetterPublisher() {
        message.setPublisher(publisher);
        assertEquals(publisher, message.getPublisher());
    }

    /**
     * Test getter and setter of authIdentifier
     */
    @Test
    public void testGetterAndSetterAuthIdentifier() {
        message.setAuthIdentifier(authIdentifier);
        assertEquals(authIdentifier, message.getAuthIdentifier());
    }

    /**
     * Test getter and setter of AuthGroup
     */
    @Test
    public void testGetterAndSetterAuthGroup() {
        message.setAuthGroup(authGroup);
        assertEquals(authGroup, message.getAuthGroup());
    }

    /**
     * Test getter and setter of version
     */
    @Test
    public void testGetterAndSetterVersion() {
        assertEquals(VERSION, message.getVersion());
    }

    /**
     * Test getter and setter of chainPosition
     */
    @Test
    public void testGetterAndSetterChainPosition() {
        message.setChainPosition(chainPosition);
        assertEquals(chainPosition, message.getChainPosition());
    }

    /**
     * Test getter and setter of hash
     */
    @Test
    public void testGetterAndSetterHash() {
        message.setHash(hash);
        assertEquals(hash, message.getHash());
    }

    /**
     * Test getter and setter of previous hash
     */
    @Test
    public void testGetterAndSetterPreviousHash() {
        message.setPreviousHash(previousHash);
        assertEquals(previousHash, message.getPreviousHash());
    }

    /**
     * Test getter and setter of nonce
     */
    @Test
    public void testGetterAndSetterNonce() {
        message.setNonce(nonce);
        assertEquals(nonce, message.getNonce());
    }

    /**
     * Test getter and setter of difficultyTarget
     */
    @Test
    public void testGetterAndSetterDifficultyTarget() {
        message.setDifficultyTarget(difficultyTarget);
        assertEquals(difficultyTarget, message.getDifficultyTarget());
    }

    /**
     * Test getter and setter of infoType
     */
    @Test
    public void testGetterAndSetterInfoType() {
        message.setInfoType(infoType);
        assertEquals(infoType, message.getInfoType());
    }

    /**
     * Test getter and setter of InfoFormat
     */
    @Test
    public void testGetterAndSetterInfoFormat() {
        message.setInfoFormat(infoFormat);
        assertEquals(infoFormat, message.getInfoFormat());
    }

    /**
     * Test getter and setter of ContextData
     */
    @Test
    public void testGetterAndSetterContextData() {
        message.setContextData(contextData);
        assertEquals(contextData, message.getContextData());
    }

    /**
     * Test getter and setter of ContentData
     */
    @Test
    public void testGetterAndSetterContentData() {
        message.setContentData(contentData);
        assertEquals(contentData, message.getContentData());
    }

    /**
     * Test getter and setter of bytes
     */
    @Test
    public void testBytes() {
        message = spy(new Message(jsonObject));
        byte[] rawByte = message.getBytes();
        message = spy(new Message(rawByte));
        assertEquals(id, message.getId());
        assertEquals(tag, message.getTag());
        assertEquals(messageGroupId, message.getMessageGroupId());
        assertEquals(sequenceNumber, message.getSequenceNumber());
        assertEquals(sequenceTotal, message.getSequenceTotal());
        assertEquals(priority, message.getPriority());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals(publisher, message.getPublisher());
        assertEquals(authIdentifier, message.getAuthIdentifier());
        assertEquals(authGroup, message.getAuthGroup());
        assertEquals(chainPosition, message.getChainPosition());
        assertEquals(hash, message.getHash());
        assertEquals(previousHash, message.getPreviousHash());
        assertEquals(nonce, message.getNonce());
        assertEquals(difficultyTarget, message.getDifficultyTarget());
        assertEquals(infoType, message.getInfoType());
        assertEquals(infoFormat, message.getInfoFormat());

    }

    /**
     * Test throws exception when ByteArrayOutputStream object is created
     */
    @Test
    public void throwsExceptionWhenByteArrayOutputStreamIsCreatedInBytes() {
        try {
            whenNew(ByteArrayOutputStream.class).withNoArguments().thenThrow(mock(IOException.class) );
            message = spy(new Message(jsonObject));
            byte[] rawByte = message.getBytes();
            assertTrue(rawByte.length == 0);
            verifyStatic(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error in getBytes"), any());
        } catch (Exception e) {
            fail("This should never happen");
        }
    }

    /**
     * Test toJson when Message constructor is without argument
     */
    @Test
    public void testJsonWithMessageConstructorWithNoArguments() {
        assertTrue(message.toJson().containsKey("version"));
        assertTrue(message.toJson().containsKey("publisher"));
        assertTrue(message.toJson().containsKey("groupid"));
        assertEquals("", message.toJson().getString("groupid"));
        assertEquals(VERSION, message.toJson().getInt("version"));
        assertEquals("", message.toJson().getString("publisher"));
        assertEquals("", message.toJson().getString("infotype"));
    }

    /**
     * Test toJson when Message constructor is without argument JsonObject
     */
    @Test
    public void testJsonWithMessageConstructorWithJsonArgument() {
        message = spy(new Message(jsonObject));
        assertTrue(message.toJson().containsKey("version"));
        assertTrue(message.toJson().containsKey("publisher"));
        assertTrue(message.toJson().containsKey("groupid"));
        assertEquals(messageGroupId, message.toJson().getString("groupid"));
        assertEquals(version, message.toJson().getInt("version"));
        assertEquals(publisher, message.toJson().getString("publisher"));
        assertEquals(infoFormat, message.toJson().getString("infoformat"));
        assertEquals(previousHash, message.toJson().getString("previoushash"));
        assertEquals(hash, message.toJson().getString("hash"));
    }

    /**
     * Test toString
     */
    @Test
    public void testToString() {
        assertFalse(message.toString().contains("@"));
        message = spy(new Message(jsonObject));
        assertFalse(message.toString().contains("@"));
    }

    /**
     * Test decodeBase64
     */
    @Test
    public void testDecodeBase64() {
        message.decodeBase64(message.encodeBase64());
        assertNotEquals(hash, message.getHash());
    }

    /**
     * Test decodeBase64 throws Exception
     */
    @Test
    public void throwsExceptionWhenDecodeBase64() {
        mockStatic(Base64.class);
        Base64.Decoder decoder = mock(Base64.Decoder.class);
        when(Base64.getDecoder()).thenReturn(decoder);
        PowerMockito.doThrow(new RuntimeException()).when(decoder).decode( any(byte[].class));
        message.decodeBase64(message.encodeBase64());
        verifyStatic(LoggingService.class);
        LoggingService.logError(eq(MODULE_NAME), eq("Error in decodeBase64"), any());
    }

    /**
     * Test encodeBase64
     */
    @Test
    public void testEncodeBase64() {
        assertNotNull(message.encodeBase64());
    }

    /**
     * Test encodeBase64 throws Exception
     */
    @Test
    public void throwsExceptionWhenEncodeBase64() {
        mockStatic(Base64.class);
        Base64.Encoder encoder = mock(Base64.Encoder.class);
        when(Base64.getEncoder()).thenReturn(encoder);
        PowerMockito.doThrow(new RuntimeException()).when(encoder).encode( any(byte[].class));
        message.encodeBase64();
        verifyStatic(LoggingService.class);
        LoggingService.logError(eq(MODULE_NAME), eq("Error in encodeBase64"), any());
    }
}