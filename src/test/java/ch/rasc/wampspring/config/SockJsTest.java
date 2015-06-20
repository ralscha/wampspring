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
package ch.rasc.wampspring.config;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import ch.rasc.wampspring.annotation.WampCallListener;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.testsupport.BaseWampTest;
import ch.rasc.wampspring.testsupport.CompletableFutureWebSocketHandler;

@SpringApplicationConfiguration(classes = SockJsTest.Config.class)
public class SockJsTest extends BaseWampTest {

	@Override
	protected WebSocketClient createWebSocketClient() {
		List<Transport> transports = new ArrayList<>(2);
		transports.add(new WebSocketTransport(new StandardWebSocketClient()));
		transports.add(new RestTemplateXhrTransport());
		return new SockJsClient(transports);
	}

	@Override
	protected String wampEndpointUrl() {
		return "ws://localhost:" + this.port + "/wampOverSockJS";
	}

	@Test
	public void testCall() throws InterruptedException, ExecutionException,
			TimeoutException, IOException {
		WampMessage response = sendWampMessage(new CallMessage("1", "sum", 2, 4));
		assertThat(response).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) response;
		assertThat(result.getCallID()).isEqualTo("1");
		assertThat(result.getResult()).isEqualTo(6);
	}

	@Test
	public void testPubSub() throws InterruptedException, ExecutionException,
			TimeoutException, IOException {

		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {
			SubscribeMessage subscribeMsg = new SubscribeMessage("/topic");
			webSocketSession.sendMessage(new TextMessage(subscribeMsg
					.toJson(this.jsonFactory)));

			PublishMessage pm = new PublishMessage("/topic", "payload");
			webSocketSession.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("/topic");
			assertThat(event.getEvent()).isEqualTo("payload");

			result.reset();

			UnsubscribeMessage unsubscribeMsg = new UnsubscribeMessage("/topic");
			webSocketSession.sendMessage(new TextMessage(unsubscribeMsg
					.toJson(this.jsonFactory)));

			try {
				pm = new PublishMessage("/topic", "payload2");
				webSocketSession
						.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

				event = (EventMessage) result.getWampMessage();
				Assert.fail("call shoud timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
		}

	}

	@Configuration
	@EnableAutoConfiguration
	@EnableWamp
	static class Config extends AbstractWampConfigurer {
		@Override
		public void registerWampEndpoints(WampEndpointRegistry registry) {
			registry.addEndpoint("/wampOverSockJS").withSockJS();
		}

		@Bean
		TestService testService() {
			return new TestService();
		}
	}

	static class TestService {
		@WampCallListener("sum")
		public int sum(int a, int b) {
			return a + b;
		}
	}

}
