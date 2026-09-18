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

package com.kubling.client;

import com.kubling.client.metadata.ParameterInfo;
import com.kubling.client.plan.Annotation;
import com.kubling.client.plan.PlanNode;
import com.kubling.client.util.ExceptionHolder;
import com.kubling.core.KublingException;
import com.kubling.core.util.ExternalizeUtil;
import com.kubling.core.util.MultiArrayOutputStream;
import com.kubling.jdbc.KublingSQLException;
import com.kubling.netty.handler.codec.serialization.CompactObjectInputStream;
import com.kubling.netty.handler.codec.serialization.CompactObjectOutputStream;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * Results Message, used by MMStatement to get the query results.
 */
public class ResultsMessage implements Externalizable {

    @Serial
    private static final long serialVersionUID = 3546924172976187793L;

    /**
     * Identifies a result produced by a server that does not support the
     * multiple-results protocol extension.
     */
    public static final long LEGACY_RESULT_ID = -1;

    private static final byte MULTIPLE_RESULTS_VERSION = 1;

    private List<? extends List<?>> results;
    private String[] columnNames;
    private String[] dataTypes;

    /**
     * A description of planning that occurred as requested in the request.
     */
    private PlanNode planDescription;

    /**
     * An exception that occurred.
     */
    private KublingException exception;

    /**
     * Warning could be schema validation errors or partial results warnings
     */
    private List<Throwable> warnings;

    /**
     * First row index
     */
    private int firstRow = 0;

    /**
     * Last row index
     */
    private int lastRow;

    /**
     * Final row index in complete result set, if known
     */
    private int finalRow = -1;

    /**
     * The parameters of a Stored Procedure
     */
    private List<ParameterInfo> parameters;

    /**
     * OPTION DEBUG log if OPTION DEBUG was used
     */
    private String debugLog;

    private byte clientSerializationVersion;

    /**
     * Query plan annotations, if OPTION SHOWPLAN or OPTION PLANONLY was used:
     * Collection of Object[] where each Object[] holds annotation information
     * that can be used to create an Annotation implementation in JDBC.
     */
    private Collection<Annotation> annotations;

    private boolean isUpdateResult;
    private int updateCount = -1;

    /**
     * Server-defined identifier used to page and advance this result without
     * re-executing its command.
     */
    private long resultId = LEGACY_RESULT_ID;

    /**
     * Indicates that the request has another heterogeneous JDBC result.
     */
    private boolean hasMoreResults;

    private boolean delayDeserialization;
    byte[] resultBytes;

    private MultiArrayOutputStream serializationBuffer;

    public ResultsMessage() {
    }

    public ResultsMessage(List<? extends List<?>> results, String[] columnNames, String[] dataTypes) {
        this.results = results;
        setFirstRow(1);
        setLastRow(results.size());

        this.columnNames = columnNames;
        this.dataTypes = dataTypes;
    }

    public List<? extends List<?>> getResultsList() {
        return results;
    }

    public void processResults() throws KublingSQLException {
        if (results == null && resultBytes != null) {
            try {
                CompactObjectInputStream ois = new CompactObjectInputStream(
                        new ByteArrayInputStream(resultBytes), ResultsMessage.class.getClassLoader());
                results = BatchSerializer.readBatch(ois, dataTypes);
            } catch (IOException | ClassNotFoundException e) {
                throw KublingSQLException.create(e);
            } finally {
                resultBytes = null;
            }
        }
    }

    public void setResults(List<?>[] results) {
        this.results = Arrays.asList(results);
    }

    public void setResults(List<? extends List<?>> results) {
        this.results = results;
    }

    public String[] getColumnNames() {
        return this.columnNames;
    }

    public String[] getDataTypes() {
        return this.dataTypes;
    }

    public KublingException getException() {
        return exception;
    }

    public int getFinalRow() {
        return finalRow;
    }

    public int getFirstRow() {
        return firstRow;
    }

    public int getLastRow() {
        return lastRow;
    }

    public PlanNode getPlanDescription() {
        return planDescription;
    }

    public List<Throwable> getWarnings() {
        return warnings;
    }

    public void setException(Throwable e) {
        if (e instanceof KublingException) {
            this.exception = (KublingException) e;
        } else {
            this.exception = new KublingException(e, e.getMessage());
        }
    }

    public void setFinalRow(int i) {
        finalRow = i;
    }

    public void setFirstRow(int i) {
        firstRow = i;
    }

    public void setLastRow(int i) {
        lastRow = i;
    }

    public void setPlanDescription(PlanNode object) {
        planDescription = object;
    }

    public void setWarnings(List<Throwable> list) {
        warnings = list;
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterInfo> list) {
        parameters = list;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public void setDataTypes(String[] dataTypes) {
        this.dataTypes = dataTypes;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        columnNames = ExternalizeUtil.readStringArray(in);
        dataTypes = ExternalizeUtil.readStringArray(in);

        // Row data
        results = BatchSerializer.readBatch(in, dataTypes);

        // Plan Descriptions
        planDescription = (PlanNode) in.readObject();

        ExceptionHolder holder = (ExceptionHolder) in.readObject();
        if (holder != null) {
            this.exception = (KublingException) holder.getException();
        }

        //delayed deserialization
        if (results == null && this.exception == null) {
            int length = in.readInt();
            resultBytes = new byte[length];
            in.readFully(resultBytes);
        }

        List<ExceptionHolder> holderList = (List<ExceptionHolder>) in.readObject();
        if (holderList != null) {
            this.warnings = ExceptionHolder.toThrowables(holderList);
        }

        firstRow = in.readInt();
        lastRow = in.readInt();
        finalRow = in.readInt();

        //Parameters
        parameters = ExternalizeUtil.readList(in, ParameterInfo.class);

        debugLog = (String) in.readObject();
        annotations = ExternalizeUtil.readList(in, Annotation.class);
        isUpdateResult = in.readBoolean();
        if (isUpdateResult) {
            try {
                updateCount = in.readInt();
            } catch (OptionalDataException | EOFException e) {
                // Ignored
            }
        }
        try {
            byte version = in.readByte();
            if (version == MULTIPLE_RESULTS_VERSION) {
                resultId = in.readLong();
                hasMoreResults = in.readBoolean();
            }
        } catch (OptionalDataException | EOFException e) {
            // Multiple-result metadata was added after the original wire format.
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        ExternalizeUtil.writeArray(out, columnNames);
        ExternalizeUtil.writeArray(out, dataTypes);

        // Results data
        if (delayDeserialization) {
            BatchSerializer.writeBatch(out, dataTypes, null, clientSerializationVersion);
        } else {
            BatchSerializer.writeBatch(out, dataTypes, results, clientSerializationVersion);
        }

        // Plan descriptions
        out.writeObject(this.planDescription);

        if (exception != null) {
            out.writeObject(new ExceptionHolder(exception));
        } else {
            out.writeObject(null);
        }

        if (delayDeserialization && results != null) {
            serialize(true);
            out.writeInt(serializationBuffer.getCount());
            serializationBuffer.writeTo(out);
            serializationBuffer = null;
        }

        if (this.warnings != null) {
            out.writeObject(ExceptionHolder.toExceptionHolders(this.warnings));
        } else {
            out.writeObject(null);
        }

        out.writeInt(firstRow);
        out.writeInt(lastRow);
        out.writeInt(finalRow);

        // Parameters
        ExternalizeUtil.writeList(out, parameters);

        out.writeObject(debugLog);
        ExternalizeUtil.writeCollection(out, annotations);
        out.writeBoolean(isUpdateResult);
        if (isUpdateResult) {
            out.writeInt(updateCount);
        }
        out.writeByte(MULTIPLE_RESULTS_VERSION);
        out.writeLong(resultId);
        out.writeBoolean(hasMoreResults);
    }

    /**
     * Serialize the result data
     *
     * @return the size of the data bytes
     */
    public int serialize(boolean keepSerialization) throws IOException {
        if (serializationBuffer == null) {
            serializationBuffer = new MultiArrayOutputStream(1 << 13);
            CompactObjectOutputStream oos = new CompactObjectOutputStream(serializationBuffer);
            BatchSerializer.writeBatch(oos, dataTypes, results, clientSerializationVersion);
            oos.close();
        }
        int result = serializationBuffer.getCount();
        if (!keepSerialization) {
            serializationBuffer = null;
        }
        return result;
    }

    public Collection<Annotation> getAnnotations() {
        return annotations;
    }

    public String getDebugLog() {
        return debugLog;
    }

    public void setAnnotations(Collection<Annotation> collection) {
        annotations = collection;
    }

    public void setDebugLog(String string) {
        debugLog = string;
    }


    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "ResultsMessage rowCount=" +
                (results == null ? 0 : results.size()) +
                " finalRow=" +
                finalRow;
    }

    public void setUpdateResult(boolean isUpdateResult) {
        this.isUpdateResult = isUpdateResult;
    }

    public boolean isUpdateResult() {
        return isUpdateResult;
    }

    public byte getClientSerializationVersion() {
        return clientSerializationVersion;
    }

    public void setClientSerializationVersion(byte clientSerializationVersion) {
        this.clientSerializationVersion = clientSerializationVersion;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    /**
     * Get the server-defined identifier for this result.
     *
     * @return the identifier, or {@link #LEGACY_RESULT_ID} for a result from a
     * server that does not implement the multiple-results extension
     */
    public long getResultId() {
        return resultId;
    }

    /**
     * Set the server-defined identifier for this result.
     *
     * @param resultId the identifier used by result continuation requests
     */
    public void setResultId(long resultId) {
        this.resultId = resultId;
    }

    /**
     * Determine whether this request has another JDBC result after this one.
     *
     * @return {@code true} if a subsequent result can be requested
     */
    public boolean hasMoreResults() {
        return hasMoreResults;
    }

    /**
     * Set whether this request has another JDBC result after this one.
     *
     * @param hasMoreResults {@code true} when a subsequent result exists
     */
    public void setHasMoreResults(boolean hasMoreResults) {
        this.hasMoreResults = hasMoreResults;
    }

    public void setDelayDeserialization(boolean delayDeserialization) {
        this.delayDeserialization = delayDeserialization;
    }
}
