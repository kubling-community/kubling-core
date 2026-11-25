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

package com.kubling.net.socket;

import com.kubling.net.HostInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestOioObjectChannelFactory {

    private static final class CloseAwareBlockingInputStream extends InputStream {
        private final CountDownLatch reading = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public int read() throws IOException {
            reading.countDown();
            try {
                closed.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            throw new IOException("closed");
        }

        @Override
        public void close() {
            closed.countDown();
        }
    }

    @Test
    public void testControlWriteAbortsInProgressPayload() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        ObjectChannel channel = null;
        Socket peer = null;
        try (ServerSocket server = new ServerSocket(0, 1, loopback)) {
            Future<Socket> accepted = executor.submit(server::accept);
            OioObjectChannelFactory factory = new OioObjectChannelFactory(new Properties());
            channel = factory.createObjectChannel(new HostInfo(
                    "localhost", new InetSocketAddress(loopback, server.getLocalPort())));
            peer = accepted.get(1, TimeUnit.SECONDS);

            CloseAwareBlockingInputStream stream = new CloseAwareBlockingInputStream();
            Message payload = new Message();
            payload.setContents(stream);
            ObjectChannel connectedChannel = channel;
            Future<?> payloadWrite = executor.submit(() -> {
                connectedChannel.write(payload).get();
                return null;
            });
            assertTrue(stream.reading.await(1, TimeUnit.SECONDS));

            Future<?> controlWrite = channel.writeControl(new Message());

            assertThrows(ExecutionException.class,
                    () -> controlWrite.get(1, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,
                    () -> payloadWrite.get(1, TimeUnit.SECONDS));
            assertTrue(stream.closed.await(1, TimeUnit.SECONDS));
            assertFalse(channel.isOpen());
            peer.setSoTimeout(1000);
            peer.getInputStream().readAllBytes();
            assertEquals(-1, peer.getInputStream().read());
        } finally {
            if (channel != null) {
                channel.close();
            }
            if (peer != null) {
                peer.close();
            }
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
    }
}
