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
package org.apache.servicemix.jms.endpoints;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.RobustInOnly;
import javax.jbi.messaging.InOut;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.MessageListener;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.jms.JmsEndpointType;
import org.apache.servicemix.store.Store;
import org.apache.servicemix.store.StoreFactory;
import org.apache.servicemix.store.memory.MemoryStoreFactory;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.JmsTemplate102;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.listener.AbstractPollingMessageListenerContainer;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer102;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

/**
 * A JMS provider endpoint
 *
 * @author gnodet
 * @org.apache.xbean.XBean element="provider"
 * @since 3.2
 */
public class JmsProviderEndpoint extends ProviderEndpoint implements JmsEndpointType {

    private static final String MSG_SELECTOR_START = "JMSCorrelationID='";    
    private static final String MSG_SELECTOR_END = "'";

    private JmsProviderMarshaler marshaler = new DefaultProviderMarshaler();
    private DestinationChooser destinationChooser = new SimpleDestinationChooser();
    private DestinationChooser replyDestinationChooser = new SimpleDestinationChooser();
    private JmsTemplate template;

    private boolean jms102;
    private ConnectionFactory connectionFactory;
    private boolean pubSubDomain;
    private DestinationResolver destinationResolver = new DynamicDestinationResolver();
    private Destination destination;
    private String destinationName;
    private boolean messageIdEnabled = true;
    private boolean messageTimestampEnabled = true;
    private boolean pubSubNoLocal;
    private long receiveTimeout = JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT;
    private boolean explicitQosEnabled;
    private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
    private int priority = Message.DEFAULT_PRIORITY;
    private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;

    private Destination replyDestination;
    private String replyDestinationName;

    private StoreFactory storeFactory;
    private Store store;

    private AbstractMessageListenerContainer listenerContainer;

    /**
     * @return the destination
     */
    public Destination getDestination() {
        return destination;
    }

    /**
     * @param destination the destination to set
     */
    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    /**
     * @return the destinationName
     */
    public String getDestinationName() {
        return destinationName;
    }

    /**
     * @param destinationName the destinationName to set
     */
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    /**
     * @return the jms102
     */
    public boolean isJms102() {
        return jms102;
    }

    /**
     * @param jms102 the jms102 to set
     */
    public void setJms102(boolean jms102) {
        this.jms102 = jms102;
    }

    /**
     * @return the connectionFactory
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * @param connectionFactory the connectionFactory to set
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * @return the deliveryMode
     */
    public int getDeliveryMode() {
        return deliveryMode;
    }

    /**
     * @param deliveryMode the deliveryMode to set
     */
    public void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    /**
     * @return the destinationChooser
     */
    public DestinationChooser getDestinationChooser() {
        return destinationChooser;
    }

    /**
     * @param destinationChooser the destinationChooser to set
     */
    public void setDestinationChooser(DestinationChooser destinationChooser) {
        if (destinationChooser == null) {
            throw new NullPointerException("destinationChooser is null");
        }
        this.destinationChooser = destinationChooser;
    }

    /**
     * @return the destinationChooser
     */
    public DestinationChooser getReplyDestinationChooser() {
        return replyDestinationChooser;
    }

    /**
     * @param replyDestinationChooser the replyDestinationChooser to set
     */
    public void setReplyDestinationChooser(DestinationChooser replyDestinationChooser) {
        this.replyDestinationChooser = replyDestinationChooser;
    }
    /**
     * @return the destinationResolver
     */
    public DestinationResolver getDestinationResolver() {
        return destinationResolver;
    }

    /**
     * @param destinationResolver the destinationResolver to set
     */
    public void setDestinationResolver(DestinationResolver destinationResolver) {
        this.destinationResolver = destinationResolver;
    }

    /**
     * @return the explicitQosEnabled
     */
    public boolean isExplicitQosEnabled() {
        return explicitQosEnabled;
    }

    /**
     * @param explicitQosEnabled the explicitQosEnabled to set
     */
    public void setExplicitQosEnabled(boolean explicitQosEnabled) {
        this.explicitQosEnabled = explicitQosEnabled;
    }

    /**
     * @return the marshaler
     */
    public JmsProviderMarshaler getMarshaler() {
        return marshaler;
    }

    /**
     * @param marshaler the marshaler to set
     */
    public void setMarshaler(JmsProviderMarshaler marshaler) {
        if (marshaler == null) {
            throw new NullPointerException("marshaler is null");
        }
        this.marshaler = marshaler;
    }

    /**
     * @return the messageIdEnabled
     */
    public boolean isMessageIdEnabled() {
        return messageIdEnabled;
    }

    /**
     * @param messageIdEnabled the messageIdEnabled to set
     */
    public void setMessageIdEnabled(boolean messageIdEnabled) {
        this.messageIdEnabled = messageIdEnabled;
    }

    /**
     * @return the messageTimestampEnabled
     */
    public boolean isMessageTimestampEnabled() {
        return messageTimestampEnabled;
    }

    /**
     * @param messageTimestampEnabled the messageTimestampEnabled to set
     */
    public void setMessageTimestampEnabled(boolean messageTimestampEnabled) {
        this.messageTimestampEnabled = messageTimestampEnabled;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * @return the pubSubDomain
     */
    public boolean isPubSubDomain() {
        return pubSubDomain;
    }

    /**
     * @param pubSubDomain the pubSubDomain to set
     */
    public void setPubSubDomain(boolean pubSubDomain) {
        this.pubSubDomain = pubSubDomain;
    }

    /**
     * @return the pubSubNoLocal
     */
    public boolean isPubSubNoLocal() {
        return pubSubNoLocal;
    }

    /**
     * @param pubSubNoLocal the pubSubNoLocal to set
     */
    public void setPubSubNoLocal(boolean pubSubNoLocal) {
        this.pubSubNoLocal = pubSubNoLocal;
    }

    /**
     * @return the receiveTimeout
     */
    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    /**
     * @param receiveTimeout the receiveTimeout to set
     */
    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    /**
     * @return the timeToLive
     */
    public long getTimeToLive() {
        return timeToLive;
    }

    /**
     * @param timeToLive the timeToLive to set
     */
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public StoreFactory getStoreFactory() {
        return storeFactory;
    }

    /**
     * Sets the store factory used to create the store.
     * If none is set, a {@link MemoryStoreFactory} will be created and used instead.
     *
     * @param storeFactory
     */
    public void setStoreFactory(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    public Store getStore() {
        return store;
    }

    /**
     * Sets the store used to store JBI exchanges that are waiting for a response
     * JMS message.  The store will be automatically created if not set.
     *
     * @param store
     */
    public void setStore(Store store) {
        this.store = store;
    }

    public Destination getReplyDestination() {
        return replyDestination;
    }

    /**
     * Sets the reply destination.
     * This JMS destination will be used as the default destination for the response
     * messages when using an InOut JBI exchange.  It will be used if the
     * <code>replyDestinationChooser</code> does not return any value.
     *
     * @param replyDestination
     */
    public void setReplyDestination(Destination replyDestination) {
        this.replyDestination = replyDestination;
    }

    public String getReplyDestinationName() {
        return replyDestinationName;
    }

    /**
     * Sets the name of the reply destination.
     * This property will be used to create the <code>replyDestination</code>
     * using the <code>destinationResolver</code> when the endpoint starts if
     * the <code>replyDestination</code> has not been set.
     *
     * @param replyDestinationName
     */
    public void setReplyDestinationName(String replyDestinationName) {
        this.replyDestinationName = replyDestinationName;
    }

    /**
     * Process the incoming JBI exchange
     * @param exchange
     * @throws Exception
     */
    public void process(MessageExchange exchange) throws Exception {
        // The component acts as a provider, this means that another component has requested our service
        // As this exchange is active, this is either an in or a fault (out are sent by this component)
        if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
            // Exchange is finished
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                return;
            // Exchange has been aborted with an exception
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                return;
            // Exchange is active
            } else {
                NormalizedMessage in;
                // Fault message
                if (exchange.getFault() != null) {
                    done(exchange);
                // In message
                } else if ((in = exchange.getMessage("in")) != null) {
                    if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
                        processInOnly(exchange, in);
                        done(exchange);
                    }
                    else {
                        processInOut(exchange, in);
                    }
                // This is not compliant with the default MEPs
                } else {
                    throw new IllegalStateException("Provider exchange is ACTIVE, but no in or fault is provided");
                }
            }
        // Unsupported role: this should never happen has we never create exchanges
        } else {
            throw new IllegalStateException("Unsupported role: " + exchange.getRole());
        }
    }

    /**
     * Process an InOnly or RobustInOnly exchange.
     * This method uses the JMS template to create a session and call the
     * {@link #processInOnlyInSession(javax.jbi.messaging.MessageExchange, javax.jbi.messaging.NormalizedMessage, javax.jms.Session)}
     * method.
     *
     * @param exchange
     * @param in
     * @throws Exception
     */
    protected void processInOnly(final MessageExchange exchange,
                                 final NormalizedMessage in) throws Exception {
        SessionCallback callback = new SessionCallback() {
          public Object doInJms(Session session) throws JMSException {
              try {
                  processInOnlyInSession(exchange, in, session);
                  return null;
              } catch (JMSException e) {
                  throw e;
              } catch (RuntimeException e) {
                  throw e;
              } catch (Exception e) {
                  throw new UncategorizedJmsException(e);
              }
          }
        };
        template.execute(callback, true);
    }

    /**
     * Process an InOnly or RobustInOnly exchange inside a JMS session.
     * This method delegates the JMS message creation to the marshaler and uses
     * the JMS template to send it.
     *
     * @param exchange
     * @param in
     * @param session
     * @throws Exception
     */
    protected void processInOnlyInSession(final MessageExchange exchange,
                                          final NormalizedMessage in,
                                          final Session session) throws Exception {
        // Create destination
        final Destination dest = getDestination(exchange, in, session);
        // Create message and send it
        final Message message = marshaler.createMessage(exchange, in, session);
        template.send(dest, new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return message;
            }
        });
    }

    /**
     * Process an InOut or InOptionalOut exchange.
     * This method uses the JMS template to create a session and call the
     * {@link #processInOutInSession(javax.jbi.messaging.MessageExchange, javax.jbi.messaging.NormalizedMessage, javax.jms.Session)}
     * method.
     *
     * @param exchange
     * @param in
     * @throws Exception
     */
    protected void processInOut(final MessageExchange exchange,
                                final NormalizedMessage in) throws Exception {
        SessionCallback callback = new SessionCallback() {
            public Object doInJms(Session session) throws JMSException {
                try {
                    processInOutInSession(exchange, in, session);
                    return null;
                } catch (JMSException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new UncategorizedJmsException(e);
                }
            }
        };
        template.execute(callback, true);
    }
    
    /**
     * Process an InOnly or RobustInOnly exchange inside a JMS session.
     * This method delegates the JMS message creation to the marshaler and uses
     * the JMS template to send it.  If the JMS destination that was used to send
     * the message is not the default one, it synchronously wait for the message
     * to come back using a JMS selector.  Else, it just returns and the response
     * message will come back from the listener container.
     *
     * @param exchange
     * @param in
     * @param session
     * @throws Exception
     */
    protected void processInOutInSession(final MessageExchange exchange,
                                         final NormalizedMessage in,
                                         final Session session) throws Exception {
        // Create destinations
        final Destination dest = getDestination(exchange, in, session);
        final Destination replyDest = getReplyDestination(exchange, in, session);
        // Create message and send it
        final Message sendJmsMsg = marshaler.createMessage(exchange, in, session);
        sendJmsMsg.setJMSReplyTo(replyDest);
        // handle correlation ID
        String correlationId = sendJmsMsg.getJMSMessageID() != null ? sendJmsMsg.getJMSMessageID() : exchange.getExchangeId(); 
        sendJmsMsg.setJMSCorrelationID(correlationId);

        boolean asynchronous = replyDest.equals(replyDestination);

        if (asynchronous) {
            store.store(correlationId, exchange);
        }

        try {
            template.send(dest, new MessageCreator() {
                public Message createMessage(Session session)
                    throws JMSException {
                    return sendJmsMsg;
                }
            });
        } catch (Exception e) {
            if (asynchronous) {
                store.load(exchange.getExchangeId());
            }
            throw e;
        }

        if (!asynchronous) {
            // Create selector
            String jmsId = sendJmsMsg.getJMSMessageID();
            String selector = MSG_SELECTOR_START + jmsId + MSG_SELECTOR_END;
            // Receiving JMS Message, Creating and Returning NormalizedMessage out
            Message receiveJmsMsg = template.receiveSelected(replyDest, selector);
            if (receiveJmsMsg == null) {
                throw new IllegalStateException("Unable to receive response");
            }

            NormalizedMessage out = exchange.getMessage("out");
            if (out == null) {
                out = exchange.createMessage();
                exchange.setMessage(out, "out");
            }
            marshaler.populateMessage(receiveJmsMsg, exchange, out);
            boolean txSync = exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
            if (txSync) {
                sendSync(exchange);
            } else {
                send(exchange);
            }
        }
    }

    /**
     * Process a JMS response message.
     * This method delegates to the marshaler for the JBI out message creation
     * and sends it in to the NMR.
     * 
     * @param message
     */
    protected void onMessage(Message message) {
        MessageExchange exchange = null;
        try {
            exchange = (InOut) store.load(message.getJMSCorrelationID());
            if (exchange == null) {
                throw new IllegalStateException("Could not find exchange " + message.getJMSCorrelationID());
            }
        } catch (Exception e) {
            logger.error("Unable to load exchange related to incoming JMS message " + message, e);
        }
        try {
            NormalizedMessage out = exchange.getMessage("out");
            if (out == null) {
                out = exchange.createMessage();
                exchange.setMessage(out, "out");
            }
            marshaler.populateMessage(message, exchange, out);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error while populating JBI exchange " + exchange, e);
            }
            exchange.setError(e);
        }
        try {
            boolean txSync = exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
            if (txSync) {
                sendSync(exchange);
            } else {
                send(exchange);
            }
        } catch (Exception e) {
            logger.error("Unable to send JBI exchange " + exchange, e);
        }
    }

    /**
     * Retrieve the destination where the JMS message should be sent to.
     *
     * @param exchange
     * @param message
     * @param session
     * @return
     * @throws JMSException
     */
    protected Destination getDestination(MessageExchange exchange, Object message, Session session) throws JMSException {
        return chooseDestination(exchange, message, session, destinationChooser, destination);
    }

    /**
     * Choose the JMS destination for the reply message
     *
     * @param exchange
     * @param message
     * @param session
     * @return
     * @throws JMSException
     */
    protected Destination getReplyDestination(MessageExchange exchange, Object message, Session session) throws JMSException {
        return chooseDestination(exchange, message, session, replyDestinationChooser, replyDestination);
    }

    /**
     * Choose a JMS destination given the chooser, a default destination and name
     * @param exchange
     * @param message
     * @param session
     * @param chooser
     * @param defaultDestination
     * @return
     * @throws JMSException
     */
    protected Destination chooseDestination(MessageExchange exchange,
                                            Object message,
                                            Session session,
                                            DestinationChooser chooser,
                                            Destination defaultDestination) throws JMSException {
        Object dest = null;
        // Let the replyDestinationChooser a chance to choose the destination
        if (chooser != null) {
            dest = chooser.chooseDestination(exchange, message);
        }
        // Default to defaultDestination properties
        if (dest == null) {
            dest = defaultDestination;
        }
        // Resolve destination if needed
        if (dest instanceof Destination) {
            return (Destination) dest;
        } else if (dest instanceof String) {
            return destinationResolver.resolveDestinationName(session, 
                                                              (String) dest, 
                                                              isPubSubDomain());
        }
        throw new IllegalStateException("Unable to choose a destination for exchange " + exchange);
    }

    /**
     * Start this endpoint.
     *
     * @throws Exception
     */
    public synchronized void start() throws Exception {
        if (store == null) {
            if (storeFactory == null) {
                storeFactory = new MemoryStoreFactory();
            }
            store = storeFactory.open(getService().toString() + getEndpoint());
        }
        template = createTemplate();
        // Obtain the default destination
        if (destination == null && destinationName != null) {
            destination = (Destination) template.execute(new SessionCallback() {
                public Object doInJms(Session session) throws JMSException {
                    return destinationResolver.resolveDestinationName(session, destinationName, isPubSubDomain());
                }
            });
        }
        // Obtain the default reply destination
        if (replyDestination == null && replyDestinationName != null) {
            replyDestination = (Destination) template.execute(new SessionCallback() {
                public Object doInJms(Session session) throws JMSException {
                    return destinationResolver.resolveDestinationName(session, replyDestinationName, isPubSubDomain());
                }
            });
        }
        // create the listener container
        if (replyDestination != null) {
            listenerContainer = createListenerContainer();
            listenerContainer.start();
        }
        super.start();
    }

    /**
     * Stops this endpoint.
     * 
     * @throws Exception
     */
    public synchronized void stop() throws Exception {
        if (listenerContainer != null) {
            listenerContainer.stop();
        }
        if (store != null) {
            if (storeFactory != null) {
                storeFactory.close(store);
            }
            store = null;
        }
        super.stop();
    }

    /**
     * Validate this endpoint.
     * 
     * @throws DeploymentException
     */
    public void validate() throws DeploymentException {
        super.validate();
        if (getService() == null) {
            throw new DeploymentException("service must be set");
        }
        if (getEndpoint() == null) {
            throw new DeploymentException("endpoint must be set");
        }
        if (getConnectionFactory() == null) {
            throw new DeploymentException("connectionFactory is required");
        }
    }

    /**
     * Create the JMS template to be used to send the JMS messages.
     *
     * @return
     */
    protected JmsTemplate createTemplate() {
        JmsTemplate tplt;
        if (isJms102()) {
            tplt = new JmsTemplate102();
        } else {
            tplt = new JmsTemplate();
        }
        tplt.setConnectionFactory(getConnectionFactory());
        if (getDestination() != null) {
            tplt.setDefaultDestination(getDestination());
        } else if (getDestinationName() != null) {
            tplt.setDefaultDestinationName(getDestinationName());
        }
        tplt.setDeliveryMode(getDeliveryMode());
        if (getDestinationResolver() != null) {
            tplt.setDestinationResolver(getDestinationResolver());
        }
        tplt.setExplicitQosEnabled(isExplicitQosEnabled());
        tplt.setMessageIdEnabled(isMessageIdEnabled());
        tplt.setMessageTimestampEnabled(isMessageTimestampEnabled());
        tplt.setPriority(getPriority());
        tplt.setPubSubDomain(isPubSubDomain());
        tplt.setPubSubNoLocal(isPubSubNoLocal());
        tplt.setTimeToLive(getTimeToLive());
        tplt.setReceiveTimeout(getReceiveTimeout());
        tplt.afterPropertiesSet();
        return tplt;
    }

    /**
     * Create the message listener container to receive response messages.
     * 
     * @return
     */
    protected AbstractMessageListenerContainer createListenerContainer() {
        DefaultMessageListenerContainer cont;
        if (isJms102()) {
            cont = new DefaultMessageListenerContainer102();
        } else {
            cont = new DefaultMessageListenerContainer();
        }
        cont.setConnectionFactory(getConnectionFactory());
        cont.setDestination(getReplyDestination());
        cont.setPubSubDomain(isPubSubDomain());
        cont.setPubSubNoLocal(isPubSubNoLocal());
        cont.setMessageListener(new MessageListener() {
            public void onMessage(Message message) {
                JmsProviderEndpoint.this.onMessage(message);
            }
        });
        cont.afterPropertiesSet();
        return cont;
    }
}
