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

public class WelcomeMessageTest extends BaseMessageTest {

	@Test
	public void serializationTest() throws IOException {
		String sessionId = "someSession";
		String serverIdent = "someServer";
		WelcomeMessage welcomeMessage = new WelcomeMessage(sessionId, serverIdent);

		String json = welcomeMessage.toJson(jsonFactory);
		assertThat(json).isEqualTo(
				toJsonArray(WampMessageType.WELCOME.getTypeId(), sessionId, WelcomeMessage.PROTOCOL_VERSION,
						serverIdent));
	}

	@Test
	public void deserializationTest() throws IOException {
		String json = toJsonArray(0, "v59mbCGDXZ7WTyxB", 1, "Autobahn/0.5.1");

		WelcomeMessage wm = WampMessage.fromJson(jsonFactory, json);

		assertThat(wm.getType()).isEqualTo(WampMessageType.WELCOME);
		assertThat(wm.getSessionId()).isEqualTo("v59mbCGDXZ7WTyxB");
		assertThat(wm.getServerIdent()).isEqualTo("Autobahn/0.5.1");
		assertThat(wm.getProtocolVersion()).isEqualTo(1);

	}
}