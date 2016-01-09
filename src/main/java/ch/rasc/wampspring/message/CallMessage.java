/**
 * Copyright 2014-2016 Ralph Schaer <ralphschaer@gmail.com>
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import ch.rasc.wampspring.config.WampSession;

/**
 * A client initiates a RPC by sending this message
 * <p>
 * Client-to-Server message
 *
 * @see <a href="http://wamp.ws/spec/wamp1/#call_message">WAMP specification</a>
 */
public class CallMessage extends WampMessage {
	private final String callID;

	private final String procURI;

	private final List<Object> arguments;

	public CallMessage(String callID, String procURI, Object... arguments) {
		super(WampMessageType.CALL);
		this.callID = callID;
		this.procURI = procURI;
		if (arguments != null) {
			this.arguments = Arrays.asList(arguments);
		}
		else {
			this.arguments = null;
		}

	}

	public CallMessage(JsonParser jp) throws IOException {
		this(jp, null);
	}

	public CallMessage(JsonParser jp, WampSession wampSession) throws IOException {
		super(WampMessageType.CALL);

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		this.callID = jp.getValueAsString();

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		this.procURI = replacePrefix(jp.getValueAsString(), wampSession);

		List<Object> args = new ArrayList<>();
		while (jp.nextToken() != JsonToken.END_ARRAY) {
			args.add(jp.readValueAs(Object.class));
		}

		if (!args.isEmpty()) {
			this.arguments = Collections.unmodifiableList(args);
		}
		else {
			this.arguments = null;
		}
	}

	public String getCallID() {
		return this.callID;
	}

	public String getProcURI() {
		return this.procURI;
	}

	public List<Object> getArguments() {
		return this.arguments;
	}

	@Override
	public String getDestination() {
		return this.procURI;
	}

	@Override
	public String toJson(JsonFactory jsonFactory) throws IOException {
		try (StringWriter sw = new StringWriter();
				JsonGenerator jg = jsonFactory.createGenerator(sw)) {
			jg.writeStartArray();
			jg.writeNumber(getTypeId());
			jg.writeString(this.callID);
			jg.writeString(this.procURI);
			if (this.arguments != null) {
				for (Object argument : this.arguments) {
					jg.writeObject(argument);
				}
			}

			jg.writeEndArray();
			jg.close();
			return sw.toString();
		}
	}

	@Override
	public String toString() {
		return "CallMessage [callID=" + this.callID + ", procURI=" + this.procURI
				+ ", arguments=" + this.arguments + "]";
	}

}
