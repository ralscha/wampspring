/**
 * Copyright 2014-2017 the original author or authors.
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
 * When a WAMP client connects to a WAMP server, the very first message sent by the server
 * back to the client is this WELCOME message.
 *
 * <p>
 * Server-to-Client message
 *
 * @see <a href="http://wamp.ws/spec/wamp1/#welcome_message">WAMP specification</a>
 */
public class WelcomeMessage extends WampMessage {
	public static final int PROTOCOL_VERSION = 1;

	private final String sessionId;

	private final int protocolVersion;

	private final String serverIdent;

	public WelcomeMessage(String sessionId, String serverIdent) {
		super(WampMessageType.WELCOME);
		this.sessionId = sessionId;
		this.serverIdent = serverIdent;
		this.protocolVersion = PROTOCOL_VERSION;
	}

	public WelcomeMessage(JsonParser jp) throws IOException {
		super(WampMessageType.WELCOME);

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		this.sessionId = jp.getValueAsString();

		if (jp.nextToken() != JsonToken.VALUE_NUMBER_INT) {
			throw new IOException();
		}
		this.protocolVersion = jp.getValueAsInt();

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		this.serverIdent = jp.getValueAsString();

	}

	@Override
	public String getWebSocketSessionId() {
		return this.sessionId;
	}

	public int getProtocolVersion() {
		return this.protocolVersion;
	}

	public String getServerIdent() {
		return this.serverIdent;
	}

	@Override
	public String toJson(JsonFactory jsonFactory) throws IOException {
		try (StringWriter sw = new StringWriter();
				JsonGenerator jg = jsonFactory.createGenerator(sw)) {
			jg.writeStartArray();
			jg.writeNumber(getTypeId());
			jg.writeString(this.sessionId);
			jg.writeNumber(this.protocolVersion);
			jg.writeString(this.serverIdent);
			jg.writeEndArray();
			jg.close();
			return sw.toString();
		}
	}

	@Override
	public String toString() {
		return "WelcomeMessage [sessionId=" + this.sessionId + ", protocolVersion="
				+ this.protocolVersion + ", serverIdent=" + this.serverIdent + "]";
	}

}
