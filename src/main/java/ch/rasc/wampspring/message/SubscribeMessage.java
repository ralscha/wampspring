/**
 * Copyright 2014-2015 Ralph Schaer <ralphschaer@gmail.com>
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

import ch.rasc.wampspring.handler.WampSession;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * A client requests access to a valid topicURI to receive events published to the given
 * topicURI.
 *
 * <p>
 * Client-to-Server message
 *
 * @see <a href="http://wamp.ws/spec/#subscribe_message">WAMP specification</a>
 */
public class SubscribeMessage extends PubSubMessage {

	public SubscribeMessage(String topicURI) {
		super(WampMessageType.SUBSCRIBE, topicURI);
	}

	public SubscribeMessage(JsonParser jp) throws IOException {
		this(jp, null);
	}

	public SubscribeMessage(JsonParser jp, WampSession wampSession) throws IOException {
		super(WampMessageType.SUBSCRIBE);

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		setTopicURI(replacePrefix(jp.getValueAsString(), wampSession));
	}

	@Override
	public String toJson(JsonFactory jsonFactory) throws IOException {
		try (StringWriter sw = new StringWriter();
				JsonGenerator jg = jsonFactory.createGenerator(sw)) {
			jg.writeStartArray();
			jg.writeNumber(getTypeId());
			jg.writeString(getTopicURI());
			jg.writeEndArray();
			jg.close();
			return sw.toString();
		}
	}

	@Override
	public String toString() {
		return "SubscribeMessage [topicURI=" + getTopicURI() + "]";
	}

}
