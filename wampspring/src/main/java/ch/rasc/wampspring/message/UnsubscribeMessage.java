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

import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.handler.WampSession;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Calling unsubscribe on a topicURI informs the server to stop delivering messages to the
 * client previously subscribed to that topicURI.
 *
 * <p>
 * Client-to-Server message
 *
 * @see <a href="http://wamp.ws/spec/#unsubscribe_message">WAMP specification</a>
 */
public class UnsubscribeMessage extends PubSubMessage {

	private boolean cleanup = false;

	public UnsubscribeMessage(String topicURI) {
		super(WampMessageType.UNSUBSCRIBE, topicURI);
	}

	public UnsubscribeMessage(JsonParser jp) throws IOException {
		this(jp, null);
	}

	public UnsubscribeMessage(JsonParser jp, WampSession wampSession) throws IOException {
		super(WampMessageType.UNSUBSCRIBE);
		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		setTopicURI(replacePrefix(jp.getValueAsString(), wampSession));
	}

	/**
	 * Creates an internal unsubscribe message. The system creates this message when the
	 * WebSocket session ends and sends it to the subscribed message handlers for cleaning
	 * up
	 *
	 * @param sessionId the WebSocket session id
	 **/
	public static UnsubscribeMessage createCleanupMessage(WebSocketSession session) {
		UnsubscribeMessage msg = new UnsubscribeMessage("**");

		msg.setSessionId(session.getId());
		msg.setPrincipal(session.getPrincipal());
		msg.setWampSession(new WampSession(session));

		msg.cleanup = true;

		return msg;
	}

	public boolean isCleanup() {
		return this.cleanup;
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
		return "UnsubscribeMessage [topicURI=" + getTopicURI() + "]";
	}

}
