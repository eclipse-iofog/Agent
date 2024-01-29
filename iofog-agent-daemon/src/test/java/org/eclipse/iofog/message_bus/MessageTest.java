/*
 * *******************************************************************************
 *  * Copyright (c) 2018-2024 Edgeworx, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private MockedStatic<LoggingService> loggingServiceMockedStatic;


    @BeforeEach
    public void setUp() throws Exception {
        loggingServiceMockedStatic = mockStatic(LoggingService.class);
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
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
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
                .build();
        message = spy(new Message());
    }

    @AfterEach
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
        loggingServiceMockedStatic.close();
        message = null;
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
            MockedConstruction<ByteArrayOutputStream> byteArrayOutputStreamMockedConstruction =
                    Mockito.mockConstructionWithAnswer(ByteArrayOutputStream.class, invocation -> {
                throw new IOException();
            });
            message = spy(new Message(jsonObject));
            byte[] rawByte = message.getBytes();
            assertEquals(0, rawByte.length);
            verify(LoggingService.class);
            LoggingService.logError(eq(MODULE_NAME), eq("Error in getBytes"), any());
            byteArrayOutputStreamMockedConstruction.close();
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
        MockedStatic<Base64> base64 = mockStatic(Base64.class);
        Base64.Decoder decoder = mock(Base64.Decoder.class);
        when(Base64.getDecoder()).thenReturn(decoder);
        Mockito.doThrow(new RuntimeException()).when(decoder).decode( any(byte[].class));
        message.decodeBase64(message.encodeBase64());
        verify(LoggingService.class);
        LoggingService.logError(eq(MODULE_NAME), eq("Error in decodeBase64"), any());
        base64.close();
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
        MockedStatic<Base64> base64 = mockStatic(Base64.class);
        Base64.Encoder encoder = mock(Base64.Encoder.class);
        when(Base64.getEncoder()).thenReturn(encoder);
        Mockito.doThrow(new RuntimeException()).when(encoder).encode( any(byte[].class));
        message.encodeBase64();
        verify(LoggingService.class);
        LoggingService.logError(eq(MODULE_NAME), eq("Error in encodeBase64"), any());
        base64.close();
    }
}