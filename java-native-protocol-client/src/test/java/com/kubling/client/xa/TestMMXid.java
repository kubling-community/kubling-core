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

package com.kubling.client.xa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMMXid {
    private static final XidImpl XID1 = new XidImpl(0, new byte[]{
            1
    }, new byte[0]);
    private static final XidImpl XID2 = new XidImpl(0, new byte[]{
            2
    }, new byte[]{3});
    private static final XidImpl XID1Copy = new XidImpl(0, new byte[]{
            1
    }, new byte[0]);

    @Test
    public void testEquals() {
        assertEquals(XID1, XID1Copy);
        assertNotEquals(XID1, XID2);
    }

    @Test
    public void testCopyConstructor() {
        XidImpl xidcopy = new XidImpl(XID1);
        assertEquals(XID1Copy, xidcopy);
        assertNotSame(XID1Copy, xidcopy);
    }

    @Test
    public void testHashCode() {
        assertEquals(XID1.hashCode(), XID1Copy.hashCode());
        assertNotEquals(XID1.hashCode(), XID2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(XID1Copy.toString(), XID1.toString());
        assertEquals("Kubling-Xid global:1 branch:null format:0", XID1.toString());
        assertEquals("Kubling-Xid global:2 branch:3 format:0", XID2.toString());
    }

}
