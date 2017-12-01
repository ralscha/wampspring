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
 * When the execution of the remote procedure finishes successfully, the server responds
 * by sending this message
 * <p>
 * Server-to-Client message
 *
 * @see <a href="http://wamp.ws/spec/wamp1/#callresult_message">WAMP specification</a>
 */
public class CallResultMessage extends WampMessage {
	private final String callID;

	private final Object result;

	public CallResultMessage(CallMessage callMessage, Object result) {
		super(WampMessageType.CALLRESULT);
		this.callID = callMessage.getCallID();
		this.result = result;

		setWebSocketSessionId(callMessage.getWebSocketSessionId());
		setPrincipal(callMessage.getPrincipal());
	}

	public CallResultMessage(JsonParser jp) throws IOException {
		super(WampMessageType.CALLRESULT);

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		this.callID = jp.getValueAsString();

		jp.nextToken();
		this.result = jp.readValueAs(Object.class);
	}

	public String getCallID() {
		return this.callID;
	}

	public Object getResult() {
		return this.result;
	}

	@Override
	public String toJson(JsonFactory jsonFactory) throws IOException {
		try (StringWriter sw = new StringWriter();
				JsonGenerator jg = jsonFactory.createGenerator(sw)) {
			jg.writeStartArray();
			jg.writeNumber(getTypeId());
			jg.writeString(this.callID);
			jg.writeObject(this.result);
			jg.writeEndArray();
			jg.close();
			return sw.toString();
		}
	}

	@Override
	public String toString() {
		return "CallResultMessage [callID=" + this.callID + ", result=" + this.result
				+ "]";
	}

}
