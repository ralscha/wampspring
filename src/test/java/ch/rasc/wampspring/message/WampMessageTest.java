/**
 * Copyright 2014-2017 Ralph Schaer <ralphschaer@gmail.com>
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

import java.security.Principal;

import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.config.WampSession;
import ch.rasc.wampspring.testsupport.TestPrincipal;

public class WampMessageTest {

	@Test
	public void headerTest() {
		CallMessage callMessage = new CallMessage("1", "call");

		TestPrincipal testPrincipal = new TestPrincipal("ralph");
		callMessage.setHeader(WampMessageHeader.PRINCIPAL, testPrincipal);

		@SuppressWarnings("resource")
		WebSocketSession nativeSession = Mockito.mock(WebSocketSession.class);
		Mockito.when(nativeSession.getId()).thenReturn("ws1");
		WampSession wampSession = new WampSession(nativeSession);

		callMessage.setHeader(WampMessageHeader.WAMP_SESSION, wampSession);

		callMessage.setHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, "ws1");

		callMessage.setHeader(WampMessageHeader.WAMP_MESSAGE_TYPE,
				WampMessageType.CALLERROR);

		assertThat((Principal) callMessage.getHeader(WampMessageHeader.PRINCIPAL))
				.isEqualTo(testPrincipal);
		assertThat((WampSession) callMessage.getHeader(WampMessageHeader.WAMP_SESSION))
				.isEqualTo(wampSession);
		assertThat((String) callMessage.getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID))
				.isEqualTo("ws1");
		assertThat((WampMessageType) callMessage
				.getHeader(WampMessageHeader.WAMP_MESSAGE_TYPE))
						.isEqualTo(WampMessageType.CALLERROR);

		assertThat(callMessage.getPrincipal()).isEqualTo(testPrincipal);
		assertThat(callMessage.getWampSession()).isEqualTo(wampSession);
		assertThat(callMessage.getWebSocketSessionId()).isEqualTo("ws1");
		assertThat(callMessage.getType()).isEqualTo(WampMessageType.CALLERROR);

		MessageHeaders messageHeaders = callMessage.getHeaders();
		assertThat(messageHeaders).hasSize(4);
		assertThat(messageHeaders).contains(MapEntry.entry("PRINCIPAL", testPrincipal),
				MapEntry.entry("WAMP_SESSION", wampSession),
				MapEntry.entry("WEBSOCKET_SESSION_ID", "ws1"),
				MapEntry.entry("WAMP_MESSAGE_TYPE", WampMessageType.CALLERROR));
	}

	@Test
	public void isPubSubMessageTest() {
		assertThat(PubSubMessage.class.isAssignableFrom(CallErrorMessage.class))
				.isFalse();
		assertThat(PubSubMessage.class.isAssignableFrom(CallMessage.class)).isFalse();
		assertThat(PubSubMessage.class.isAssignableFrom(CallResultMessage.class))
				.isFalse();
		assertThat(PubSubMessage.class.isAssignableFrom(EventMessage.class)).isTrue();
		assertThat(PubSubMessage.class.isAssignableFrom(PrefixMessage.class)).isFalse();
		assertThat(PubSubMessage.class.isAssignableFrom(PublishMessage.class)).isTrue();
		assertThat(PubSubMessage.class.isAssignableFrom(SubscribeMessage.class)).isTrue();
		assertThat(PubSubMessage.class.isAssignableFrom(UnsubscribeMessage.class))
				.isTrue();
		assertThat(PubSubMessage.class.isAssignableFrom(WelcomeMessage.class)).isFalse();
	}
}
