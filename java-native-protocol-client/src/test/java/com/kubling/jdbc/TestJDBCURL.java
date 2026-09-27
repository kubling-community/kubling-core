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

import com.kubling.net.KublingURL;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @since 4.3
 */
public class TestJDBCURL {

    // Need to allow embedded spaces and ='s within optional properties
    @Test
    public final void testCredentials() {
        String credentials =
                URLEncoder.encode("defaultToLogon,(system=BQT1 SQL Server 2000 Simple Cap,user=xyz,password=xyz)",
                        StandardCharsets.UTF_8);
        JDBCURL url =
                new JDBCURL("jdbc:kubling:QT_sqls2kds@mm://slwxp136:43100;credentials=" + credentials);
        Properties p = url.getProperties();
        assertEquals("defaultToLogon,(system=BQT1 SQL Server 2000 Simple Cap,user=xyz,password=xyz)",
                p.getProperty("credentials"));
    }

    @Test
    public void testJDBCURLWithProperties() {
        String URL = "jdbc:kubling:bqt@mm://localhost:12345;version=1;user=%25xyz;password=***;logLevel=1;configFile=testdata/bqt/dqp_stmt_e2e.xmi;disableLocalTxn=true;autoFailover=false";

        Properties expectedProperties = new Properties();
        expectedProperties.setProperty("version", "1");
        expectedProperties.setProperty("user", "%xyz");
        expectedProperties.setProperty("password", "***");
        expectedProperties.setProperty("logLevel", "1");
        expectedProperties.setProperty("configFile", "testdata/bqt/dqp_stmt_e2e.xmi");
        expectedProperties.setProperty(ExecutionProperties.DISABLE_LOCAL_TRANSACTIONS, "true");
        expectedProperties.setProperty(KublingURL.CONNECTION.AUTO_FAILOVER, "false");
        JDBCURL url = new JDBCURL(URL);
        assertEquals("bqt", url.getVDBName());
        assertEquals("mm://localhost:12345", url.getConnectionURL());
        assertEquals(expectedProperties, url.getProperties());
        assertTrue(url.getJDBCURL().contains("user=%25xyz"));
    }

    @Test
    public void testJDBCURLWithoutProperties() {
        String URL = "jdbc:kubling:bqt@mm://localhost:12345";

        JDBCURL url = new JDBCURL(URL);
        assertEquals("bqt", url.getVDBName());
        assertEquals("mm://localhost:12345", url.getConnectionURL());
        assertEquals(new Properties(), url.getProperties());
    }

    @Test
    public void testCaseConversion() {
        // Different case ------------------------------------HERE -v  ----------------and HERE  -v
        String URL = "jdbc:kubling:bqt@mm://localhost:12345;VERSION=1;user=xyz;password=***;configFile=testdata/bqt/dqp_stmt_e2e.xmi";

        Properties expectedProperties = new Properties();
        expectedProperties.setProperty("version", "1");
        expectedProperties.setProperty("user", "xyz");
        expectedProperties.setProperty("password", "***");
        expectedProperties.setProperty("configFile", "testdata/bqt/dqp_stmt_e2e.xmi");
        JDBCURL url = new JDBCURL(URL);
        assertEquals("bqt", url.getVDBName());
        assertEquals("mm://localhost:12345", url.getConnectionURL());
        assertEquals(expectedProperties, url.getProperties());
    }

    @Test
    public void testWithExtraSemicolons() {
        String URL = "jdbc:kubling:bqt@mm://localhost:12345;version=1;user=xyz;password=***;logLevel=1;;;configFile=testdata/bqt/dqp_stmt_e2e.xmi;;";

        Properties expectedProperties = new Properties();
        expectedProperties.setProperty("version", "1");
        expectedProperties.setProperty("user", "xyz");
        expectedProperties.setProperty("password", "***");
        expectedProperties.setProperty("logLevel", "1");
        expectedProperties.setProperty("configFile", "testdata/bqt/dqp_stmt_e2e.xmi");
        JDBCURL url = new JDBCURL(URL);
        assertEquals("bqt", url.getVDBName());
        assertEquals("mm://localhost:12345", url.getConnectionURL());
        assertEquals(expectedProperties, url.getProperties());
    }

    @Test
    public void testWithWhitespace() {
        String URL = "jdbc:kubling:bqt@mm://localhost:12345; version =1;user= xyz ;password=***; logLevel = 1 ; configFile=testdata/bqt/dqp_stmt_e2e.xmi ;";

        Properties expectedProperties = new Properties();
        expectedProperties.setProperty("version", "1");
        expectedProperties.setProperty("user", "xyz");
        expectedProperties.setProperty("password", "***");
        expectedProperties.setProperty("logLevel", "1");
        expectedProperties.setProperty("configFile", "testdata/bqt/dqp_stmt_e2e.xmi");
        JDBCURL url = new JDBCURL(URL);
        assertEquals("bqt", url.getVDBName());
        assertEquals("mm://localhost:12345", url.getConnectionURL());
        assertEquals(expectedProperties, url.getProperties());
    }

    @Test
    public void testNoPropertyValue() {
        String URL = "jdbc:kubling:bqt@mm://localhost:12345;version=1;user=xyz;password=***;logLevel=;configFile=";

        Properties expectedProperties = new Properties();
        expectedProperties.setProperty("version", "1");
        expectedProperties.setProperty("user", "xyz");
        expectedProperties.setProperty("password", "***");
        expectedProperties.setProperty("logLevel", "");
        expectedProperties.setProperty("configFile", "");
        JDBCURL url = new JDBCURL(URL);
        assertEquals("bqt", url.getVDBName());
        assertEquals("mm://localhost:12345", url.getConnectionURL());
        assertEquals(expectedProperties, url.getProperties());
    }

    @Test
    public void testInvalidProtocol() {
        String URL = "jdbc:monkeymatrix:bqt@mm://localhost:12345;version=1;user=xyz;password=***;logLevel=1";
        try {
            new JDBCURL(URL);
            fail("Illegal argument should have failed.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testNoVDBName() {
        String URL = "jdbc:kubling:@mm://localhost:12345;version=1;user=xyz;password=***;logLevel=1";
        try {
            new JDBCURL(URL);
            fail("Illegal argument should have failed.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testNoAtSignInURL() {
        String URL = "jdbc:kubling:bqt!mm://localhost:12345;version=1;user=xyz;password=***;logLevel=1";
        try {
            new JDBCURL(URL);
            // No @ sign is llowed as part of embedded driver now,
            // but this form of URL rejected in the acceptURL
            //fail("Illegal argument should have failed.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testMoreThanOneAtSign() {
        String URL = "jdbc:kubling:bqt@mm://localhost:12345;version=1;user=xy@;password=***;logLevel=1";
        try {
            // this allowed as customer properties can have @ in their properties
            new JDBCURL(URL);
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testNoEqualsInProperty() {
        String URL = "jdbc:kubling:bqt@mm://localhost:12345;version=1;user=xyz;password***;logLevel=1";
        try {
            new JDBCURL(URL);
            fail("Illegal argument should have failed.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testMoreThanOneEqualsInProperty() {
        String URL = "jdbc:kubling:bqt@mm://localhost:12345;version=1;user=xyz;password==***;logLevel=1";
        try {
            new JDBCURL(URL);
            fail("Illegal argument should have failed.");
        } catch (IllegalArgumentException e) {
            // expected
        }
        URL = "jdbc:kubling:bqt@mm://localhost:12345;version=1;user=xyz;password=***=;logLevel=1";
        try {
            new JDBCURL(URL);
            fail("Illegal argument should have failed.");
        } catch (IllegalArgumentException e) {
            // expected
        }
        URL = "jdbc:kubling:bqt@mm://localhost:12345;version=1;user=xyz;=password=***;logLevel=1";
        try {
            new JDBCURL(URL);
            fail("Illegal argument should have failed.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testNoKeyInProperty() {
        String URL = "jdbc:kubling:bqt@mm://localhost:12345;version=1;user=xyz;=***;logLevel=1";
        try {
            new JDBCURL(URL);
            fail("Illegal argument should have failed.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testConstructor() {
        JDBCURL url = new JDBCURL("myVDB", "mm://myhost:12345", null);
        assertEquals("jdbc:kubling:myVDB@mm://myhost:12345", url.getJDBCURL());

        Properties props = new Properties();
        props.setProperty(BaseDataSource.USER_NAME, "myuser");
        props.setProperty(BaseDataSource.PASSWORD, "mypassword");
        props.put("ClieNTtOKeN", 1);
        url = new JDBCURL("myVDB", "mm://myhost:12345", props);
        assertEquals("jdbc:kubling:myVDB@mm://myhost:12345;password=mypassword;user=myuser", url.getJDBCURL());
    }

    @Test
    public void testConstructor_Exception() {
        try {
            new JDBCURL(null, "myhost", null);
            fail("Should have failed.");
        } catch (Exception e) {

        }
        try {
            new JDBCURL("  ", "myhost", null);
            fail("Should have failed.");
        } catch (Exception e) {

        }

        try {
            // in embedded situation there is no connection url
            new JDBCURL("myVDB", "  ", null);
        } catch (Exception e) {

        }
    }

    @Test
    public void testNormalize() {
        Properties props = new Properties();
        props.setProperty("UsEr", "myuser");
        props.setProperty("pAssWOrD", "mypassword");
        props.put("ClieNTtOKeN", 1);
        JDBCURL.normalizeProperties(props);
        assertEquals("myuser", props.getProperty(BaseDataSource.USER_NAME));
        assertEquals("mypassword", props.getProperty(BaseDataSource.PASSWORD));
    }

    @Test
    public final void testEncodedPropertyProperties() {
        String password = "=@#^&*()+!%$^%@#_-)_~{}||\\`':;,./<>?password has = & %";
        Properties props = new Properties();
        props.setProperty("UsEr", "foo");
        props.setProperty("PASswoRd", password);
        JDBCURL.normalizeProperties(props);

        assertEquals(password, props.getProperty("password"));
        assertEquals("foo", props.getProperty("user"));
    }

    @Test
    public final void testEncodedPropertyInURL() {
        String password = "=@#^&*()+!%$^%@#_-)_~{}||\\`':;,./<>?password has = & %";
        String encPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
        JDBCURL url = new JDBCURL("jdbc:kubling:QT_sqls2kds@mm://slwxp136:43100;PASswoRd=" + encPassword);
        Properties p = url.getProperties();
        assertEquals(password, p.getProperty("password"));
    }


    @Test
    public void testGetServerURL_NoProperties() {
        String result = new JDBCURL("jdbc:kubling:designtimecatalog@mm://slwxp172:44401;user=ddifranco;password=mm").getConnectionURL();
        assertEquals("mm://slwxp172:44401", result);
    }

    @Test
    public void testGetServerURL_Properties() {
        String result = new JDBCURL("jdbc:kubling:designtimecatalog@mm://slwxp172:44401;user=ddifranco;password=mm").getConnectionURL();
        assertEquals("mm://slwxp172:44401", result);
    }

    /**
     * Test getServerURL with a valid URL and password that contains at least
     * one ASCII character in the range of 32 to 126 excluding the ; and = sign.
     *
     * @since 5.0.2
     */
    @Test
    public void testGetServerURL_PasswordProperties() {
        String result;
        String srcURL = "jdbc:kubling:designtimecatalog@mm://slwxp172:44401;user=ddifranco;password=";
        String password;
        String tgtURL = "mm://slwxp172:44401";


        for (char ch = 32; ch <= 126; ch++) {
            //exclude URL reserved characters
            if (ch != ';' && ch != '=' && ch != '%') {
                password = ch + "mm";
                result = new JDBCURL(srcURL + URLEncoder.encode(password, StandardCharsets.UTF_8)).getConnectionURL();
                assertEquals(tgtURL, result,
                        "Failed to obtain correct ServerURL when using password " + password);
            }
        }

    }

    @Test
    public void testGetServerURL_2Servers() {
        String result = new JDBCURL("jdbc:kubling:designtimecatalog@mm://slwxp172:44401,slabc123:12345;user=ddifranco;password=mm")
                .getConnectionURL();
        assertEquals("mm://slwxp172:44401,slabc123:12345", result);
    }

    @Test
    public void testBuildEmbeedURL() {
        JDBCURL url = new JDBCURL("vdb", "/home/foo/deploy.properties", new Properties());
        assertEquals("jdbc:kubling:vdb@/home/foo/deploy.properties", url.getJDBCURL());

        Properties p = new Properties();
        p.setProperty("user", "test");
        p.setProperty("password", "pass");
        p.setProperty("autoFailover", "true");
        p.setProperty("any", "thing");

        url = new JDBCURL("vdb", "/home/foo/deploy.properties", p);
        assertTrue(url.getJDBCURL().startsWith("jdbc:kubling:vdb@/home/foo/deploy.properties;"));
        assertTrue(url.getJDBCURL().contains("any=thing"));
        assertTrue(url.getJDBCURL().contains("password=pass"));
        assertTrue(url.getJDBCURL().contains("autoFailover=true"));

    }

    @Test
    public void testUnicodeName() {
        String result = new JDBCURL("jdbc:kubling:%E4%BD%A0%E5%A5%BD").getVDBName();
        assertEquals("你好", result);
        result = new JDBCURL("jdbc:kubling:你好").getVDBName();
        assertEquals("你好", result);
    }

    @Test
    public void testEncoding() {
        JDBCURL url = new JDBCURL("jdbc:kubling:a%40b@mm://%50;%55=a");
        assertEquals("a@b", url.getVDBName());
        assertEquals("mm://P", url.getConnectionURL());
        assertEquals("U", url.getProperties().entrySet().iterator().next().getKey());
    }

}
