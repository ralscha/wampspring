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

import org.junit.Test;

public class PrefixMessageTest extends BaseMessageTest {

	@Test
	public void serializationTest() throws IOException {
		String prefix = "news:read";
		String uri = "http://example.com/simple/news#";
		PrefixMessage prefixMessage = new PrefixMessage(prefix, uri);

		String json = prefixMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.PREFIX.getTypeId(), prefix, uri));
	}

	@Test
	public void deserializationTest() throws IOException {
		String json = toJsonArray(1, "news:read", "http://example.com/simple/news#");

		PrefixMessage wm = WampMessage.fromJson(jsonFactory, json);

		assertThat(wm.getType()).isEqualTo(WampMessageType.PREFIX);
		assertThat(wm.getPrefix()).isEqualTo("news:read");
		assertThat(wm.getUri()).isEqualTo("http://example.com/simple/news#");

	}
}
