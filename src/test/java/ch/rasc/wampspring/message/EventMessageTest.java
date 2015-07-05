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

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.fest.assertions.data.MapEntry;
import org.junit.Test;

public class EventMessageTest extends BaseMessageTest {

	@Test
	public void serializationTest() throws IOException {
		EventMessage eventMessage = new EventMessage("http://example.com/simple",
				"Hello, I am a simple event.");
		assertWampMessageTypeHeader(eventMessage, WampMessageType.EVENT);
		String json = eventMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.EVENT.getTypeId(),
				"http://example.com/simple", "Hello, I am a simple event."));

		eventMessage = new EventMessage("http://example.com/simple", null);
		assertWampMessageTypeHeader(eventMessage, WampMessageType.EVENT);
		json = eventMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.EVENT.getTypeId(),
				"http://example.com/simple", null));

		Map<String, Object> eventObject = new HashMap<>();
		eventObject.put("rand", 0.09187032734575862);
		eventObject.put("flag", Boolean.FALSE);
		eventObject.put("num", 23);
		eventObject.put("name", "Kross");
		eventObject.put("created", "2012-03-29T10:41:09.864Z");

		EventMessage mapEventMessage = new EventMessage(
				"http://example.com/event#myevent2", eventObject);
		assertWampMessageTypeHeader(mapEventMessage, WampMessageType.EVENT);
		json = mapEventMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.EVENT.getTypeId(),
				"http://example.com/event#myevent2", eventObject));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void deserializationTest() throws IOException {
		String json = toJsonArray(8, "http://example.com/simple",
				"Hello, I am a simple event.");
		EventMessage eventMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(eventMessage, WampMessageType.EVENT);
		assertThat(eventMessage.getType()).isEqualTo(WampMessageType.EVENT);
		assertThat(eventMessage.getTopicURI()).isEqualTo("http://example.com/simple");
		assertThat(eventMessage.getDestination()).isEqualTo("http://example.com/simple");
		assertThat(eventMessage.getEvent()).isEqualTo("Hello, I am a simple event.");

		json = toJsonArray(8, "http://example.com/simple", null);
		eventMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(eventMessage, WampMessageType.EVENT);
		assertThat(eventMessage.getType()).isEqualTo(WampMessageType.EVENT);
		assertThat(eventMessage.getTopicURI()).isEqualTo("http://example.com/simple");
		assertThat(eventMessage.getDestination()).isEqualTo("http://example.com/simple");
		assertThat(eventMessage.getEvent()).isNull();

		Map<String, Object> eventObject = new HashMap<>();
		eventObject.put("rand", 0.09187032734575862);
		eventObject.put("flag", Boolean.FALSE);
		eventObject.put("num", 23);
		eventObject.put("name", "Kross");
		eventObject.put("created", "2012-03-29T10:41:09.864Z");

		json = toJsonArray(8, "http://example.com/event#myevent2", eventObject);

		EventMessage mapEventMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(mapEventMessage, WampMessageType.EVENT);
		assertThat(mapEventMessage.getType()).isEqualTo(WampMessageType.EVENT);
		assertThat(mapEventMessage.getTopicURI())
				.isEqualTo("http://example.com/event#myevent2");
		assertThat(mapEventMessage.getDestination())
				.isEqualTo("http://example.com/event#myevent2");
		assertThat((Map) mapEventMessage.getEvent()).hasSize(5).contains(
				MapEntry.entry("rand", 0.09187032734575862),
				MapEntry.entry("flag", Boolean.FALSE), MapEntry.entry("num", 23),
				MapEntry.entry("name", "Kross"),
				MapEntry.entry("created", "2012-03-29T10:41:09.864Z"));
	}

	@Test
	public void copyConstructorTest() {
		EventMessage eventMessage = new EventMessage("topicURI", 1L);
		EventMessage copyOfMsg = new EventMessage(eventMessage, "wsId");
		assertWampMessageTypeHeader(copyOfMsg, WampMessageType.EVENT);

		assertThat(copyOfMsg.getDestination()).isEqualTo("topicURI");
		assertThat(copyOfMsg.getEvent()).isEqualTo(1L);
		assertThat(copyOfMsg.getTopicURI()).isEqualTo("topicURI");
		assertThat(copyOfMsg.getDestination()).isEqualTo("topicURI");

		PublishMessage publishMessage = new PublishMessage("topicURI", 1L);
		copyOfMsg = new EventMessage(publishMessage, "wsId");
		assertWampMessageTypeHeader(copyOfMsg, WampMessageType.EVENT);

		assertThat(copyOfMsg.getDestination()).isEqualTo("topicURI");
		assertThat(copyOfMsg.getEvent()).isEqualTo(1L);
		assertThat(copyOfMsg.getTopicURI()).isEqualTo("topicURI");
		assertThat(copyOfMsg.getDestination()).isEqualTo("topicURI");
	}
}
