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

public class SubscribeMessageTest extends BaseMessageTest {

	@Test
	public void serializationTest() throws IOException {
		SubscribeMessage subscribeMessage = new SubscribeMessage(
				"http://example.com/simple");

		assertWampMessageTypeHeader(subscribeMessage, WampMessageType.SUBSCRIBE);
		String json = subscribeMessage.toJson(getJsonFactory());
		assertThat(json).isEqualTo(toJsonArray(WampMessageType.SUBSCRIBE.getTypeId(),
				"http://example.com/simple"));
	}

	@Test
	public void deserializationTest() throws IOException {
		String json = toJsonArray(5, "http://example.com/simple");

		SubscribeMessage subscribeMessage = WampMessage.fromJson(getJsonFactory(), json);

		assertWampMessageTypeHeader(subscribeMessage, WampMessageType.SUBSCRIBE);
		assertThat(subscribeMessage.getType()).isEqualTo(WampMessageType.SUBSCRIBE);
		assertThat(subscribeMessage.getTopicURI()).isEqualTo("http://example.com/simple");
		assertThat(subscribeMessage.getDestination())
				.isEqualTo("http://example.com/simple");

	}
}
