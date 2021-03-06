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
package org.apache.servicemix.soap.util.stax;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

public class ExtendedXMLStreamReader extends StreamReaderDelegate {

	private SimpleNamespaceContext context = new SimpleNamespaceContext();
	
	public ExtendedXMLStreamReader(XMLStreamReader delegate) {
		super(delegate);
	}

	public NamespaceContext getNamespaceContext() {
		return context;
	}
	
    public int nextTag() throws XMLStreamException {
    	int eventType = next();
    	 while((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace()) // skip whitespace
	    	 || (eventType == XMLStreamConstants.CDATA && isWhiteSpace()) 
	    	 // skip whitespace
	    	 || eventType == XMLStreamConstants.SPACE
	    	 || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
	    	 || eventType == XMLStreamConstants.COMMENT
	    	 ) {
	    	 eventType = next();
    	 }
    	 if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
    	 	throw new XMLStreamException("expected start or end tag", getLocation());
    	 }
    	 return eventType;
    }

	public int next() throws XMLStreamException {
		int next = super.next();
		if (next == START_ELEMENT) {
			context = new SimpleNamespaceContext(context, getNamespaces());
		} else if (next == END_ELEMENT) {
			context = context.getParent();
		}
		return next;
	}

	private Map<String, String> getNamespaces() {
		Map<String,String> ns = new HashMap<String,String>();
		for (int i = 0; i < getNamespaceCount(); i++) {
			ns.put(getNamespacePrefix(i), getNamespaceURI(i));
		}
		return ns;
	}
	
	public static class SimpleNamespaceContext implements ExtendedNamespaceContext {

		private SimpleNamespaceContext parent;
		private Map<String,String> namespaces;
		
		public SimpleNamespaceContext() {
			namespaces = new HashMap<String,String>();
		}

		public SimpleNamespaceContext(SimpleNamespaceContext parent, Map<String,String> namespaces) {
			this.parent = parent;
			this.namespaces = namespaces;
		}

		public SimpleNamespaceContext getParent() {
			return parent;
		}
		
		public Collection<String> getPrefixes() {
			HashSet<String> prefixes = new HashSet<String>();
			for (SimpleNamespaceContext context = this; context != null; context = context.parent) {
				prefixes.addAll(context.namespaces.keySet());
			}
			return prefixes;
		}

		public String getNamespaceURI(String prefix) {
			String uri = namespaces.get(prefix);
			if (uri == null && parent != null) {
				uri = parent.getNamespaceURI(prefix);
			}
			return uri;
		}

		public String getPrefix(String namespaceURI) {
			for (SimpleNamespaceContext context = this; context != null; context = context.parent) {
                for (Map.Entry<String, String> entry : context.namespaces.entrySet()) {
					if (entry.getValue().equals(namespaceURI)) {
						return entry.getKey();
					}
				}
			}
			return null;
		}

		public Iterator<String> getPrefixes(String namespaceURI) {
			HashSet<String> prefixes = new HashSet<String>();
            for (SimpleNamespaceContext context = this; context != null; context = context.parent) {
                for (Map.Entry<String, String> entry : context.namespaces.entrySet()) {
                    if (entry.getValue().equals(namespaceURI)) {
						prefixes.add(entry.getKey());
					}
				}
			}
			return prefixes.iterator();
		}
		
	}


}
