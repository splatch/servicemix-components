/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.http;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
import javax.jbi.component.InstallationContext;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Base class for components bootstrap.
 * 
 * @author Guillaume Nodet
 * @version $Revision$
 * @since 3.0
 */
public class HttpBootstrap implements Bootstrap {

    protected InstallationContext context;
    protected ObjectName mbeanName;
    protected HttpConfiguration configuration;

    public HttpBootstrap() {
    }

    public ObjectName getExtensionMBeanName() {
        return mbeanName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jbi.component.Bootstrap#init(javax.jbi.component.InstallationContext)
     */
    public void init(InstallationContext installContext) throws JBIException {
        try {
            this.context = installContext;
            doInit();
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException("Error calling init", e);
        }
    }

    protected void doInit() throws Exception {
        configuration = new HttpConfiguration();
        configuration.setRootDir(this.context.getInstallRoot());
        configuration.setComponentName(this.context.getComponentName());
        configuration.load();
        mbeanName = this.context.getContext().getMBeanNames().createCustomComponentMBeanName("bootstrap");
        MBeanServer server = this.context.getContext().getMBeanServer();
        if (server == null) {
            throw new JBIException("null mBeanServer");
        }
        if (server.isRegistered(this.mbeanName)) {
            server.unregisterMBean(this.mbeanName);
        }
        server.registerMBean(configuration, this.mbeanName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jbi.component.Bootstrap#cleanUp()
     */
    public void cleanUp() throws JBIException {
        try {
            doCleanUp();
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException("Error calling cleanUp", e);
        }
    }

    protected void doCleanUp() throws Exception {
        if (this.mbeanName != null) {
            MBeanServer server = this.context.getContext().getMBeanServer();
            if (server == null) {
                throw new JBIException("null mBeanServer");
            }
            if (server.isRegistered(this.mbeanName)) {
                server.unregisterMBean(this.mbeanName);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jbi.component.Bootstrap#onInstall()
     */
    public void onInstall() throws JBIException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jbi.component.Bootstrap#onUninstall()
     */
    public void onUninstall() throws JBIException {
    }

}
