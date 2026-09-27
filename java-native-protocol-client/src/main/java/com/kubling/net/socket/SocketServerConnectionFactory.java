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

import com.kubling.core.util.FilesystemHelper;
import com.kubling.core.util.PropertiesUtils;
import com.kubling.jdbc.JDBCPlugin;
import com.kubling.net.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.vfs2.FileSystemException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * Responsible for creating socket based connections.
 * The comm approach is object based and layered.  Connections manage failover and identity.
 * ServerInstances represent the service layer to a particular cluster member.  ObjectChannels
 * abstract the underlying IO.
 */
public class SocketServerConnectionFactory implements ServerConnectionFactory, SocketServerInstanceFactory {

    private static final Logger log = Logger.getLogger("com.kubling.net.sockets");

    static final String PROPERTIES_FILENAME = "connectionPropsFilePath";

    private static volatile SocketServerConnectionFactory INSTANCE;

    private ObjectChannelFactory channelFactory;

    private final DefaultHostnameResolver resolver = new DefaultHostnameResolver();

    //config properties
    private long synchronousTtl = 240000L;

    private SocketServerConnectionFactory() {}

    public static synchronized SocketServerConnectionFactory getInstance(Properties props) {

        if (INSTANCE == null) {
            INSTANCE = new SocketServerConnectionFactory();
            InputStream is = getConnectionClientSettings(
                    ObjectUtils.firstNonNull(
                            props.getProperty(PROPERTIES_FILENAME),
                            StringUtils.EMPTY));
            if (is != null) {
                Properties newProps = new Properties();
                try {
                    newProps.load(is);
                } catch (IOException e) {
                    // nothing to do here
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // nothing to do here
                    }
                }
                newProps.putAll(props);
                props = newProps;
            }
            INSTANCE.initialize(props);
        }
        return INSTANCE;
    }

    public void initialize(Properties info) {
        PropertiesUtils.setBeanProperties(this, info, "com.kubling.sockets", true);
        this.channelFactory = new OioObjectChannelFactory(info);
    }

    @Override
    public SocketServerInstance getServerInstance(HostInfo info) throws CommunicationException, IOException {
        SocketServerInstanceImpl ssii =
                new SocketServerInstanceImpl(info, getSynchronousTtl(), this.channelFactory.getSoTimeout());
        ssii.connect(this.channelFactory);
        return ssii;
    }

    /**
     * @param connectionProperties will be updated with additional information before logon
     */
    public SocketServerConnection getConnection(Properties connectionProperties)
            throws CommunicationException, ConnectionException {

        KublingURL url;
        try {
            url = new KublingURL(connectionProperties.getProperty(KublingURL.CONNECTION.SERVER_URL));
        } catch (MalformedURLException e1) {
            throw new ConnectionException(JDBCPlugin.Event.KBL20014, e1, e1.getMessage());
        }

        UrlServerDiscovery discovery = new UrlServerDiscovery();

        discovery.init(url, connectionProperties);

        return new SocketServerConnection(this, url.isUsingSSL(), discovery, connectionProperties);
    }

    public long getSynchronousTtl() {
        return synchronousTtl;
    }

    public void setSynchronousTtl(long synchronousTTL) {
        this.synchronousTtl = synchronousTTL;
    }

    @Override
    public String resolveHostname(InetAddress addr) {
        // only wait 100 milliseconds by default, this could be made configurable if needed
        return resolver.resolve(addr, 100);
    }

    private static InputStream getConnectionClientSettings(String location) {

        location = StringUtils.defaultIfEmpty(location, "/kubling-client-settings.properties");

        InputStream is = SocketServerConnectionFactory.class.getResourceAsStream(location);
        if (is != null) {
            return is;
        }

        try {
            var fileObject = FilesystemHelper.getManager().resolveFile(location);
            if (Objects.nonNull(fileObject) && fileObject.exists()) {
                return fileObject.getContent().getInputStream();
            } else {
                log.warning(JDBCPlugin.Util.getString("SocketServerConnectionFactory.invalid_config_file", location));
            }
        } catch (FileSystemException e) {
            log.severe("Failed to load client settings from file system: " + ExceptionUtils.getRootCauseMessage(e));
        }

        return null;
    }

}
