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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author nehanaithani
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MessageIdGeneratorTest {
    private MessageIdGenerator messageIdGenerator;

    @BeforeEach
    public void setUp() throws Exception {
        messageIdGenerator = spy(new MessageIdGenerator());
    }

    @AfterEach
    public void tearDown() throws Exception {
        reset(messageIdGenerator);
    }

    /**
     * Test generate
     */
    @Test
    public void testGenerate() {
        try {
            assertNotNull("Message Id not null",
                    messageIdGenerator.generate(currentTimeMillis()));
        } catch (Exception e) {
            fail("This should not happen");
        }
    }

    /**
     * Test getNextId
     */
    @Test
    public void testGetNextId() {
        assertNotNull("Next Id", messageIdGenerator.getNextId());
        assertFalse(messageIdGenerator.getNextId().contains("?"));
    }
}