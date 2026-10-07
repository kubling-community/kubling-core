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

package com.kubling.core.util;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestCopyOnWriteLinkedHashMap {

    @Test
    public void testEntryCannotMutatePublishedMap() {
        CopyOnWriteLinkedHashMap<String, Integer> map = new CopyOnWriteLinkedHashMap<>();
        map.put("one", 1);

        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();

        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(2));
        assertEquals(1, map.get("one"));
    }

    @Test
    public void testViewIsSnapshotAcrossWrites() {
        CopyOnWriteLinkedHashMap<String, Integer> map = new CopyOnWriteLinkedHashMap<>();
        map.put("one", 1);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        map.put("two", 2);

        assertEquals(Set.of(Map.entry("one", 1)), entries);
        assertEquals(Set.of(Map.entry("one", 1), Map.entry("two", 2)), map.entrySet());
    }
}
