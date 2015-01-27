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

/**
 * Base class of the WampMessages
 */
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.handler.WampSession;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public abstract class WampMessage implements Message<Object> {

	private final static Object EMPTY_OBJECT = new Object();

	private final MutableMessageHeaders messageHeaders = new MutableMessageHeaders();

	WampMessage(WampMessageType type) {
		setHeader(WampMessageHeader.WAMP_MESSAGE_TYPE, type);
	}

	int getTypeId() {
		return getType().getTypeId();
	}

	public WampMessageType getType() {
		return getHeader(WampMessageHeader.WAMP_MESSAGE_TYPE);
	}

	public void setHeader(WampMessageHeader header, Object value) {
		this.messageHeaders.getRawHeaders().put(header.name(), value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getHeader(WampMessageHeader header) {
		return (T) this.messageHeaders.get(header.name());
	}

	public void setDestinationTemplateVariables(Map<String, String> vars) {
		this.messageHeaders
				.getRawHeaders()
				.put(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER,
						vars);
	}

	/**
	 * Convenient method to retrieve the WebSocket session id.
	 */
	public String getSessionId() {
		return getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID);
	}

	public void setSessionId(String webSocketSessionId) {
		setHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, webSocketSessionId);
	}

	public String getDestination() {
		return null;
	}

	public Principal getPrincipal() {
		return getHeader(WampMessageHeader.PRINCIPAL);
	}

	void setPrincipal(Principal principal) {
		setHeader(WampMessageHeader.PRINCIPAL, principal);
	}

	public WampSession getWampSession() {
		return (WampSession) getHeader(WampMessageHeader.WAMP_SESSION);
	}

	void setWampSession(WampSession wampSession) {
		setHeader(WampMessageHeader.WAMP_SESSION, wampSession);
	}

	@Override
	public Object getPayload() {
		return EMPTY_OBJECT;
	}

	@Override
	public MessageHeaders getHeaders() {
		return this.messageHeaders;
	}

	public static <T extends WampMessage> T fromJson(WebSocketSession session,
			JsonFactory jsonFactory, String json) throws IOException {

		WampSession wampSession = new WampSession(session);

		T newWampMessage = fromJson(jsonFactory, json, wampSession);

		newWampMessage.setSessionId(session.getId());
		newWampMessage.setPrincipal(session.getPrincipal());
		newWampMessage.setWampSession(wampSession);

		return newWampMessage;
	}

	public abstract String toJson(JsonFactory jsonFactory) throws IOException;

	public static <T extends WampMessage> T fromJson(JsonFactory jsonFactory, String json)
			throws IOException {
		return fromJson(jsonFactory, json, null);
	}

	@SuppressWarnings("unchecked")
	public static <T extends WampMessage> T fromJson(JsonFactory jsonFactory,
			String json, WampSession wampSession) throws IOException {

		try (JsonParser jp = jsonFactory.createParser(json)) {
			if (jp.nextToken() != JsonToken.START_ARRAY) {
				throw new IOException("Not a JSON array");
			}
			if (jp.nextToken() != JsonToken.VALUE_NUMBER_INT) {
				throw new IOException("Wrong message format");
			}

			WampMessageType messageType = WampMessageType.fromTypeId(jp.getValueAsInt());

			switch (messageType) {
			case WELCOME:
				return (T) new WelcomeMessage(jp);
			case PREFIX:
				return (T) new PrefixMessage(jp);
			case CALL:
				return (T) new CallMessage(jp, wampSession);
			case CALLRESULT:
				return (T) new CallResultMessage(jp);
			case CALLERROR:
				return (T) new CallErrorMessage(jp);
			case SUBSCRIBE:
				return (T) new SubscribeMessage(jp, wampSession);
			case UNSUBSCRIBE:
				return (T) new UnsubscribeMessage(jp, wampSession);
			case PUBLISH:
				return (T) new PublishMessage(jp, wampSession);
			case EVENT:
				return (T) new EventMessage(jp, wampSession);
			default:
				return null;
			}

		}
	}

	@SuppressWarnings("serial")
	private static class MutableMessageHeaders extends MessageHeaders {

		public MutableMessageHeaders() {
			super(null);
		}

		@Override
		public Map<String, Object> getRawHeaders() {
			return super.getRawHeaders();
		}
	}

	protected String replacePrefix(String uri, WampSession wampSession) {
		if (uri != null && wampSession != null && wampSession.hasPrefixes()) {
			String[] curie = uri.split(":");
			if (curie.length == 2) {
				String prefix = wampSession.getPrefix(curie[0]);
				if (prefix != null) {
					return prefix + curie[1];
				}
			}
		}
		return uri;
	}

}
