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

package com.kubling.core.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kubling.core.CorePlugin;
import com.kubling.core.KublingRuntimeException;

import java.io.IOException;
import java.io.Reader;
import java.io.Serial;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Objects;


/**
 * This is wrapper on top of clob functionality
 */
public final class JsonType extends BaseClobType implements Comparable<BaseClobType> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private transient JsonNode jsonNode;  // cache

    @Serial
    private static final long serialVersionUID = 2753412502127824104L;

    public JsonType() {
    }

    public JsonType(Clob clob) {
        super(clob);
    }

    @Override
    public int compareTo(BaseClobType o) {
        if (!(o instanceof JsonType)) {
            return super.compareTo(o);
        }
        return this.normalizedJsonString().compareTo(((JsonType) o).normalizedJsonString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof JsonType jsonType) {
            return Objects.equals(this.getParsedNode(), jsonType.getParsedNode());
        } else if (obj instanceof ClobType clobType) {
            return Objects.equals(this.getParsedNode(), new JsonType(clobType).getParsedNode());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return normalizedJsonString().hashCode();
    }

    @Override
    public String toString() {
        return normalizedJsonString();
    }

    private JsonNode getParsedNode() {
        if (jsonNode != null) return jsonNode;
        try (final Reader r = getCharacterStream()) {
            jsonNode = MAPPER.readTree(r);
            return jsonNode;
        } catch (IOException | SQLException e) {
            throw new KublingRuntimeException(CorePlugin.Event.KBL10085, e);
        }
    }

    private String normalizedJsonString() {
        try {
            return MAPPER.writeValueAsString(MAPPER.treeToValue(getParsedNode(), Object.class));
        } catch (IOException e) {
            throw new KublingRuntimeException(CorePlugin.Event.KBL10085, e);
        }
    }
}
