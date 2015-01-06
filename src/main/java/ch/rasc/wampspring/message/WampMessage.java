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
import java.util.EnumMap;

import ch.rasc.wampspring.handler.WampSession;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public abstract class WampMessage {
	private final WampMessageType type;

	private EnumMap<WampMessageHeader, Object> headers;

	WampMessage(WampMessageType type) {
		this.type = type;
	}

	int getTypeId() {
		return type.getTypeId();
	}

	public WampMessageType getType() {
		return type;
	}

	public void addHeader(WampMessageHeader header, Object value) {
		if (headers == null) {
			headers = new EnumMap<>(WampMessageHeader.class);
		}
		headers.put(header, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T getHeader(WampMessageHeader header) {
		if (headers == null) {
			return null;
		}
		return (T) headers.get(header);
	}

	/**
	 * Convenient method to retrieve the WebSocket session id.
	 */
	public String getWebSocketSessionId() {
		return getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID);
	}

	public WampSession getWampSession() {
		return getHeader(WampMessageHeader.WAMP_SESSION);
	}

	public abstract String toJson(JsonFactory jsonFactory) throws IOException;

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(JsonFactory jsonFactory, String json) throws IOException {

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
				return (T) new CallMessage(jp);
			case CALLRESULT:
				return (T) new CallResultMessage(jp);
			case CALLERROR:
				return (T) new CallErrorMessage(jp);
			case SUBSCRIBE:
				return (T) new SubscribeMessage(jp);
			case UNSUBSCRIBE:
				return (T) new UnsubscribeMessage(jp);
			case PUBLISH:
				return (T) new PublishMessage(jp);
			case EVENT:
				return (T) new EventMessage(jp);
			default:
				return null;
			}

		}
	}
}
