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

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * Provides a minimally concurrent (concurrent read/exclusive write) {@link LinkedHashMap} for use in read mostly situations.
 * <p>
 * Collection views are immutable snapshots. Writes clone the current map and publish the completed clone atomically.
 *
 * @param <K>
 * @param <V>
 */
public class CopyOnWriteLinkedHashMap<K, V> implements Map<K, V>, Serializable {

    @Serial
    private static final long serialVersionUID = -2690353315316696065L;

    @SuppressWarnings("rawtypes")
    private static final LinkedHashMap EMPTY = new LinkedHashMap(2);

    @SuppressWarnings("unchecked")
    private volatile LinkedHashMap<K, V> map = EMPTY;

    @Override
    public V get(Object arg0) {
        return map.get(arg0);
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public synchronized void clear() {
        map = new LinkedHashMap<>();
    }

    @Override
    public boolean containsValue(Object arg0) {
        return map.containsValue(arg0);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableMap(map).entrySet();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public synchronized V put(K arg0, V arg1) {
        @SuppressWarnings("unchecked")
        LinkedHashMap<K, V> next = (LinkedHashMap<K, V>) map.clone();
        V result = next.put(arg0, arg1);
        map = next;
        return result;
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> arg0) {
        if (arg0.isEmpty()) {
            return;
        }
        @SuppressWarnings("unchecked")
        LinkedHashMap<K, V> next = (LinkedHashMap<K, V>) map.clone();
        next.putAll(arg0);
        map = next;
    }

    @Override
    public synchronized V remove(Object arg0) {
        if (map.containsKey(arg0)) {
            @SuppressWarnings("unchecked")
            LinkedHashMap<K, V> next = (LinkedHashMap<K, V>) map.clone();
            V result = next.remove(arg0);
            map = next;
            return result;
        }
        return null;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(map.values());
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return map.equals(obj);
    }

    @Override
    public String toString() {
        return map.toString();
    }

}
