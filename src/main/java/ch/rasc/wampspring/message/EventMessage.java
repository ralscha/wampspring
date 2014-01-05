/**
 * Copyright 2014-2014 Ralph Schaer <ralphschaer@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.rasc.wampspring.message;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Subscribers receive PubSub events published by subscribers via the EVENT
 * message. The EVENT message contains the topicURI, the topic under which the
 * event was published, and the event, the PubSub event payload.
 * 
 * <p>
 * Server-to-Client message
 * 
 * @see <a href="http://wamp.ws/spec/#event_message">WAMP specification</a>
 */
public class EventMessage extends WampMessage {
	private final String topicURI;

	private final Object event;

	public EventMessage(String topicURI, Object event) {
		super(WampMessageType.EVENT);
		this.topicURI = topicURI;
		this.event = event;
	}

	public EventMessage(JsonParser jp) throws IOException {
		super(WampMessageType.EVENT);

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		this.topicURI = jp.getValueAsString();

		jp.nextToken();
		this.event = jp.readValueAs(Object.class);
	}

	public String getTopicURI() {
		return topicURI;
	}

	public Object getEvent() {
		return event;
	}

	@Override
	public String toJson(JsonFactory jsonFactory) throws IOException {
		try (StringWriter sw = new StringWriter(); JsonGenerator jg = jsonFactory.createGenerator(sw)) {
			jg.writeStartArray();
			jg.writeNumber(getTypeId());
			jg.writeString(topicURI);
			jg.writeObject(event);
			jg.writeEndArray();
			jg.close();

			return sw.toString();
		}
	}

	@Override
	public String toString() {
		return "EventMessage [topicURI=" + topicURI + ", event=" + event + "]";
	}

}
