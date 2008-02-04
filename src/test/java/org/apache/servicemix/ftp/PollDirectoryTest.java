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
package org.apache.servicemix.ftp;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class PollDirectoryTest extends SpringTestSupport {
    private static final int NUMBER = 100;
    private static final int SIZE = 100;

    protected String directoryName = "target/pollDirectory";
    protected String dynamicURI = "file:" + directoryName;

    public void testSendToWriterSoItCanBePolled() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        StringBuffer sb = new StringBuffer("<root>");
        for (int i = 0; i < SIZE; i++) {
            sb.append("<hello>world</hello>");
        }
        sb.append("</root>");
        
        // lets send a request to be written to a file
        // which should then be polled
        for (int i = 0; i < NUMBER; i++) {
            InOnly me = client.createInOnlyExchange();
            me.setService(new QName("urn:test", "service"));
            NormalizedMessage message = me.getInMessage();
            message.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, "test" + i + ".xml");
            message.setContent(new StringSource(sb.toString()));
            client.sendSync(me);
        }

        Receiver receiver = (Receiver) getBean("receiver");
        receiver.getMessageList().assertMessagesReceived(NUMBER);
    }

    protected void assertExchangeWorked(MessageExchange me) throws Exception {
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("spring-polling.xml");
    }

}