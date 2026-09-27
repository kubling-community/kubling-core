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

import com.kubling.client.RequestMessage;

/**
 * These execution properties can
 * be set via the {@link KublingStatement#setExecutionProperty(String, String)}
 * method.  They affect the subsequent execution of all commands on that Statement
 * instance.
 * <p>
 * They can also be set using a SET statement via JDBC and take effect for the
 * duration of the session.
 */
public interface ExecutionProperties {

    interface Values {
        /**
         * XML results format:  XML results displayed as a formatted tree
         */
        String XML_TREE_FORMAT = "Tree";

        /**
         * XML results format:  XML results displayed in compact form
         */
        String XML_COMPACT_FORMAT = "Compact";

        /**
         * Transaction auto wrap constant - checks if a command
         * requires a transaction and will be automatically wrap it.
         */
        String TXN_WRAP_DETECT = RequestMessage.TXN_WRAP_DETECT;

        /**
         * Transaction auto wrap constant - never wrap a command execution in a transaction
         */
        String TXN_WRAP_OFF = RequestMessage.TXN_WRAP_OFF;

        /**
         * Transaction auto wrap constant - always wrap commands in a transaction.
         */
        String TXN_WRAP_ON = RequestMessage.TXN_WRAP_ON;
    }

    /**
     * Execution property name for XML format
     */
    String PROP_XML_FORMAT = "XMLFormat";

    /**
     * Execution property name for XML validation
     */
    String PROP_XML_VALIDATION = "XMLValidation";

    /**
     * Execution property name for transaction auto wrap mode
     */
    String PROP_TXN_AUTO_WRAP = "autoCommitTxn";

    /**
     * Execution property name for partial results mode
     */
    String PROP_PARTIAL_RESULTS_MODE = "partialResultsMode";

    /**
     * Whether to use result set cache if it is available
     *
     * @since 4.2
     */
    String RESULT_SET_CACHE_MODE = "resultSetCacheMode";

    /**
     * Default fetch size to use on Statements if the fetch size is not explicitly set.
     * The default is 500.
     *
     * @since 4.2
     */
    String PROP_FETCH_SIZE = "fetchSize";

    /**
     * If true, will ignore autocommit for local transactions.
     *
     * @since 5.5.2
     */
    String DISABLE_LOCAL_TRANSACTIONS = "disableLocalTxn";

    /**
     * Overrides the handling of double-quoted identifiers to allow them to be strings.
     *
     * @since 4.3
     */
    String ANSI_QUOTED_IDENTIFIERS = "ansiQuotedIdentifiers";

    /**
     * Can be one of ON|OFF|DEBUG
     */
    String SQL_OPTION_SHOWPLAN = "SHOWPLAN";

    /**
     * Can be one of ON|OFF
     */
    String NOEXEC = "NOEXEC";

    String QUERYTIMEOUT = "QUERYTIMEOUT";

    /**
     * A change was made in JDBC4 so that when an 'Alias' is used it will
     * now be returned as the label.   Prior to this, it was returned as
     * the name.   Setting this property to <code>false</code> will enable
     * backwards compatibility when JDBC3 and older support is still required.
     * <p>
     * Default is <code>true</code>
     *
     * @since 7.4.1
     */

    String JDBC4COLUMNNAMEANDLABELSEMANTICS = "useJDBC4ColumnNameAndLabelSemantics";


}
