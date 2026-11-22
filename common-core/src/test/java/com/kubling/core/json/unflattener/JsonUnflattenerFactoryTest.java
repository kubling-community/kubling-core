/*
 *
 * Copyright 2022 Wei-Ming Wu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
/*
 * This file was modified as part of the Kubling project.
 */

package com.kubling.core.json.unflattener;

import com.github.wnameless.json.base.GsonJsonCore;
import com.github.wnameless.json.base.Jackson3JsonCore;
import com.github.wnameless.json.base.JsonCore;
import com.github.wnameless.json.base.JsonPrinter;
import com.kubling.core.json.flattener.FlattenMode;
import com.kubling.core.json.flattener.PrintMode;
import com.kubling.query.unittest.ResourcesUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class JsonUnflattenerFactoryTest {

    Consumer<JsonUnflattener> configurer;
    JsonCore<?> jsonCore;
    JsonUnflattenerFactory jsonUnflattenerFactory;

    static String expectedJson;

    @BeforeAll
    public static void init() throws IOException {
        expectedJson = JsonPrinter.prettyPrint(ResourcesUtil.getClassPathResource("test_mongo.json"));
    }

    @BeforeEach
    public void setUp() {
        configurer = ju -> {
            ju.withFlattenMode(FlattenMode.MONGODB);
            ju.withPrintMode(PrintMode.PRETTY);
        };
        jsonCore = new GsonJsonCore();
        jsonUnflattenerFactory = new JsonUnflattenerFactory(configurer, jsonCore);
    }

    @Test
    public void testBuildWithJSONString() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test_mongo_flattened.json");

        JsonUnflattener ju = jsonUnflattenerFactory.build(json);
        assertEquals(expectedJson, ju.unflatten());

        jsonUnflattenerFactory = new JsonUnflattenerFactory(configurer);
        ju = jsonUnflattenerFactory.build(json);
        assertEquals(expectedJson, ju.unflatten());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildWithMap() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test_mongo_flattened.json");

        JsonUnflattener ju = jsonUnflattenerFactory
                .build((Map<String, ?>) JsonMapper.builder().build().readValue(json, Map.class));
        assertEquals(expectedJson, ju.unflatten());

        jsonUnflattenerFactory = new JsonUnflattenerFactory(configurer);
        ju = jsonUnflattenerFactory
                .build((Map<String, ?>) JsonMapper.builder().build().readValue(json, Map.class));
        assertEquals(expectedJson, ju.unflatten());
    }

    @Test
    public void testBuildWithJsonReader() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test_mongo_flattened.json");

        JsonUnflattener ju = jsonUnflattenerFactory.build(new StringReader(json));
        assertEquals(expectedJson, ju.unflatten());

        jsonUnflattenerFactory = new JsonUnflattenerFactory(configurer);
        ju = jsonUnflattenerFactory.build(new StringReader(json));
        assertEquals(expectedJson, ju.unflatten());
    }

    @Test
    public void testHashCode() {
        int result = 27;
        result = 31 * result + configurer.hashCode();
        result = 31 * result + jsonCore.hashCode();
        assertEquals(result, jsonUnflattenerFactory.hashCode());

        configurer = ju -> ju.withPrintMode(PrintMode.PRETTY);
        jsonCore = new Jackson3JsonCore();
        jsonUnflattenerFactory = new JsonUnflattenerFactory(configurer, jsonCore);
        assertNotEquals(result, jsonUnflattenerFactory.hashCode());
    }

    @Test
    public void testEquals() {

        JsonUnflattenerFactory otherJsonUnflattenerFactory =
                new JsonUnflattenerFactory(configurer, jsonCore);
        assertEquals(jsonUnflattenerFactory, otherJsonUnflattenerFactory);

        otherJsonUnflattenerFactory = new JsonUnflattenerFactory(configurer);
        assertNotEquals(jsonUnflattenerFactory, otherJsonUnflattenerFactory);

        jsonCore = new Jackson3JsonCore();
        otherJsonUnflattenerFactory = new JsonUnflattenerFactory(configurer, jsonCore);
        assertNotEquals(jsonUnflattenerFactory, otherJsonUnflattenerFactory);

        configurer = ju -> ju.withPrintMode(PrintMode.MINIMAL);
        otherJsonUnflattenerFactory = new JsonUnflattenerFactory(configurer, jsonCore);
        assertNotEquals(jsonUnflattenerFactory, otherJsonUnflattenerFactory);

        assertNotEquals(null, jsonUnflattenerFactory);
    }

    @Test
    public void testToString() {
        jsonUnflattenerFactory = new JsonUnflattenerFactory(configurer);

        assertEquals("JsonUnflattenerFactory{configurer=" + configurer.toString() + ", jsonCore="
                + Optional.empty() + "}", jsonUnflattenerFactory.toString());

        jsonCore = new GsonJsonCore();
        jsonUnflattenerFactory = new JsonUnflattenerFactory(configurer, jsonCore);
        assertEquals("JsonUnflattenerFactory{configurer=" + configurer.toString() + ", jsonCore="
                + Optional.of(jsonCore) + "}", jsonUnflattenerFactory.toString());
    }

}
