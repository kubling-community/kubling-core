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

import com.kubling.core.KublingRuntimeException;
import com.kubling.core.types.XMLType;
import com.kubling.core.util.ExternalizeUtil;
import com.kubling.jdbc.JDBCPlugin;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;


/**
 * A PlanNode represents part of processing plan tree.  For relational plans
 * child PlanNodes may be either subqueries or nodes that feed tuples into the
 * parent. For procedure plans child PlanNodes will be processing instructions,
 * which can in turn contain other relational or procedure plans.
 */
public class PlanNode implements Externalizable {

    /**
     * A Property is a named value of a {@link PlanNode} that may be
     * another {@link PlanNode} or a non-null list of values.
     */
    public static class Property implements Externalizable {
        private String name;
        private List<String> values;
        private PlanNode planNode;

        public Property() {

        }

        public Property(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<String> getValues() {
            return values;
        }

        public void setValues(List<String> values) {
            this.values = values;
        }

        public PlanNode getPlanNode() {
            return planNode;
        }

        public void setPlanNode(PlanNode planNode) {
            this.planNode = planNode;
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
            this.name = (String) in.readObject();
            this.values = ExternalizeUtil.readList(in, String.class);
            this.planNode = (PlanNode) in.readObject();
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(name);
            ExternalizeUtil.writeCollection(out, values);
            out.writeObject(planNode);
        }

    }

    private LinkedHashMap<String, Property> properties = new LinkedHashMap<>();
    private PlanNode parent;
    private String name;

    public PlanNode() {

    }

    public PlanNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    void setParent(PlanNode parent) {
        this.parent = parent;
    }

    public PlanNode getParent() {
        return this.parent;
    }

    public List<Property> getProperties() {
        return new ArrayList<>(properties.values());
    }

    public void addProperty(String pname, PlanNode value) {
        Property p = new Property(pname);
        p.setPlanNode(value);
        value.setParent(this);
        this.properties.put(pname, p);
    }

    public void addProperty(String pname, List<String> value) {
        Property p = new Property(pname);
        if (value == null) {
            value = Collections.emptyList();
        }
        p.setValues(value);
        this.properties.put(pname, p);
    }

    public void addProperty(String pname, String value) {
        Property p = new Property(pname);
        if (value == null) {
            p.setValues(new ArrayList<>(0));
        } else {
            p.setValues(Arrays.asList(value));
        }
        this.properties.put(pname, p);
    }

    /**
     * Converts this PlanNode to XML. See the JAXB bindings for the
     * document form.
     *
     * @return an XML document of this PlanNode
     */
    public String toXml() {
        try {
            XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
            StringWriter stringWriter = new StringWriter();
            XMLStreamWriter writer = outputFactory.createXMLStreamWriter(stringWriter);
            writer.writeStartDocument("UTF-8", "1.0");
            writePlanNode(this, writer);
            writer.writeEndDocument();
            return stringWriter.toString();
        } catch (FactoryConfigurationError e) {
            throw new KublingRuntimeException(JDBCPlugin.Event.KBL20002, e);
        } catch (XMLStreamException e) {
            throw new KublingRuntimeException(JDBCPlugin.Event.KBL20003, e);
        }
    }

    private void writeProperty(Property property, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("property");
        writer.writeAttribute("name", property.getName());
        if (property.getValues() != null) {
            for (String value : property.getValues()) {
                if (value != null) {
                    writeElement(writer, "value", value);
                }
            }
        }
        PlanNode node = property.getPlanNode();
        if (node != null) {
            writePlanNode(node, writer);
        }
        writer.writeEndElement();
    }

    private void writePlanNode(PlanNode node, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("node");
        writer.writeAttribute("name", node.getName());
        for (Property p : node.getProperties()) {
            writeProperty(p, writer);
        }
        writer.writeEndElement();
    }

    private static void writeElement(final XMLStreamWriter writer, String name, String value) throws XMLStreamException {
        writer.writeStartElement(name);
        if (value != null) {
            writer.writeCharacters(value);
        }
        writer.writeEndElement();
    }

    private static Properties getAttributes(XMLStreamReader reader) {
        Properties props = new Properties();
        if (reader.getAttributeCount() > 0) {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                String attrName = reader.getAttributeLocalName(i);
                String attrValue = reader.getAttributeValue(i);
                props.setProperty(attrName, attrValue);
            }
        }
        return props;
    }

    public static PlanNode fromXml(String planString) throws XMLStreamException {

        XMLInputFactory inputFactory = XMLType.getXmlInputFactory();
        XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(planString));

        // TODO XML Support is marked for removal
        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            String element = reader.getLocalName();
            if (element.equals("node")) {
                Properties props = getAttributes(reader);
                PlanNode planNode = new PlanNode(props.getProperty("name"));
                planNode.setParent(null);
                buildNode(reader, planNode);
                return planNode;
            }
            throw new XMLStreamException(JDBCPlugin.Util.gs("unexpected_element", reader.getName(), "node"), reader.getLocation());
        }
        return null;
    }

    private static PlanNode buildNode(XMLStreamReader reader, PlanNode node) throws XMLStreamException {
        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            String property = reader.getLocalName();
            if (property.equals("property")) {
                Properties props = getAttributes(reader);
                ArrayList<String> values = new ArrayList<>();
                while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
                    String valueNode = reader.getLocalName();
                    if (values != null && valueNode.equals("value")) {
                        values.add(reader.getElementText());
                    } else if (valueNode.equals("node")) {
                        values = null;
                        Properties nodeProps = getAttributes(reader);
                        PlanNode childNode = new PlanNode(nodeProps.getProperty("name"));
                        node.addProperty(props.getProperty("name"), buildNode(reader, childNode));
                    } else {
                        throw new XMLStreamException(
                                JDBCPlugin.Util.gs("unexpected_element", reader.getName(), "value"),
                                reader.getLocation());
                    }
                }
                if (values != null) {
                    node.addProperty(props.getProperty("name"), values);
                }
            } else {
                throw new XMLStreamException(
                        JDBCPlugin.Util.gs("unexpected_element", reader.getName(), "property"),
                        reader.getLocation());
            }
        }
        return node;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        visitNode(this, 0, false, builder);
        return builder.toString();
    }

    public String toYaml() {
        StringBuilder builder = new StringBuilder();
        visitNode(this, 0, true, builder);
        return builder.toString();
    }

    protected void visitNode(PlanNode node, int nodeLevel, boolean yaml, StringBuilder text) {
        text.append("  ".repeat(Math.max(0, nodeLevel)));
        text.append(node.getName());
        if (yaml) {
            text.append(":\n");
        } else {
            text.append("\n");
        }

        // Print properties appropriately
        int propTabs = nodeLevel + 1;
        for (Property property : node.getProperties()) {
            // Print leading spaces for prop name
            text.append("  ".repeat(Math.max(0, propTabs)));
            printProperty(nodeLevel, property, yaml, text);
        }
    }

    private void printProperty(int nodeLevel, Property p, boolean yaml, StringBuilder text) {
        if (!yaml) {
            text.append("+ ");
        }
        text.append(p.getName());

        if (p.getPlanNode() != null) {
            text.append(":\n");
            visitNode(p.getPlanNode(), nodeLevel + 2, yaml, text);
        } else if (p.getValues().size() > 1) {
            text.append(":\n");
            for (int i = 0; i < p.getValues().size(); i++) {
                text.append("  ".repeat(Math.max(0, nodeLevel + 2)));
                if (yaml) {
                    text.append("- ");
                } else {
                    text.append(i);
                    text.append(": ");
                }
                text.append(p.getValues().get(i));
                text.append("\n");
            }
        } else if (p.getValues().size() == 1) {
            if (yaml) {
                text.append(": ");
            } else {
                text.append(":");
            }
            text.append(p.getValues().getFirst());
            text.append("\n");
        } else {
            if (yaml) {
                text.append(": ~\n");
            } else {
                text.append("\n");
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        this.properties = new LinkedHashMap<>();
        for (Property p : ExternalizeUtil.readList(in, Property.class)) {
            this.properties.put(p.name, p);
        }
        this.parent = (PlanNode) in.readObject();
        this.name = (String) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        ExternalizeUtil.writeCollection(out, this.properties.values());
        out.writeObject(this.parent);
        out.writeObject(this.name);
    }

    public Property getProperty(String pName) {
        return this.properties.get(pName);
    }

}
