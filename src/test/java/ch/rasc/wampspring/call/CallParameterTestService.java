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
package ch.rasc.wampspring.call;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

import ch.rasc.wampspring.annotation.WampCallListener;
import ch.rasc.wampspring.config.WampSession;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.WampMessageHeader;
import ch.rasc.wampspring.message.WampMessageType;

@Service
public class CallParameterTestService {

	@WampCallListener(value = "headerMethod")
	public String headerMethod(WampSession wampSession,
			@Header(value = "WEBSOCKET_SESSION_ID") String webSocketSessionId) {
		return "headerMethod called: "
				+ wampSession.getWebSocketSessionId().equals(webSocketSessionId);
	}

	@WampCallListener(value = "headersMethod")
	public String headersMethod(@Headers Map<String, Object> headers) {

		assertThat(headers).hasSize(4).containsKey("WAMP_SESSION")
				.containsKey("PRINCIPAL").containsKey("WEBSOCKET_SESSION_ID")
				.containsKey("WAMP_MESSAGE_TYPE");

		assertThat(
				(WampMessageType) headers.get(WampMessageHeader.WAMP_MESSAGE_TYPE.name()))
						.isEqualTo(WampMessageType.CALL);

		return "headersMethod called";
	}

	@WampCallListener(value = "wampSesionMethod")
	public String wampSesionMethod(WampSession wampSession) {
		assertThat(wampSession).isNotNull();
		return "wampSesionMethod called";
	}

	@WampCallListener(value = "messageMethod")
	public String messageMethod(CallMessage message) {
		return "messageMethod called: " + message.getCallID() + "/"
				+ message.getProcURI();
	}

	@WampCallListener(value = "messageMethod/{id}")
	public String mix(String param1, CallMessage message, int param2,
			WampSession wampSession, @Headers Map<String, Object> headers,
			@Header(value = "WAMP_MESSAGE_TYPE") WampMessageType wampMessageType,
			float param3, @DestinationVariable("id") int id, String param4) {

		assertThat(param1).isEqualTo("param1");
		assertThat(message).isNotNull();
		assertThat(message.getDestination()).isEqualTo("messageMethod/23");
		assertThat(param2).isEqualTo(2);
		assertThat(wampSession).isNotNull();
		assertThat(headers).hasSize(5);
		assertThat(wampMessageType).isEqualTo(WampMessageType.CALL);
		assertThat(param3).isEqualTo(3.3f);
		assertThat(id).isEqualTo(23);
		assertThat(param4).isEqualTo("param4");

		return "mix";
	}

}
