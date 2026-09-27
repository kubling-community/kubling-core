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

import com.kubling.core.BundleUtil;

import java.util.ResourceBundle;

/**
 * JDBCPlugin
 * <p>Used here in <code>jdbc</code> to have access to the new
 * logging framework.
 */
public class JDBCPlugin { // extends Plugin {

    public static final String PLUGIN_ID = "com.kubling.jdbc";

    public static final BundleUtil Util =
            new BundleUtil(PLUGIN_ID, PLUGIN_ID + ".i18n",
                    ResourceBundle.getBundle(PLUGIN_ID + ".i18n"));

    public enum Event implements BundleUtil.Event {
        KBL20000,
        KBL20001,
        KBL20002,
        KBL20003,
        KBL20007,
        KBL20008,
        KBL20009,
        KBL20010,
        KBL20012,
        KBL20013,
        KBL20014,
        KBL20016,
        KBL20018,
        KBL20019,
        KBL20020,
        KBL20021,
        KBL20023,
        KBL20027,
        KBL20028,
        KBL20029,
        KBL20030,
        KBL20031,
        KBL20032,
        KBL20033,
        KBL20034,
        KBL20035,
        KBL20036,
        KBL20037,
        KBL20038,
        KBL20039,
    }
}
