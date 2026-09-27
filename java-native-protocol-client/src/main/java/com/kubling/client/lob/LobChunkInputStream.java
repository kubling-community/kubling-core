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

package com.kubling.client.lob;

import com.kubling.core.CorePlugin;

import java.io.IOException;
import java.io.InputStream;


/**
 * A InputStream wrapper class for a Lob Chunks. Given a stream of Lob Chunks
 * this class will convert those chunks into InputStream, which can be used to
 * stream the lob data.
 */
public class LobChunkInputStream extends InputStream {

    private final LobChunkProducer reader;

    private byte[] byteData = null;
    private int currentCounter = 0;
    private boolean lastChunk = false;
    private boolean closed = false;

    public LobChunkInputStream(LobChunkProducer reader) {
        this.reader = reader;
    }

    public int read() throws IOException {
        if (this.closed) {
            throw new IllegalStateException(CorePlugin.Util.getString("stream_closed"));
        }
        while (this.byteData == null || this.byteData.length <= currentCounter) {
            if (this.lastChunk) {
                // we are done
                return -1;
            }
            LobChunk value = this.reader.getNextChunk();
            this.lastChunk = value.isLast();
            this.byteData = value.getBytes();
            this.currentCounter = 0;
        }

        // so we have data
        return (byteData[currentCounter++] & 0xFF);
    }

    /**
     * @see InputStream#close()
     */
    public void close() throws IOException {
        this.closed = true;
        this.reader.close();
    }

}
