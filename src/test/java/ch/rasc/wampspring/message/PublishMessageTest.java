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

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.fest.assertions.data.MapEntry;
import org.junit.Test;

public class PublishMessageTest extends BaseMessageTest {

	@Test
	public void serializationTest() throws IOException {
		PublishMessage publishMessage = new PublishMessage("http://example.com/simple", "Hello, world!");
		String json = publishMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.PUBLISH.getTypeId(), "http://example.com/simple", "Hello, world!"));

		publishMessage = new PublishMessage("http://example.com/simple", null);
		json = publishMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.PUBLISH.getTypeId(), "http://example.com/simple", null));

		Map<String, Object> map = new HashMap<>();
		map.put("rand", 0.09187032734575862);
		map.put("flag", false);
		map.put("num", 23);
		map.put("name", "Kross");
		map.put("created", "2012-03-29T10:41:09.864Z");
		publishMessage = new PublishMessage("http://example.com/event#myevent2", map);
		json = publishMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.PUBLISH.getTypeId(), "http://example.com/event#myevent2", map));

		publishMessage = new PublishMessage("event:myevent1", "hello", new TreeSet<>(Arrays.asList("NwtXQ8rdfPsy-ewS",
				"dYqgDl0FthI6_hjb")));
		json = publishMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.PUBLISH.getTypeId(), "event:myevent1", "hello",
						Arrays.asList("NwtXQ8rdfPsy-ewS", "dYqgDl0FthI6_hjb")));

		publishMessage = new PublishMessage("event:myevent1", "hello", Collections.<String> emptySet(), new TreeSet<>(
				Arrays.asList("NwtXQ8rdfPsy-ewS")));
		json = publishMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.PUBLISH.getTypeId(), "event:myevent1", "hello", Collections.emptyList(),
						Arrays.asList("NwtXQ8rdfPsy-ewS")));

		publishMessage = new PublishMessage("event:myevent1", "true", true);
		json = publishMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.PUBLISH.getTypeId(), "event:myevent1", "true", true));

		publishMessage = new PublishMessage("event:myevent1", "false", false);
		json = publishMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.PUBLISH.getTypeId(), "event:myevent1", "false"));
	}

	@Test
	public void deserializationTest() throws IOException {
		String json = toJsonArray(7, "http://example.com/simple", "Hello, world!");
		PublishMessage publishMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(publishMessage.getType()).isEqualTo(WampMessageType.PUBLISH);
		assertThat(publishMessage.getTopicURI()).isEqualTo("http://example.com/simple");
		assertThat(publishMessage.getEvent()).isEqualTo("Hello, world!");
		assertThat(publishMessage.getExcludeMe()).isNull();
		assertThat(publishMessage.getExclude()).isNull();
		assertThat(publishMessage.getEligible()).isNull();

		json = toJsonArray(7, "http://example.com/simple", null);
		publishMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(publishMessage.getType()).isEqualTo(WampMessageType.PUBLISH);
		assertThat(publishMessage.getTopicURI()).isEqualTo("http://example.com/simple");
		assertThat(publishMessage.getEvent()).isNull();
		assertThat(publishMessage.getExcludeMe()).isNull();
		assertThat(publishMessage.getExclude()).isNull();
		assertThat(publishMessage.getEligible()).isNull();

		json = toJsonArray(7, "http://example.com/simple", "hi", true);
		publishMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(publishMessage.getType()).isEqualTo(WampMessageType.PUBLISH);
		assertThat(publishMessage.getTopicURI()).isEqualTo("http://example.com/simple");
		assertThat(publishMessage.getEvent()).isEqualTo("hi");
		assertThat(publishMessage.getExcludeMe()).isTrue();
		assertThat(publishMessage.getExclude()).isNull();
		assertThat(publishMessage.getEligible()).isNull();

		json = toJsonArray(7, "http://example.com/simple", "hi", false);
		publishMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(publishMessage.getType()).isEqualTo(WampMessageType.PUBLISH);
		assertThat(publishMessage.getTopicURI()).isEqualTo("http://example.com/simple");
		assertThat(publishMessage.getEvent()).isEqualTo("hi");
		assertThat(publishMessage.getExcludeMe()).isFalse();
		assertThat(publishMessage.getExclude()).isNull();
		assertThat(publishMessage.getEligible()).isNull();

		Map<String, Object> map = new HashMap<>();
		map.put("rand", 0.09187032734575862);
		map.put("flag", false);
		map.put("num", 23);
		map.put("name", "Kross");
		map.put("created", "2012-03-29T10:41:09.864Z");
		json = toJsonArray(7, "http://example.com/simple", map);
		publishMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(publishMessage.getType()).isEqualTo(WampMessageType.PUBLISH);
		assertThat(publishMessage.getTopicURI()).isEqualTo("http://example.com/simple");
		assertThat((Map<String, Object>) publishMessage.getEvent()).hasSize(5).contains(
				MapEntry.entry("rand", 0.09187032734575862), MapEntry.entry("flag", false), MapEntry.entry("num", 23),
				MapEntry.entry("name", "Kross"), MapEntry.entry("created", "2012-03-29T10:41:09.864Z"));
		assertThat(publishMessage.getExcludeMe()).isNull();
		assertThat(publishMessage.getExclude()).isNull();
		assertThat(publishMessage.getEligible()).isNull();

		json = toJsonArray(7, "event:myevent1", "hello", Arrays.asList("NwtXQ8rdfPsy-ewS", "dYqgDl0FthI6_hjb"));
		publishMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(publishMessage.getType()).isEqualTo(WampMessageType.PUBLISH);
		assertThat(publishMessage.getTopicURI()).isEqualTo("event:myevent1");
		assertThat(publishMessage.getEvent()).isEqualTo("hello");
		assertThat(publishMessage.getExcludeMe()).isNull();
		assertThat(publishMessage.getExclude()).containsExactly("NwtXQ8rdfPsy-ewS", "dYqgDl0FthI6_hjb");
		assertThat(publishMessage.getEligible()).isNull();

		json = toJsonArray(7, "event:myevent1", "hello", Collections.emptyList(), Arrays.asList("NwtXQ8rdfPsy-ewS"));
		publishMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(publishMessage.getType()).isEqualTo(WampMessageType.PUBLISH);
		assertThat(publishMessage.getTopicURI()).isEqualTo("event:myevent1");
		assertThat(publishMessage.getEvent()).isEqualTo("hello");
		assertThat(publishMessage.getExcludeMe()).isNull();
		assertThat(publishMessage.getExclude()).isEmpty();
		assertThat(publishMessage.getEligible()).containsExactly("NwtXQ8rdfPsy-ewS");

	}
}
