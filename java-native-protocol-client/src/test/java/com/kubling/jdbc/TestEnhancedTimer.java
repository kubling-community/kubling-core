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

package com.kubling.jdbc;

import com.kubling.core.util.Assertion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
public class TestEnhancedTimer {

    private static final class SimpleCancelTask implements Runnable {
        @Override
        public void run() {
        }
    }

    @Test
    public void testRemove() {
        EnhancedTimer ct = new EnhancedTimer("foo");
        SimpleCancelTask sct = new SimpleCancelTask();
        EnhancedTimer.Task tt = ct.add(sct, 20000);
        EnhancedTimer.Task tt1 = ct.add(sct, 20000);
        Assertion.assertTrue(tt.compareTo(tt1) < 0);
        EnhancedTimer.Task tt2 = ct.add(sct, 10000);
        Assertions.assertEquals(3, ct.getQueueSize());
        tt.cancel();
        tt1.cancel();
        tt2.cancel();
        Assertions.assertEquals(0, ct.getQueueSize());
    }

}
