/*
 *
 * Copyright 2016 Wei-Ming Wu
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

package com.kubling.core.json.flattener;

import com.github.wnameless.json.base.JsonPrinter;
import org.apache.commons.text.translate.CharSequenceTranslator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * {@link JsonifyArrayList} is simply a ArrayList but with an override jsonify toString method.
 *
 * @param <E> the type of elements
 * @author Wei-Ming Wu
 */
public class JsonifyArrayList<E> extends ArrayList<E> {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient CharSequenceTranslator translator =
            StringEscapePolicy.DEFAULT.getCharSequenceTranslator();

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        translator = StringEscapePolicy.DEFAULT.getCharSequenceTranslator();
    }

    public JsonifyArrayList() {
    }

    public JsonifyArrayList(Collection<E> coll) {
        super(coll);
    }

    public void setTranslator(CharSequenceTranslator translator) {
        this.translator = translator;
    }

    public String toString(PrintMode printMode) {
        if (Objects.requireNonNull(printMode) == PrintMode.PRETTY) {
            return JsonPrinter.prettyPrint(toString());
        }
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (E e : this) {
            if (e instanceof String) {
                sb.append('"');
                sb.append(translator.translate((String) e));
                sb.append('"');
            } else if (e instanceof Collection) {
                sb.append(new JsonifyArrayList<>((Collection<?>) e));
            } else if (e instanceof Map) {
                sb.append(new JsonifyLinkedHashMap<>((Map<?, ?>) e));
            } else {
                sb.append(e);
            }
            sb.append(',');
        }
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        sb.append(']');

        return sb.toString();
    }

}
