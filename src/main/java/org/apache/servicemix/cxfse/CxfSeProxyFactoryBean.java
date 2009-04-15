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
package org.apache.servicemix.cxfse;

import java.util.List;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.jbi.JBITransportFactory;
import org.apache.servicemix.cxfse.interceptors.AttachmentInInterceptor;
import org.apache.servicemix.cxfse.interceptors.AttachmentOutInterceptor;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.api.ClientFactory;
import org.apache.servicemix.jbi.api.Container;
import org.apache.servicemix.jbi.api.ServiceMixClient;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 
 * @author ffang
 * @org.apache.xbean.XBean element="proxy" description="A CXF proxy"
 * 
 */
public class CxfSeProxyFactoryBean implements FactoryBean, InitializingBean,
        DisposableBean {

    private String name = ClientFactory.DEFAULT_JNDI_NAME;

    private Container container;

    private ClientFactory factory;

    private ComponentContext context;

    private Class type;

    private Object proxy;

    private QName service;

    private QName interfaceName;

    private String endpoint;

    private boolean propagateSecuritySubject;

    private ServiceMixClient client;
    
    private boolean useJBIWrapper = true;
    
    private boolean useSOAPEnvelope = true;

    private boolean mtomEnabled;

    public Object getObject() throws Exception {
        if (proxy == null) {
            proxy = createProxy();
        }
        return proxy;
    }

    private Object createProxy() throws Exception {
        JaxWsProxyFactoryBean cf = new JaxWsProxyFactoryBean();
        cf.setServiceName(getService());
        if (getEndpoint() != null) {
            cf.setEndpointName(new QName(getService().getNamespaceURI(), getEndpoint()));
        }
        cf.setServiceClass(type);
        cf.setAddress("jbi://" + new IdGenerator().generateSanitizedId());
        if (isUseJBIWrapper()) {
            cf.setBindingId(org.apache.cxf.binding.jbi.JBIConstants.NS_JBI_BINDING);
        }
        Bus bus = BusFactory.getDefaultBus();
        JBITransportFactory jbiTransportFactory = (JBITransportFactory) bus
                .getExtension(ConduitInitiatorManager.class)
                .getConduitInitiator(JBITransportFactory.TRANSPORT_ID);
        if (getInternalContext() != null) { 
            DeliveryChannel dc = getInternalContext().getDeliveryChannel();
            if (dc != null) {
                jbiTransportFactory.setDeliveryChannel(dc);
            }
        }
        Object proxy = cf.create();
        if (!isUseJBIWrapper() && !isUseSOAPEnvelope()) {
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getInInterceptors(), 
				"ReadHeadersInterceptor");
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getInFaultInterceptors(), 
        		"ReadHeadersInterceptor");
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getOutInterceptors(), 
				"SoapOutInterceptor");
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getOutFaultInterceptors(), 
				"SoapOutInterceptor");
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getOutInterceptors(), 
        		"StaxOutInterceptor");
        }
        if (isMtomEnabled()) {
            ClientProxy.getClient(proxy).getEndpoint()
                .getBinding().getInInterceptors().add(new AttachmentInInterceptor());
            ClientProxy.getClient(proxy).getEndpoint()
                .getBinding().getOutInterceptors().add(new AttachmentOutInterceptor());
        }
        return proxy;
    }

    private void removeInterceptor(List<Interceptor> interceptors, String whichInterceptor) {
		for (Interceptor interceptor : interceptors) {
			if (interceptor.getClass().getName().endsWith(whichInterceptor)) {
				interceptors.remove(interceptor);
			}
		}
	}
    
    public Class getObjectType() {
        return type;
    }

    public boolean isSingleton() {
        return true;
    }

    protected ComponentContext getInternalContext() throws Exception {
        if (context == null) {
            if (factory == null) {
                if (container != null) {
                    factory = container.getClientFactory();
                } else {
                    factory = (ClientFactory) new InitialContext().lookup(name);
                }
            }
            client = factory.createClient();
            context = client.getContext();
        }
        return context;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpointName) {
        this.endpoint = endpointName;
    }

    public QName getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(QName interfaceName) {
        this.interfaceName = interfaceName;
    }

    public QName getService() {
        return service;
    }

    public void setService(QName service) {
        this.service = service;
    }

    /**
     * @return the context
     */
    public ComponentContext getContext() {
        return context;
    }

    /**
     * @param context
     *            the context to set
     */
    public void setContext(ComponentContext context) {
        this.context = context;
    }

    /**
     * @return the container
     */
    public Container getContainer() {
        return container;
    }

    /**
     * @param container
     *            the container to set
     */
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * @return the factory
     */
    public ClientFactory getFactory() {
        return factory;
    }

    /**
     * @param factory
     *            the factory to set
     */
    public void setFactory(ClientFactory factory) {
        this.factory = factory;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the propagateSecuritySubject
     */
    public boolean isPropagateSecuritySubject() {
        return propagateSecuritySubject;
    }

    /**
     * @param propagateSecuritySubject
     *            the propagateSecuritySubject to set
     */
    public void setPropagateSecuritySubject(boolean propagateSecuritySubject) {
        this.propagateSecuritySubject = propagateSecuritySubject;
    }

    public void afterPropertiesSet() throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("type must be set");
        }
    }

    public void destroy() throws Exception {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    /**
     * Specifies if the endpoint expects messages that are encased in the 
     * JBI wrapper used for SOAP messages. Ignore the value of useSOAPEnvelope 
     * if useJBIWrapper is true
     *
     * @org.apache.xbean.Property description="Specifies if the endpoint expects to receive the JBI wrapper in the message received from the NMR. The  default is <code>true</code>.
     * 			Ignore the value of useSOAPEnvelope if useJBIWrapper is true"
     * */
    public void setUseJBIWrapper(boolean useJBIWrapper) {
        this.useJBIWrapper = useJBIWrapper;
    }

    public boolean isUseJBIWrapper() {
        return useJBIWrapper;
    }
    
    /**
     * Specifies if the endpoint expects soap messages when useJBIWrapper is false, 
     * if useJBIWrapper is true then ignore useSOAPEnvelope
     *
     * @org.apache.xbean.Property description="Specifies if the endpoint expects soap messages when useJBIWrapper is false, 
     * 				if useJBIWrapper is true then ignore useSOAPEnvelope. The  default is <code>true</code>.
     * */
	public void setUseSOAPEnvelope(boolean useSOAPEnvelope) {
		this.useSOAPEnvelope = useSOAPEnvelope;
	}

	public boolean isUseSOAPEnvelope() {
		return useSOAPEnvelope;
	}

        public void setMtomEnabled(boolean mtomEnabled) {
            this.mtomEnabled = mtomEnabled;
        }

        public boolean isMtomEnabled() {
            return mtomEnabled;
        }


}
