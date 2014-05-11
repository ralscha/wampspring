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
 * When the remote procedure call could not be executed, an error or exception occurred during the execution or the
 * execution of the remote procedure finishes unsuccessfully for any other reason, the server responds by sending this
 * message
 * <p>
 * Server-to-Client message
 * 
 * @see <a href="http://wamp.ws/spec/#callerror_message">WAMP specification</a>
 */
public class CallErrorMessage extends WampMessage {
	private final String callID;

	private final String errorURI;

	private final String errorDesc;

	private final Object errorDetails;

	public CallErrorMessage(String callID, String errorURI, String errorDesc) {
		this(callID, errorURI, errorDesc, null);
	}

	public CallErrorMessage(String callID, String errorURI, String errorDesc, Object errorDetails) {
		super(WampMessageType.CALLERROR);
		this.callID = callID;
		this.errorURI = errorURI;
		this.errorDesc = errorDesc;
		this.errorDetails = errorDetails;
	}

	public CallErrorMessage(JsonParser jp) throws IOException {
		super(WampMessageType.CALLERROR);

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		this.callID = jp.getValueAsString();

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		this.errorURI = jp.getValueAsString();

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		this.errorDesc = jp.getValueAsString();

		if (jp.nextToken() != JsonToken.END_ARRAY) {
			this.errorDetails = jp.readValueAs(Object.class);
		} else {
			this.errorDetails = null;
		}
	}

	public String getCallID() {
		return callID;
	}

	public String getErrorURI() {
		return errorURI;
	}

	public String getErrorDesc() {
		return errorDesc;
	}

	public Object getErrorDetails() {
		return errorDetails;
	}

	@Override
	public String toJson(JsonFactory jsonFactory) throws IOException {
		try (StringWriter sw = new StringWriter(); JsonGenerator jg = jsonFactory.createGenerator(sw)) {
			jg.writeStartArray();
			jg.writeNumber(getTypeId());
			jg.writeString(callID);
			jg.writeString(errorURI);
			jg.writeString(errorDesc);
			if (errorDetails != null) {
				jg.writeObject(errorDetails);
			}
			jg.writeEndArray();
			jg.close();

			return sw.toString();
		}
	}

	@Override
	public String toString() {
		return "CallErrorMessage [callID=" + callID + ", errorURI=" + errorURI + ", errorDesc=" + errorDesc
				+ ", errorDetails=" + errorDetails + "]";
	}

}
