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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.data.MapEntry;
import org.junit.Test;

public class CallMessageTest extends BaseMessageTest {

	@Test
	public void serializationTest() throws IOException {
		CallMessage callMessage = new CallMessage("7DK6TdN4wLiUJgNM",
				"http://example.com/api#howdy");
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		String json = callMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.CALL.getTypeId(),
				"7DK6TdN4wLiUJgNM", "http://example.com/api#howdy"));

		callMessage = new CallMessage("Yp9EFZt9DFkuKndg", "api:add2", 23, 99);
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		json = callMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.CALL.getTypeId(),
				"Yp9EFZt9DFkuKndg", "api:add2", 23, 99));

		Map<String, Object> callObject = new HashMap<>();
		callObject.put("category", "dinner");
		callObject.put("calories", 2309);
		callMessage = new CallMessage("J5DkZJgByutvaDWc",
				"http://example.com/api#storeMeal", callObject);
		json = callMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.CALL.getTypeId(),
				"J5DkZJgByutvaDWc", "http://example.com/api#storeMeal", callObject));

		callMessage = new CallMessage("Dns3wuQo0ipOX1Xc", "http://example.com/api#woooat",
				new Object[] { null });
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		json = callMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.CALL.getTypeId(),
				"Dns3wuQo0ipOX1Xc", "http://example.com/api#woooat", null));

		callMessage = new CallMessage("M0nncaH0ywCSYzRv", "api:sum",
				Arrays.asList(9, 1, 3, 4));
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		json = callMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.CALL.getTypeId(),
				"M0nncaH0ywCSYzRv", "api:sum", Arrays.asList(9, 1, 3, 4)));

		callObject = new HashMap<>();
		callObject.put("value1", "23");
		callObject.put("value2", "singsing");
		callObject.put("value3", Boolean.TRUE);
		callObject.put("modified", "2012-03-29T10:29:16.625Z");
		callMessage = new CallMessage("ujL7WKGXCn8bkvFV", "keyvalue:set", "foobar",
				callObject);
		json = callMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.CALL.getTypeId(),
				"ujL7WKGXCn8bkvFV", "keyvalue:set", "foobar", callObject));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void deserializationTest() throws IOException {
		String json = toJsonArray(2, "7DK6TdN4wLiUJgNM", "http://example.com/api#howdy");
		CallMessage callMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		assertThat(callMessage.getType()).isEqualTo(WampMessageType.CALL);
		assertThat(callMessage.getCallID()).isEqualTo("7DK6TdN4wLiUJgNM");
		assertThat(callMessage.getProcURI()).isEqualTo("http://example.com/api#howdy");
		assertThat(callMessage.getArguments()).isNull();

		json = toJsonArray(2, "Yp9EFZt9DFkuKndg", "api:add2", 23, 99);
		callMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		assertThat(callMessage.getType()).isEqualTo(WampMessageType.CALL);
		assertThat(callMessage.getCallID()).isEqualTo("Yp9EFZt9DFkuKndg");
		assertThat(callMessage.getProcURI()).isEqualTo("api:add2");
		assertThat(callMessage.getArguments()).contains(23, 99);

		Map<String, Object> callObject = new HashMap<>();
		callObject.put("category", "dinner");
		callObject.put("calories", 2309);
		json = toJsonArray(2, "J5DkZJgByutvaDWc", "http://example.com/api#storeMeal",
				callObject);

		callMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		assertThat(callMessage.getType()).isEqualTo(WampMessageType.CALL);
		assertThat(callMessage.getCallID()).isEqualTo("J5DkZJgByutvaDWc");
		assertThat(callMessage.getProcURI())
				.isEqualTo("http://example.com/api#storeMeal");
		assertThat(callMessage.getArguments()).hasSize(1);
		assertThat((Map<String, Object>) callMessage.getArguments().get(0)).contains(
				MapEntry.entry("category", "dinner"), MapEntry.entry("calories", 2309));

		json = toJsonArray(2, "Dns3wuQo0ipOX1Xc", "http://example.com/api#woooat", null);
		callMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		assertThat(callMessage.getType()).isEqualTo(WampMessageType.CALL);
		assertThat(callMessage.getCallID()).isEqualTo("Dns3wuQo0ipOX1Xc");
		assertThat(callMessage.getProcURI()).isEqualTo("http://example.com/api#woooat");
		assertThat(callMessage.getArguments()).hasSize(1).containsNull();

		json = toJsonArray(2, "M0nncaH0ywCSYzRv", "api:sum", Arrays.asList(9, 1, 3, 4));
		callMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		assertThat(callMessage.getType()).isEqualTo(WampMessageType.CALL);
		assertThat(callMessage.getCallID()).isEqualTo("M0nncaH0ywCSYzRv");
		assertThat(callMessage.getProcURI()).isEqualTo("api:sum");
		assertThat(callMessage.getArguments()).hasSize(1)
				.contains(Arrays.asList(9, 1, 3, 4));

		callObject = new HashMap<>();
		callObject.put("value3", Boolean.TRUE);
		callObject.put("value2", "singsing");
		callObject.put("value1", 23);
		callObject.put("modified", "2012-03-29T10:29:16.625Z");

		json = toJsonArray(2, "ujL7WKGXCn8bkvFV", "keyvalue:set", "foobar", callObject);
		callMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		assertThat(callMessage.getType()).isEqualTo(WampMessageType.CALL);
		assertThat(callMessage.getCallID()).isEqualTo("ujL7WKGXCn8bkvFV");
		assertThat(callMessage.getProcURI()).isEqualTo("keyvalue:set");
		assertThat(callMessage.getArguments()).hasSize(2);
		assertThat(callMessage.getArguments().get(0)).isEqualTo("foobar");
		assertThat((Map<String, Object>) callMessage.getArguments().get(1)).contains(
				MapEntry.entry("value3", Boolean.TRUE),
				MapEntry.entry("value2", "singsing"), MapEntry.entry("value1", 23),
				MapEntry.entry("modified", "2012-03-29T10:29:16.625Z"));

	}
}
