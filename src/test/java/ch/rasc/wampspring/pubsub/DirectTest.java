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
package ch.rasc.wampspring.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.config.DefaultWampConfiguration;
import ch.rasc.wampspring.config.WampMessageSelector;
import ch.rasc.wampspring.config.WampMessageSelectors;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.testsupport.BaseWampTest;
import ch.rasc.wampspring.testsupport.CompletableFutureWebSocketHandler;

@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT, classes = DirectTest.Config.class)
public class DirectTest extends BaseWampTest {

	@Test
	public void testDirect() throws InterruptedException, ExecutionException, IOException,
			TimeoutException {
		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result3 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2);
				WebSocketSession webSocketSession3 = startWebSocketSession(result3)) {

			// Client 1 subscribe
			SubscribeMessage subscribeMsg = new SubscribeMessage("/topic/1");
			webSocketSession1
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));
			waitForMessage(result1);

			// Client 2 subscribe
			result1.reset(1);
			subscribeMsg = new SubscribeMessage("/topic/2");
			webSocketSession2
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));
			waitForMessage(result2);
			EventMessage response1 = (EventMessage) result1.getWampMessage();
			assertThat(response1.getTopicURI()).isEqualTo("/topic");
			assertThat(response1.getEvent()).isEqualTo("join:2");

			// Client 3 subscribe
			result1.reset(1);
			result2.reset(1);
			subscribeMsg = new SubscribeMessage("/topic/3");
			webSocketSession3
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));
			waitForMessage(result3);
			response1 = (EventMessage) result1.getWampMessage();
			assertThat(response1.getTopicURI()).isEqualTo("/topic");
			assertThat(response1.getEvent()).isEqualTo("join:3");
			EventMessage response2 = (EventMessage) result2.getWampMessage();
			assertThat(response2.getTopicURI()).isEqualTo("/topic");
			assertThat(response2.getEvent()).isEqualTo("join:3");

			// Client 1 publish
			result1.reset(1);
			result2.reset(1);
			result3.reset(1);
			webSocketSession1.sendMessage(
					new TextMessage(new PublishMessage("/topic", "fromClient1")
							.toJson(this.jsonFactory)));
			response1 = (EventMessage) result1.getWampMessage();
			assertThat(response1.getTopicURI()).isEqualTo("/topic");
			assertThat(response1.getEvent()).isEqualTo("publish:fromClient1:1");
			response2 = (EventMessage) result2.getWampMessage();
			assertThat(response2.getTopicURI()).isEqualTo("/topic");
			assertThat(response2.getEvent()).isEqualTo("publish:fromClient1:1");
			EventMessage response3 = (EventMessage) result3.getWampMessage();
			assertThat(response3.getTopicURI()).isEqualTo("/topic");
			assertThat(response3.getEvent()).isEqualTo("publish:fromClient1:1");

			// Client 2 unsubscribe
			result1.reset(1);
			result2.reset();
			result3.reset(1);
			UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage("/topic");
			webSocketSession2.sendMessage(
					new TextMessage(unsubscribeMessage.toJson(this.jsonFactory)));
			waitForMessage(result2);
			response1 = (EventMessage) result1.getWampMessage();
			assertThat(response1.getTopicURI()).isEqualTo("/topic");
			assertThat(response1.getEvent()).isEqualTo("leave:2");
			response3 = (EventMessage) result3.getWampMessage();
			assertThat(response3.getTopicURI()).isEqualTo("/topic");
			assertThat(response3.getEvent()).isEqualTo("leave:2");

			// Client 3 publish
			result1.reset(1);
			result2.reset();
			result3.reset(1);
			webSocketSession3.sendMessage(
					new TextMessage(new PublishMessage("/topic", "fromClient3")
							.toJson(this.jsonFactory)));
			response1 = (EventMessage) result1.getWampMessage();
			assertThat(response1.getTopicURI()).isEqualTo("/topic");
			assertThat(response1.getEvent()).isEqualTo("publish:fromClient3:3");
			waitForMessage(result2);
			response3 = (EventMessage) result3.getWampMessage();
			assertThat(response3.getTopicURI()).isEqualTo("/topic");
			assertThat(response3.getEvent()).isEqualTo("publish:fromClient3:3");
		}
	}

	private static void waitForMessage(CompletableFutureWebSocketHandler result1) {
		try {
			result1.getWampMessages();
			Assert.fail("has to fail with a timeout exception");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(TimeoutException.class);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config extends DefaultWampConfiguration {

		@Override
		protected WampMessageSelector brokerMessageHandlerMessageSelector() {
			return WampMessageSelectors.REJECT_ALL;
		}

		@Bean
		public DirectService directService() {
			return new DirectService(eventMessenger());
		}

	}

}
