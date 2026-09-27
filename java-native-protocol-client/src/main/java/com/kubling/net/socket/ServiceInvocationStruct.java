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

package com.kubling.net.socket;

import com.kubling.core.util.ArgCheck;
import com.kubling.core.util.ExternalizeUtil;

import java.io.*;


public final class ServiceInvocationStruct implements Externalizable {

    @Serial
    private static final long serialVersionUID = 1207674062670068350L;
    public Class<?> targetClass;
    public String methodName;
    public Object[] args;

    public ServiceInvocationStruct() {

    }

    public ServiceInvocationStruct(Object[] args, String methodName,
                                   Class<?> targetClass) {
        ArgCheck.isNotNull(methodName);
        ArgCheck.isNotNull(targetClass);
        this.args = args;
        this.methodName = methodName;
        this.targetClass = targetClass;
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        this.targetClass = (Class<?>) in.readObject();
        this.methodName = (String) in.readObject();
        this.args = ExternalizeUtil.readArray(in, Object.class);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(targetClass);
        out.writeObject(methodName);
        ExternalizeUtil.writeArray(out, args);
    }

    @Override
    public String toString() {
        return "Invoke " + targetClass + "." + methodName + " " + args.length;
    }
}