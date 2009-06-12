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
package org.apache.servicemix.camel;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.util.ExchangeHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The binding of how Camel messages get mapped to JBI and back again
 *
 * @version $Revision: 563665 $
 */
public class JbiBinding {

    private static final Log LOG = LogFactory.getLog(JbiBinding.class);

    private String messageExchangePattern;

    /**
     * Extracts the body from the given normalized message
     */
    public Object extractBodyFromJbi(JbiExchange exchange, NormalizedMessage normalizedMessage) {
        // TODO we may wish to turn this into a POJO such as a JAXB/DOM
        return normalizedMessage.getContent();
    }

    public Source convertBodyToJbi(Exchange exchange, Object body) {
        if (body instanceof Source) {
            return (Source) body;
        } else {
            try {
                return ExchangeHelper.convertToType(exchange, Source.class, body);
            } catch (NoTypeConversionAvailableException e) {
                LOG.warn("Unable to convert message body of type " + body.getClass() + " into an XML Source");
                return null;
            }
        }
    }

    public MessageExchange makeJbiMessageExchange(Exchange camelExchange,
                                                  MessageExchangeFactory exchangeFactory, String defaultMep)
        throws MessagingException, URISyntaxException {

        MessageExchange jbiExchange = createJbiMessageExchange(camelExchange, exchangeFactory, defaultMep);
        NormalizedMessage normalizedMessage = jbiExchange.getMessage("in");
        if (normalizedMessage == null) {
            normalizedMessage = jbiExchange.createMessage();
            jbiExchange.setMessage(normalizedMessage, "in");
        }
        normalizedMessage.setContent(getJbiInContent(camelExchange));
        addJbiHeaders(jbiExchange, normalizedMessage, camelExchange.getIn());
        addJbiAttachments(jbiExchange, normalizedMessage, camelExchange);
        return jbiExchange;
    }

    // Properties
    // -------------------------------------------------------------------------

    public String getMessageExchangePattern() {
        return messageExchangePattern;
    }

    /**
     * Sets the message exchange pattern to use for communicating with JBI
     *
     * @param messageExchangePattern
     */
    public void setMessageExchangePattern(String messageExchangePattern) {
        this.messageExchangePattern = messageExchangePattern;
    }

    protected MessageExchange createJbiMessageExchange(Exchange camelExchange,
        MessageExchangeFactory exchangeFactory, String defaultMep)
        throws MessagingException, URISyntaxException {

        // option 1 -- use the MEP that was configured on the endpoint URI
        ExchangePattern mep = ExchangePattern.fromWsdlUri(defaultMep);
        if (mep == null) {
            // option 2 -- use the MEP configured on the ToJbiProcessor
            mep = ExchangePattern.fromWsdlUri(getMessageExchangePattern());
        }
        if (mep == null) {
            // option 3 -- use the MEP from the Camel Exchange
            mep = camelExchange.getPattern();
        }
        MessageExchange answer = null;
        if (mep != null) {
            if (mep == ExchangePattern.InOnly) {
                answer = exchangeFactory.createInOnlyExchange();
            } else if (mep == ExchangePattern.InOptionalOut) {
                answer = exchangeFactory.createInOptionalOutExchange();
            } else if (mep == ExchangePattern.InOut) {
                answer = exchangeFactory.createInOutExchange();
            } else if (mep == ExchangePattern.RobustInOnly) {
                answer = exchangeFactory.createRobustInOnlyExchange();
            } else {
                answer = exchangeFactory.createExchange(new URI(mep.toString()));
            }
        }
        // TODO: this is not really usefull as the out will not be
        // TODO: populated at that time
        if (answer == null) {
            // lets try choose the best MEP based on the camel message
            Message out = camelExchange.getOut(false);
            if (out == null || out.getBody() == null) {
                answer = exchangeFactory.createInOnlyExchange();
            } else {
                answer = exchangeFactory.createInOutExchange();
            }
        }

        if (camelExchange.getProperty("jbi.operation") != null) {

            String operationName = (String) camelExchange.getProperty("jbi.operation");
            QName operationQName = QName.valueOf(operationName);
            answer.setOperation(operationQName);

        }

        return answer;
    }

    protected Source getJbiInContent(Exchange camelExchange) {
        // TODO this should be more smart
        Source content = null;
        try {
            content = camelExchange.getIn().getBody(Source.class);
        } catch (NoTypeConversionAvailableException e) {
            if (camelExchange.getIn().getBody() != null) {
                LOG.warn("'in' message content of type " + camelExchange.getIn().getBody().getClass()
                         + " could not be converted to Source and will be dropped");
            }
        }
        return content;
    }

    protected void addJbiHeaders(MessageExchange jbiExchange, NormalizedMessage normalizedMessage,
                                 Message camelMessage) {
        // get headers from the Camel in message
        Set<Map.Entry<String, Object>> entries = camelMessage.getHeaders().entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            //check if value is Serializable, and if value is Map or collection,
            //just exclude it since the entry of it may not be Serializable as well
            if (entry.getValue() instanceof Serializable
                    && !(entry.getValue() instanceof Map)
                    && !(entry.getValue() instanceof Collection)) {
                normalizedMessage.setProperty(entry.getKey(), entry.getValue());
            }
        }
        
        // if there's a NormalizedMessage inside the Camel Message, copy those headers over as well
        NormalizedMessage camelNormalizedMessage = getNormalizedMessage(camelMessage);
        if (camelNormalizedMessage != null) {
            copyNormalizedMessageHeaders(normalizedMessage, camelNormalizedMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private void copyNormalizedMessageHeaders(NormalizedMessage from, NormalizedMessage to) {
        Set<String> propertyNames = to.getPropertyNames();
        for (String propertyName : propertyNames) {
            if (from.getProperty(propertyName) == null) {
                Object propertyValue = to.getProperty(propertyName);
                //check if value is Serializable, and if value is Map or collection,
                //just exclude it since the entry of it may not be Serializable as well
                if (propertyValue instanceof Serializable
                        && !(propertyValue instanceof Map)
                        && !(propertyValue instanceof Collection)) {
                    from.setProperty(propertyName, propertyValue);
                }
            }
        }
    }

    protected void addJbiAttachments(MessageExchange jbiExchange, NormalizedMessage normalizedMessage,
                                     Exchange camelExchange)
        throws MessagingException {

        Set<Map.Entry<String, DataHandler>> entries = camelExchange.getIn().getAttachments().entrySet();
        for (Map.Entry<String, DataHandler> entry : entries) {
            normalizedMessage.addAttachment(entry.getKey(), entry.getValue());
        }
    }

    protected NormalizedMessage getNormalizedMessage(Message message) {
        if (message instanceof JbiMessage) {
            return ((JbiMessage) message).getNormalizedMessage();
        }
        return null;
    }

}