/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file was modified as part of the Kubling project.
 */

package com.kubling.net.socket;

import com.kubling.core.util.UnitTestUtil;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("nls")
public class TestHandshake {

    @Test
    @SuppressWarnings("deprecation")
    public void testVersion26_2Compatibility() throws Exception {
        // Written by kubling-client 26.2. Add a new versioned fixture for a future compatibility boundary.
        Handshake handshake;
        try (ObjectInputStream in = new ObjectInputStream(
                new FileInputStream(UnitTestUtil.getTestDataFile("handshake-26.2.ser")))) {
            handshake = (Handshake) in.readObject();
        }

        assertEquals("26.02", handshake.getVersion());
        assertEquals(AuthenticationType.USERPASSWORD, handshake.getAuthType());
        assertArrayEquals(new byte[]{1, 2, 3}, handshake.getPublicKey());
        assertArrayEquals(new byte[]{4, 5, 6, 7}, handshake.getPublicKeyLarge());
        assertTrue(handshake.isCbc());
    }

    @Test
    public void testVersionNormalization() throws Exception {
        Handshake hs = new Handshake("11.2.3.a");
        assertEquals("11.02.03.a", hs.getVersion());
    }

}
