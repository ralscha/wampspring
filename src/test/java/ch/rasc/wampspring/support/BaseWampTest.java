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
package ch.rasc.wampspring.support;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import ch.rasc.wampspring.cra.DefaultAuthenticationHandler;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseWampTest {

	protected final JsonFactory jsonFactory = new MappingJsonFactory(new ObjectMapper());

	@Value("${local.server.port}")
	protected String port;

	protected WampMessage sendWampMessage(WampMessage msg) throws IOException,
			InterruptedException, ExecutionException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);

		WebSocketClient webSocketClient = new StandardWebSocketClient();
		try (WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				wampEndpointUrl()).get()) {
			webSocketSession.sendMessage(new TextMessage(msg.toJson(jsonFactory)));
			WampMessage response = result.getWampMessage();
			return response;
		}
	}

	protected WebSocketSession startWebSocketSession(AbstractWebSocketHandler result)
			throws InterruptedException, ExecutionException {
		WebSocketClient webSocketClient = new StandardWebSocketClient();
		return webSocketClient.doHandshake(result, wampEndpointUrl()).get();
	}

	protected String wampEndpointUrl() {
		return "ws://localhost:" + port + "/wamp";
	}

	protected WampMessage sendAuthenticatedMessage(WampMessage msg) throws Exception {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);

		WebSocketClient webSocketClient = new StandardWebSocketClient();
		try (WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				wampEndpointUrl()).get()) {

			authenticate(result, webSocketSession);

			webSocketSession.sendMessage(new TextMessage(msg.toJson(jsonFactory)));
			WampMessage response = result.getWampMessage();
			return response;
		}
	}

	protected void authenticate(ResultWebSocketHandler result,
			WebSocketSession webSocketSession) throws IOException, InterruptedException,
			InvalidKeyException, NoSuchAlgorithmException {
		CallMessage authReqCallMessage = new CallMessage("1",
				"http://api.wamp.ws/procedure#authreq", "a", Collections.emptyMap());
		webSocketSession.sendMessage(new TextMessage(authReqCallMessage
				.toJson(jsonFactory)));
		WampMessage response = result.getWampMessage();

		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage resultMessage = (CallResultMessage) response;
		assertThat(resultMessage.getCallID()).isEqualTo("1");
		assertThat(resultMessage.getResult()).isNotNull();

		result.reset();

		String challengeBase64 = (String) resultMessage.getResult();
		String signature = DefaultAuthenticationHandler.generateHMacSHA256("secretofa",
				challengeBase64);

		CallMessage authCallMessage = new CallMessage("2",
				"http://api.wamp.ws/procedure#auth", signature);
		webSocketSession
				.sendMessage(new TextMessage(authCallMessage.toJson(jsonFactory)));
		response = result.getWampMessage();

		assertThat(response).isInstanceOf(CallResultMessage.class);
		resultMessage = (CallResultMessage) response;
		assertThat(resultMessage.getCallID()).isEqualTo("2");
		assertThat(resultMessage.getResult()).isNull();

		result.reset();
	}

}
