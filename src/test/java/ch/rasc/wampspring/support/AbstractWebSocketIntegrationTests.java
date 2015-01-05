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
/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import ch.rasc.wampspring.cra.DefaultAuthenticationHandler;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractWebSocketIntegrationTests {

	private final Log logger = LogFactory.getLog(getClass());

	private final TomcatWebSocketTestServer server = new TomcatWebSocketTestServer();

	protected final StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

	private final ObjectMapper objectMapper = new ObjectMapper();

	protected final JsonFactory jsonFactory = new MappingJsonFactory(objectMapper);

	protected AnnotationConfigWebApplicationContext wac;

	@Before
	public void setup() throws Exception {

		this.wac = new AnnotationConfigWebApplicationContext();
		this.wac.register(getAnnotatedConfigClasses());
		this.wac.register(TomcatUpgradeStrategyConfig.class);
		this.wac.refresh();

		if (this.webSocketClient instanceof Lifecycle) {
			((Lifecycle) this.webSocketClient).start();
		}

		this.server.deployConfig(this.wac);
		this.server.start();
	}

	protected abstract Class<?>[] getAnnotatedConfigClasses();

	@After
	public void teardown() throws Exception {
		try {
			if (this.webSocketClient instanceof Lifecycle) {
				((Lifecycle) this.webSocketClient).stop();
			}
		}
		catch (Throwable t) {
			logger.error("Failed to stop WebSocket client", t);
		}

		try {
			this.server.undeployConfig();
		}
		catch (Throwable t) {
			logger.error("Failed to undeploy application config", t);
		}

		try {
			this.server.stop();
		}
		catch (Throwable t) {
			logger.error("Failed to stop server", t);
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

	protected String getWsBaseUrl() {
		return "ws://localhost:" + this.server.getPort();
	}

	protected ListenableFuture<WebSocketSession> doHandshake(
			WebSocketHandler clientHandler, String endpointPath) {
		return this.webSocketClient.doHandshake(clientHandler, getWsBaseUrl()
				+ endpointPath);
	}

	static abstract class AbstractRequestUpgradeStrategyConfig {

		@Bean
		public DefaultHandshakeHandler handshakeHandler() {
			return new DefaultHandshakeHandler(requestUpgradeStrategy());
		}

		public abstract RequestUpgradeStrategy requestUpgradeStrategy();
	}

	@Configuration
	static class TomcatUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

		@Override
		@Bean
		public RequestUpgradeStrategy requestUpgradeStrategy() {
			return new TomcatRequestUpgradeStrategy();
		}
	}

	protected void runInWebSocketSession(
			AbstractTestWebSocketHandler callClientWebSocketHandler)
			throws InterruptedException, ExecutionException, IOException {
		try (WebSocketSession webSocketSession = this.webSocketClient.doHandshake(
				callClientWebSocketHandler, getWsBaseUrl() + wampEndpointPath()).get()) {
			callClientWebSocketHandler.waitForConversationEnd();
		}
	}

	protected WebSocketSession startWebSocketSession(
			AbstractTestWebSocketHandler callClientWebSocketHandler)
			throws InterruptedException, ExecutionException {
		return this.webSocketClient.doHandshake(callClientWebSocketHandler,
				getWsBaseUrl() + wampEndpointPath()).get();
	}

	protected String wampEndpointPath() {
		return "/wamp";
	}

	protected WampMessage sendWampMessage(WampMessage msg) throws IOException,
			InterruptedException, ExecutionException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);

		try (WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get()) {
			webSocketSession.sendMessage(new TextMessage(msg.toJson(jsonFactory)));
			WampMessage response = result.getWampMessage();
			return response;
		}
	}

	protected WampMessage sendAuthenticatedMessage(WampMessage msg) throws Exception {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);

		try (WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get()) {

			authenticate(result, webSocketSession);

			webSocketSession.sendMessage(new TextMessage(msg.toJson(jsonFactory)));
			WampMessage response = result.getWampMessage();
			return response;
		}
	}

}
