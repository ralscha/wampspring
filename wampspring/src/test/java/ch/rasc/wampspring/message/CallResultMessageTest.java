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

public class CallResultMessageTest extends BaseMessageTest {

	@Test
	public void serializationTest() throws IOException {
		CallMessage callMessage = new CallMessage("CcDnuI2bl2oLGBzO", "testProcURI");
		CallResultMessage callResultMessage = new CallResultMessage(callMessage,
				"Hello, I am a simple event.");
		String json = callResultMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.CALLRESULT.getTypeId(), "CcDnuI2bl2oLGBzO",
						"Hello, I am a simple event."));

		callResultMessage = new CallResultMessage(callMessage, null);
		json = callResultMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.CALLRESULT.getTypeId(), "CcDnuI2bl2oLGBzO",
						null));

		Map<String, Object> eventObject = new HashMap<>();
		eventObject.put("value3", Boolean.TRUE);
		eventObject.put("value2", "singsing");
		eventObject.put("value1", 23);
		eventObject.put("modified", "2012-03-29T10:29:16.625Z");

		CallResultMessage mapCallResultMessage = new CallResultMessage(callMessage,
				eventObject);
		json = mapCallResultMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.CALLRESULT.getTypeId(), "CcDnuI2bl2oLGBzO",
						eventObject));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void deserializationTest() throws IOException {
		String json = toJsonArray(3, "CcDnuI2bl2oLGBzO", null);
		CallResultMessage callResultMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(callResultMessage.getType()).isEqualTo(WampMessageType.CALLRESULT);
		assertThat(callResultMessage.getCallID()).isEqualTo("CcDnuI2bl2oLGBzO");
		assertThat(callResultMessage.getResult()).isNull();

		json = toJsonArray(3, "otZom9UsJhrnzvLa", "Awesome result ..");
		callResultMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(callResultMessage.getType()).isEqualTo(WampMessageType.CALLRESULT);
		assertThat(callResultMessage.getCallID()).isEqualTo("otZom9UsJhrnzvLa");
		assertThat(callResultMessage.getResult()).isEqualTo("Awesome result ..");

		Map<String, Object> eventObject = new HashMap<>();
		eventObject.put("value3", Boolean.TRUE);
		eventObject.put("value2", "singsing");
		eventObject.put("value1", 23);
		eventObject.put("modified", "2012-03-29T10:29:16.625Z");

		json = toJsonArray(3, "CcDnuI2bl2oLGBzO", eventObject);

		CallResultMessage mapCallResultMessage = WampMessage.fromJson(jsonFactory, json);
		assertThat(mapCallResultMessage.getType()).isEqualTo(WampMessageType.CALLRESULT);
		assertThat(mapCallResultMessage.getCallID()).isEqualTo("CcDnuI2bl2oLGBzO");
		assertThat((Map) mapCallResultMessage.getResult()).hasSize(4).contains(
				MapEntry.entry("value3", Boolean.TRUE),
				MapEntry.entry("value2", "singsing"), MapEntry.entry("value1", 23),
				MapEntry.entry("modified", "2012-03-29T10:29:16.625Z"));
	}

	@Test
	public void copyConstructorTest() {
		CallMessage callMessage = new CallMessage("1", "procURI");
		CallResultMessage result = new CallResultMessage(callMessage, "response");
		assertThat(result.getCallID()).isEqualTo("1");
		assertThat(result.getResult()).isEqualTo("response");
	}
}
