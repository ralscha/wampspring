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

import org.junit.Test;

public class UnsubscribeMessageTest extends BaseMessageTest {

	@Test
	public void serializationTest() throws IOException {
		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
				"http://example.com/simple");

		assertWampMessageTypeHeader(unsubscribeMessage, WampMessageType.UNSUBSCRIBE);
		String json = unsubscribeMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.UNSUBSCRIBE.getTypeId(),
						"http://example.com/simple"));
	}

	@Test
	public void deserializationTest() throws IOException {
		String json = toJsonArray(6, "http://example.com/simple");

		UnsubscribeMessage unsubscribeMessage = WampMessage.fromJson(getJsonFactory(),
				json);

		assertWampMessageTypeHeader(unsubscribeMessage, WampMessageType.UNSUBSCRIBE);
		assertThat(unsubscribeMessage.getType()).isEqualTo(WampMessageType.UNSUBSCRIBE);
		assertThat(unsubscribeMessage.getTopicURI()).isEqualTo(
				"http://example.com/simple");
		assertThat(unsubscribeMessage.getDestination()).isEqualTo(
				"http://example.com/simple");
	}
}
