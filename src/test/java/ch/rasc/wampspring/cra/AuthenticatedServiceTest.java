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
package ch.rasc.wampspring.cra;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Collections;

import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.config.EnableWamp;
import ch.rasc.wampspring.config.WampConfigurerAdapter;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.support.AbstractWebSocketIntegrationTests;
import ch.rasc.wampspring.support.ResultWebSocketHandler;

public class AuthenticatedServiceTest extends AbstractWebSocketIntegrationTests {

	@Test
	public void testWithoutAuthentication() throws Exception {
		CallMessage sumMessage = new CallMessage("12", "callService.sum", 3, 4);
		WampMessage receivedMessage = sendWampMessage(sumMessage);
		assertThat(receivedMessage).isNull();
	}

	@Test
	public void testWithAuthentication() throws Exception {

		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);

		try (WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get()) {

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
			String signature = DefaultAuthenticationHandler.generateHMacSHA256(
					"secretofa", challengeBase64);

			CallMessage authCallMessage = new CallMessage("2",
					"http://api.wamp.ws/procedure#auth", signature);
			webSocketSession.sendMessage(new TextMessage(authCallMessage
					.toJson(jsonFactory)));
			response = result.getWampMessage();

			assertThat(response).isInstanceOf(CallResultMessage.class);
			resultMessage = (CallResultMessage) response;
			assertThat(resultMessage.getCallID()).isEqualTo("2");
			assertThat(resultMessage.getResult()).isNull();

			result.reset();

			CallMessage sumMessage = new CallMessage("12", "callService.sum", 3, 4);
			webSocketSession.sendMessage(new TextMessage(sumMessage.toJson(jsonFactory)));
			response = result.getWampMessage();
			assertThat(response).isInstanceOf(CallResultMessage.class);
			resultMessage = (CallResultMessage) response;
			assertThat(resultMessage.getCallID()).isEqualTo("12");
			assertThat(resultMessage.getResult()).isEqualTo(7);
		}
	}

	@Override
	protected String wampEndpointPath() {
		return "/ws";
	}

	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] { Config.class };
	}

	@Configuration
	@EnableWamp
	static class Config extends WampConfigurerAdapter {
		@Bean
		public AuthenticatedService callService() {
			return new AuthenticatedService();
		}

		@Override
		public String wampEndpointPath() {
			return "/ws";
		}

		@Override
		public AuthenticationSecretProvider authenticationSecretProvider() {
			return new TestSecretProvider();
		}

		@Override
		public boolean authenticationRequired() {
			return true;
		}

	}

}
