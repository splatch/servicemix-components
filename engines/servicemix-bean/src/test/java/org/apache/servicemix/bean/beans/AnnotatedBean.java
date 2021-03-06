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
package org.apache.servicemix.bean.beans;

import javax.jbi.messaging.MessageExchange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.bean.Operation;

/**
 * A simple POJO which uses an annotation to indicate which method should be driven by a message exchange
 *
 * @version $Revision: $
 */
public class AnnotatedBean {

    private static final Log LOG = LogFactory.getLog(AnnotatedBean.class);

    private MessageExchange myExchangeMethod;

    @Operation(name = "foo")
    public void myExchangeMethod(MessageExchange messageExchange) {
        this.myExchangeMethod = messageExchange;

        LOG.info("myExchangeMethod() received exchange: " + messageExchange);
    }
    
    public MessageExchange getMyExchangeMethod() {
        return myExchangeMethod;
    }
}
