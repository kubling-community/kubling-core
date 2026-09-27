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

package com.kubling.client.plan;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("nls")
public class TestPlanNode {

    public static PlanNode example1() {
        PlanNode map = new PlanNode("x");
        map.addProperty("test", "");
        map.addProperty("null", (String) null);
        map.addProperty("string", "string");
        List<String> list1 = new ArrayList<>();
        list1.add("item1");
        list1.add("item2");
        list1.add("item3");
        map.addProperty("list<string>", list1);

        PlanNode child = new PlanNode("y");
        List<String> outputCols = new ArrayList<>();
        outputCols.add("Name (string)");
        outputCols.add("Year (integer)");
        child.addProperty("outputCols", outputCols);
        child.addProperty("Join Type", "INNER JOIN");
        List<String> crits = new ArrayList<>();
        crits.add("Item.ID = History.ID");
        child.addProperty("Criteria", crits);
        child.addProperty("Other", new ArrayList<>());
        map.addProperty("child", child);
        return map;
    }

    @Test
    public void testXml() {
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><node name=\"x\"><property name=\"test\"><value></value></property><property name=\"null\"></property><property name=\"string\"><value>string</value></property><property name=\"list&lt;string&gt;\"><value>item1</value><value>item2</value><value>item3</value></property><property name=\"child\"><node name=\"y\"><property name=\"outputCols\"><value>Name (string)</value><value>Year (integer)</value></property><property name=\"Join Type\"><value>INNER JOIN</value></property><property name=\"Criteria\"><value>Item.ID = History.ID</value></property><property name=\"Other\"></property></node></property></node>", example1().toXml());
    }

    @Test
    public void testXmlRoundtrip() throws Exception {
        PlanNode example1 = example1();
        example1.addProperty("last", "x");
        String planString = example1.toXml();
        PlanNode planNode = PlanNode.fromXml(planString);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><node name=\"x\"><property name=\"test\"><value></value></property><property name=\"null\"></property><property name=\"string\"><value>string</value></property><property name=\"list&lt;string&gt;\"><value>item1</value><value>item2</value><value>item3</value></property><property name=\"child\"><node name=\"y\"><property name=\"outputCols\"><value>Name (string)</value><value>Year (integer)</value></property><property name=\"Join Type\"><value>INNER JOIN</value></property><property name=\"Criteria\"><value>Item.ID = History.ID</value></property><property name=\"Other\"></property></node></property><property name=\"last\"><value>x</value></property></node>", planNode.toXml());
    }

    @Test
    public void testText() {
        assertEquals("x\n  + test:\n  + null\n  + string:string\n  + list<string>:\n    0: item1\n    1: item2\n    2: item3\n  + child:\n    y\n      + outputCols:\n        0: Name (string)\n        1: Year (integer)\n      + Join Type:INNER JOIN\n      + Criteria:Item.ID = History.ID\n      + Other\n", example1().toString());
    }

    @Test
    public void testYaml() {
        assertEquals("""
                x:
                  test:\s
                  null: ~
                  string: string
                  list<string>:
                    - item1
                    - item2
                    - item3
                  child:
                    y:
                      outputCols:
                        - Name (string)
                        - Year (integer)
                      Join Type: INNER JOIN
                      Criteria: Item.ID = History.ID
                      Other: ~
                """, example1().toYaml());
    }

}
