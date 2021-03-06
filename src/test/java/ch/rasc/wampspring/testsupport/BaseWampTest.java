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
package ch.rasc.wampspring.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.rasc.wampspring.cra.DefaultAuthenticationHandler;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Ignore
public class BaseWampTest {

	protected final JsonFactory jsonFactory = new MappingJsonFactory(new ObjectMapper());

	@LocalServerPort
	public int actualPort;

	protected WampMessage sendWampMessage(WampMessage msg) throws InterruptedException,
			ExecutionException, TimeoutException, IOException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		WebSocketClient webSocketClient = createWebSocketClient();

		try (WebSocketSession webSocketSession = webSocketClient
				.doHandshake(result, wampEndpointUrl()).get()) {

			result.getWelcomeMessage();
			webSocketSession.sendMessage(new TextMessage(msg.toJson(this.jsonFactory)));

			return result.getWampMessage();
		}
	}

	protected WebSocketClient createWebSocketClient() {
		return new StandardWebSocketClient();
	}

	protected WebSocketSession startWebSocketSession(AbstractWebSocketHandler result)
			throws InterruptedException, ExecutionException {
		WebSocketClient webSocketClient = createWebSocketClient();
		return webSocketClient.doHandshake(result, wampEndpointUrl()).get();
	}

	protected String wampEndpointUrl() {
		return "ws://localhost:" + this.actualPort + "/wamp";
	}

	protected WampMessage sendAuthenticatedMessage(WampMessage msg) throws Exception {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		WebSocketClient webSocketClient = createWebSocketClient();
		try (WebSocketSession webSocketSession = webSocketClient
				.doHandshake(result, wampEndpointUrl()).get()) {

			result.getWelcomeMessage();
			authenticate(result, webSocketSession);
			webSocketSession.sendMessage(new TextMessage(msg.toJson(this.jsonFactory)));

			return result.getWampMessage();
		}
	}

	protected void authenticate(CompletableFutureWebSocketHandler result,
			WebSocketSession webSocketSession)
			throws IOException, InterruptedException, InvalidKeyException,
			NoSuchAlgorithmException, ExecutionException, TimeoutException {
		CallMessage authReqCallMessage = new CallMessage("1",
				"http://api.wamp.ws/procedure#authreq", "a", Collections.emptyMap());
		webSocketSession.sendMessage(
				new TextMessage(authReqCallMessage.toJson(this.jsonFactory)));
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
				.sendMessage(new TextMessage(authCallMessage.toJson(this.jsonFactory)));
		response = result.getWampMessage();

		assertThat(response).isInstanceOf(CallResultMessage.class);
		resultMessage = (CallResultMessage) response;
		assertThat(resultMessage.getCallID()).isEqualTo("2");
		assertThat(resultMessage.getResult()).isNull();

		result.reset();
	}

}
