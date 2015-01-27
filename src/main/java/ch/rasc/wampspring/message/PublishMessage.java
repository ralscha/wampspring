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
import java.util.Set;

import ch.rasc.wampspring.handler.WampSession;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * The client will send an event to all clients connected to the server who have
 * subscribed to the topicURI.
 *
 * <p>
 * Client-to-Server message
 *
 * @see <a href="http://wamp.ws/spec/wamp1/#publish_message">WAMP specification</a>
 */
public class PublishMessage extends PubSubMessage {
	private final Object event;

	private final Boolean excludeMe;

	private final Set<String> exclude;

	private final Set<String> eligible;

	public PublishMessage(String topicURI, Object event) {
		this(topicURI, event, null, null, null);
	}

	public PublishMessage(String topicURI, Object event, Boolean excludeMe) {
		this(topicURI, event, excludeMe, null, null);
	}

	public PublishMessage(String topicURI, Object event, Set<String> exclude) {
		this(topicURI, event, null, exclude, null);
	}

	public PublishMessage(String topicURI, Object event, Set<String> exclude,
			Set<String> eligible) {
		this(topicURI, event, null, exclude, eligible);
	}

	private PublishMessage(String topicURI, Object event, Boolean excludeMe,
			Set<String> exclude, Set<String> eligible) {
		super(WampMessageType.PUBLISH, topicURI);
		this.event = event;
		this.excludeMe = excludeMe;
		this.exclude = exclude;
		this.eligible = eligible;
	}

	public PublishMessage(JsonParser jp) throws IOException {
		this(jp, null);
	}

	public PublishMessage(JsonParser jp, WampSession wampSession) throws IOException {
		super(WampMessageType.PUBLISH);

		if (jp.nextToken() != JsonToken.VALUE_STRING) {
			throw new IOException();
		}
		setTopicURI(replacePrefix(jp.getValueAsString(), wampSession));

		jp.nextToken();
		this.event = jp.readValueAs(Object.class);

		if (jp.nextToken() != JsonToken.END_ARRAY) {
			if (jp.getCurrentToken() == JsonToken.VALUE_TRUE
					|| jp.getCurrentToken() == JsonToken.VALUE_FALSE) {
				this.excludeMe = jp.getValueAsBoolean();

				this.exclude = null;

				this.eligible = null;

				if (jp.nextToken() != JsonToken.END_ARRAY) {
					// Wrong message format, excludeMe should not be followed by
					// any value
					throw new IOException();
				}
			}
			else {
				this.excludeMe = null;

				TypeReference<Set<String>> typRef = new TypeReference<Set<String>>() {
					// nothing here
				};

				if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
					throw new IOException();
				}
				this.exclude = jp.readValueAs(typRef);

				if (jp.nextToken() == JsonToken.START_ARRAY) {
					this.eligible = jp.readValueAs(typRef);
				}
				else {
					this.eligible = null;
				}
			}
		}
		else {
			this.excludeMe = null;
			this.exclude = null;
			this.eligible = null;
		}

	}

	public Object getEvent() {
		return this.event;
	}

	@Override
	public Object getPayload() {
		return this.event;
	}

	public Boolean getExcludeMe() {
		return this.excludeMe;
	}

	public Set<String> getExclude() {
		return this.exclude;
	}

	public Set<String> getEligible() {
		return this.eligible;
	}

	@Override
	public String toJson(JsonFactory jsonFactory) throws IOException {
		try (StringWriter sw = new StringWriter();
				JsonGenerator jg = jsonFactory.createGenerator(sw)) {
			jg.writeStartArray();
			jg.writeNumber(getTypeId());
			jg.writeString(getTopicURI());

			jg.writeObject(this.event);
			if (this.excludeMe != null && this.excludeMe) {
				jg.writeBoolean(true);
			}
			else if (this.exclude != null) {
				jg.writeObject(this.exclude);
				if (this.eligible != null) {
					jg.writeObject(this.eligible);
				}
			}

			jg.writeEndArray();
			jg.close();
			return sw.toString();
		}
	}

	@Override
	public String toString() {
		return "PublishMessage [topicURI=" + getTopicURI() + ", event=" + this.event
				+ ", excludeMe=" + this.excludeMe + ", exclude=" + this.exclude
				+ ", eligible=" + this.eligible + "]";
	}

}
