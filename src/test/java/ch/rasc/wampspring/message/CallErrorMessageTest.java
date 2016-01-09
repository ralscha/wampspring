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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CallErrorMessageTest extends BaseMessageTest {

	@Test
	public void serializationTest() throws IOException {
		CallMessage callMessage = new CallMessage("gwbN3EDtFv6JvNV5", "testProcURI");
		CallErrorMessage callErrorMessage = new CallErrorMessage(callMessage,
				"http://autobahn.tavendo.de/error#generic", "math domain error");
		assertWampMessageTypeHeader(callErrorMessage, WampMessageType.CALLERROR);
		String json = callErrorMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.CALLERROR.getTypeId(), "gwbN3EDtFv6JvNV5",
						"http://autobahn.tavendo.de/error#generic", "math domain error"));

		CallErrorMessage callErrorIntMessage = new CallErrorMessage(callMessage,
				"http://example.com/error#number_too_big",
				"1001 too big for me, max is 1000", 1000);
		assertWampMessageTypeHeader(callErrorIntMessage, WampMessageType.CALLERROR);
		json = callErrorIntMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.CALLERROR.getTypeId(),
				"gwbN3EDtFv6JvNV5", "http://example.com/error#number_too_big",
				"1001 too big for me, max is 1000", 1000));

		callMessage = new CallMessage("7bVW5pv8r60ZeL6u", "testProcURI");
		CallErrorMessage listCallErrorMessage = new CallErrorMessage(callMessage,
				"http://example.com/error#invalid_numbers",
				"one or more numbers are multiples of 3", Arrays.asList(0, 3));
		assertWampMessageTypeHeader(listCallErrorMessage, WampMessageType.CALLERROR);
		json = listCallErrorMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.CALLERROR.getTypeId(),
				"7bVW5pv8r60ZeL6u", "http://example.com/error#invalid_numbers",
				"one or more numbers are multiples of 3", Arrays.asList(0, 3)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void deserializationTest() throws IOException {
		String json = toJsonArray(4, "gwbN3EDtFv6JvNV5",
				"http://autobahn.tavendo.de/error#generic", "math domain error");
		CallErrorMessage callErrorMessage = WampMessage.fromJson(getJsonFactory(), json);
		assertWampMessageTypeHeader(callErrorMessage, WampMessageType.CALLERROR);
		assertThat(callErrorMessage.getType()).isEqualTo(WampMessageType.CALLERROR);
		assertThat(callErrorMessage.getCallID()).isEqualTo("gwbN3EDtFv6JvNV5");
		assertThat(callErrorMessage.getErrorURI())
				.isEqualTo("http://autobahn.tavendo.de/error#generic");
		assertThat(callErrorMessage.getErrorDesc()).isEqualTo("math domain error");
		assertThat(callErrorMessage.getErrorDetails()).isNull();

		json = toJsonArray(4, "7bVW5pv8r60ZeL6u",
				"http://example.com/error#number_too_big",
				"1001 too big for me, max is 1000", 1000);
		CallErrorMessage intCallErrorMessage = WampMessage.fromJson(getJsonFactory(),
				json);
		assertWampMessageTypeHeader(intCallErrorMessage, WampMessageType.CALLERROR);
		assertThat(intCallErrorMessage.getType()).isEqualTo(WampMessageType.CALLERROR);
		assertThat(intCallErrorMessage.getCallID()).isEqualTo("7bVW5pv8r60ZeL6u");
		assertThat(intCallErrorMessage.getErrorURI())
				.isEqualTo("http://example.com/error#number_too_big");
		assertThat(intCallErrorMessage.getErrorDesc())
				.isEqualTo("1001 too big for me, max is 1000");
		assertThat(intCallErrorMessage.getErrorDetails()).isEqualTo(1000);

		json = toJsonArray(4, "AStPd8RS60pfYP8c",
				"http://example.com/error#invalid_numbers",
				"one or more numbers are multiples of 3", Arrays.asList(0, 3));

		CallErrorMessage listCallErrorMessage = WampMessage.fromJson(getJsonFactory(),
				json);
		assertWampMessageTypeHeader(listCallErrorMessage, WampMessageType.CALLERROR);
		assertThat(listCallErrorMessage.getType()).isEqualTo(WampMessageType.CALLERROR);
		assertThat(listCallErrorMessage.getCallID()).isEqualTo("AStPd8RS60pfYP8c");
		assertThat(listCallErrorMessage.getErrorURI())
				.isEqualTo("http://example.com/error#invalid_numbers");
		assertThat(listCallErrorMessage.getErrorDesc())
				.isEqualTo("one or more numbers are multiples of 3");
		assertThat((List) listCallErrorMessage.getErrorDetails()).contains(0, 3);

	}

	@Test
	public void copyConstructorTest() {
		CallMessage callMessage = new CallMessage("2", "procURI");
		CallErrorMessage result = new CallErrorMessage(callMessage, "errorURI",
				"description");
		assertThat(result.getCallID()).isEqualTo("2");
		assertThat(result.getErrorURI()).isEqualTo("errorURI");
		assertThat(result.getErrorDesc()).isEqualTo("description");
		assertThat(result.getErrorDetails()).isNull();

		assertWampMessageTypeHeader(callMessage, WampMessageType.CALL);
		assertWampMessageTypeHeader(result, WampMessageType.CALLERROR);
	}
}
