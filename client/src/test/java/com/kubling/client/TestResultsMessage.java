/*
 * Copyright Kubling and/or its affiliates
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

package com.kubling.client;

import com.kubling.client.plan.Annotation;
import com.kubling.client.plan.PlanNode;
import com.kubling.client.util.ExceptionHolder;
import com.kubling.core.KublingException;
import com.kubling.core.util.ExternalizeUtil;
import com.kubling.core.util.UnitTestUtil;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"nls", "unchecked"})
public class TestResultsMessage {

    @Test
    public void testMultipleResultMetadataRoundTrip() throws Exception {
        ResultsMessage message = message();
        message.setResultId(37);
        message.setHasMoreResults(true);
        message.setDelayDeserialization(true);

        ResultsMessage copy = UnitTestUtil.helpSerialize(message);
        copy.processResults();

        assertEquals(37, copy.getResultId());
        assertTrue(copy.hasMoreResults());
        assertEquals(message.getResultsList(), copy.getResultsList());
    }

    @Test
    public void testNewReaderAcceptsLegacyPayload() throws Exception {
        LegacyWriterResultsMessage message = new LegacyWriterResultsMessage();
        initialize(message);

        LegacyWriterResultsMessage copy = UnitTestUtil.helpSerialize(message);

        assertEquals(ResultsMessage.LEGACY_RESULT_ID, copy.getResultId());
        assertFalse(copy.hasMoreResults());
        assertEquals(message.getResultsList(), copy.getResultsList());
    }

    @Test
    public void testLegacyReaderIgnoresMultipleResultExtension() throws Exception {
        LegacyReaderResultsMessage message = new LegacyReaderResultsMessage();
        initialize(message);
        message.setResultId(91);
        message.setHasMoreResults(true);

        LegacyReaderResultsMessage copy = UnitTestUtil.helpSerialize(message);

        assertEquals(ResultsMessage.LEGACY_RESULT_ID, copy.getResultId());
        assertFalse(copy.hasMoreResults());
        assertEquals(message.getResultsList(), copy.getResultsList());
    }

    private static ResultsMessage message() {
        ResultsMessage message = new ResultsMessage();
        initialize(message);
        return message;
    }

    private static void initialize(ResultsMessage message) {
        message.setColumnNames(new String[]{"value"});
        message.setDataTypes(new String[]{"integer"});
        message.setResults(new List<?>[]{List.of(1)});
        message.setFirstRow(1);
        message.setLastRow(1);
        message.setFinalRow(1);
    }

    /**
     * Writes the ResultsMessage format that predates multiple-result metadata.
     */
    public static final class LegacyWriterResultsMessage extends ResultsMessage {
        @Serial
        private static final long serialVersionUID = 1L;

        public LegacyWriterResultsMessage() {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            ExternalizeUtil.writeArray(out, getColumnNames());
            ExternalizeUtil.writeArray(out, getDataTypes());
            BatchSerializer.writeBatch(
                    out, getDataTypes(), getResultsList(), getClientSerializationVersion());
            out.writeObject(getPlanDescription());
            out.writeObject(getException() == null ? null : new ExceptionHolder(getException()));
            out.writeObject(getWarnings() == null
                    ? null : ExceptionHolder.toExceptionHolders(getWarnings()));
            out.writeInt(getFirstRow());
            out.writeInt(getLastRow());
            out.writeInt(getFinalRow());
            ExternalizeUtil.writeList(out, getParameters());
            out.writeObject(getDebugLog());
            ExternalizeUtil.writeCollection(out, getAnnotations());
            out.writeBoolean(isUpdateResult());
            if (isUpdateResult()) {
                out.writeInt(getUpdateCount());
            }
        }
    }

    /**
     * Reads only the ResultsMessage format that predates the extension.
     */
    public static final class LegacyReaderResultsMessage extends ResultsMessage {
        @Serial
        private static final long serialVersionUID = 1L;

        public LegacyReaderResultsMessage() {
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            setColumnNames(ExternalizeUtil.readStringArray(in));
            setDataTypes(ExternalizeUtil.readStringArray(in));
            setResults(BatchSerializer.readBatch(in, getDataTypes()));
            setPlanDescription((PlanNode) in.readObject());
            ExceptionHolder exception = (ExceptionHolder) in.readObject();
            if (exception != null) {
                setException((KublingException) exception.getException());
            }
            List<ExceptionHolder> warnings = (List<ExceptionHolder>) in.readObject();
            if (warnings != null) {
                setWarnings(ExceptionHolder.toThrowables(warnings));
            }
            setFirstRow(in.readInt());
            setLastRow(in.readInt());
            setFinalRow(in.readInt());
            setParameters(ExternalizeUtil.readList(in, com.kubling.client.metadata.ParameterInfo.class));
            setDebugLog((String) in.readObject());
            setAnnotations(ExternalizeUtil.readList(in, Annotation.class));
            setUpdateResult(in.readBoolean());
            if (isUpdateResult()) {
                try {
                    setUpdateCount(in.readInt());
                } catch (OptionalDataException | EOFException e) {
                    // Compatibility with update messages that predate updateCount.
                }
            }
        }
    }
}
