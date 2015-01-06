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
package ch.rasc.wampspring.pubsub;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.config.EnableWamp;
import ch.rasc.wampspring.config.WampConfigurerAdapter;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.support.AbstractWebSocketIntegrationTests;
import ch.rasc.wampspring.support.MultiResultWebSocketHandler;
import ch.rasc.wampspring.support.ResultWebSocketHandler;

public class PubSubReplyAnnotationTest extends AbstractWebSocketIntegrationTests {

	@Test
	public void testPublish1() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo1");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		PublishMessage pm = new PublishMessage("pubSubService.incomingPublish1",
				"testPublish1");
		webSocketSession.sendMessage(new TextMessage(pm.toJson(jsonFactory)));

		EventMessage event = (EventMessage) result.getWampMessage();
		assertThat(event.getTopicURI()).isEqualTo("replyTo1");
		assertThat(event.getEvent()).isEqualTo("return1:testPublish1");

		webSocketSession.close();
	}

	@Test
	public void testPublish2() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo2");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		PublishMessage pm = new PublishMessage("pubSubService.incomingPublish2",
				"testPublish2");
		webSocketSession.sendMessage(new TextMessage(pm.toJson(jsonFactory)));

		EventMessage event = (EventMessage) result.getWampMessage();
		assertThat(event).isNull();

		webSocketSession.close();
	}

	@Test
	public void testPublish3() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo3");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		PublishMessage pm = new PublishMessage("incomingPublish3", "testPublish3");
		webSocketSession.sendMessage(new TextMessage(pm.toJson(jsonFactory)));

		EventMessage event = (EventMessage) result.getWampMessage();
		assertThat(event.getTopicURI()).isEqualTo("replyTo3");
		assertThat(event.getEvent()).isEqualTo("return3:testPublish3");

		webSocketSession.close();
	}

	@Test
	public void testPublish4() throws InterruptedException, ExecutionException,
			IOException {
		MultiResultWebSocketHandler result = new MultiResultWebSocketHandler(3,
				jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		webSocketSession.sendMessage(new TextMessage(new SubscribeMessage("replyTo4_1")
				.toJson(jsonFactory)));
		webSocketSession.sendMessage(new TextMessage(new SubscribeMessage("replyTo4_2")
				.toJson(jsonFactory)));
		webSocketSession.sendMessage(new TextMessage(new SubscribeMessage("replyTo4_3")
				.toJson(jsonFactory)));

		PublishMessage pm = new PublishMessage("incomingPublish4", "testPublish4");
		webSocketSession.sendMessage(new TextMessage(pm.toJson(jsonFactory)));

		List<WampMessage> resultList = result.getWampMessages();

		for (int i = 0; i < resultList.size(); i++) {
			EventMessage eventMessage = (EventMessage) resultList.get(i);
			assertThat(eventMessage.getTopicURI()).isEqualTo("replyTo4_" + (i + 1));
			assertThat(eventMessage.getEvent()).isEqualTo("return4:testPublish4");
		}

		webSocketSession.close();
	}

	@Test
	public void testSubscribe1() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo1");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		subscribeMsg = new SubscribeMessage("pubSubService.incomingSubscribe1");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		EventMessage event = (EventMessage) result.getWampMessage();
		assertThat(event.getTopicURI()).isEqualTo("replyTo1");
		assertThat(event.getEvent()).isEqualTo("returnSub1");

		webSocketSession.close();
	}

	@Test
	public void testSubscribe2() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo2");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		subscribeMsg = new SubscribeMessage("pubSubService.incomingSubscribe2");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		EventMessage event = (EventMessage) result.getWampMessage();
		assertThat(event).isNull();

		webSocketSession.close();
	}

	@Test
	public void testSubscribe3() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo3");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		subscribeMsg = new SubscribeMessage("incomingSub3");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		EventMessage event = (EventMessage) result.getWampMessage();
		assertThat(event.getTopicURI()).isEqualTo("replyTo3");
		assertThat(event.getEvent()).isEqualTo("returnSub3");

		webSocketSession.close();
	}

	@Test
	public void testSubscribe4() throws InterruptedException, ExecutionException,
			IOException {
		MultiResultWebSocketHandler result = new MultiResultWebSocketHandler(3,
				jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		webSocketSession.sendMessage(new TextMessage(new SubscribeMessage("replyTo4_1")
				.toJson(jsonFactory)));
		webSocketSession.sendMessage(new TextMessage(new SubscribeMessage("replyTo4_2")
				.toJson(jsonFactory)));
		webSocketSession.sendMessage(new TextMessage(new SubscribeMessage("replyTo4_3")
				.toJson(jsonFactory)));

		SubscribeMessage subscribeMsg = new SubscribeMessage("incomingSub4");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		List<WampMessage> resultList = result.getWampMessages();

		for (int i = 0; i < resultList.size(); i++) {
			EventMessage eventMessage = (EventMessage) resultList.get(i);
			assertThat(eventMessage.getTopicURI()).isEqualTo("replyTo4_" + (i + 1));
			assertThat(eventMessage.getEvent()).isEqualTo("returnSub4");
		}

		webSocketSession.close();
	}

	@Test
	public void testUnsubscribe1() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo1");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
				"pubSubService.incomingUnsubscribe1");
		webSocketSession.sendMessage(new TextMessage(unsubscribeMessage
				.toJson(jsonFactory)));

		EventMessage event = (EventMessage) result.getWampMessage();
		assertThat(event.getTopicURI()).isEqualTo("replyTo1");
		assertThat(event.getEvent()).isEqualTo("returnUnsub1");

		webSocketSession.close();
	}

	@Test
	public void testUnsubscribe2() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo2");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
				"pubSubService.incomingUnsubscribe2");
		webSocketSession.sendMessage(new TextMessage(unsubscribeMessage
				.toJson(jsonFactory)));

		EventMessage event = (EventMessage) result.getWampMessage();
		assertThat(event).isNull();

		webSocketSession.close();
	}

	@Test
	public void testUnsubscribe3() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo3");
		webSocketSession.sendMessage(new TextMessage(subscribeMsg.toJson(jsonFactory)));

		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage("incomingUnsub3");
		webSocketSession.sendMessage(new TextMessage(unsubscribeMessage
				.toJson(jsonFactory)));

		EventMessage event = (EventMessage) result.getWampMessage();
		assertThat(event.getTopicURI()).isEqualTo("replyTo3");
		assertThat(event.getEvent()).isEqualTo("returnUnsub3");

		webSocketSession.close();
	}

	@Test
	public void testUnsubscribe4() throws InterruptedException, ExecutionException,
			IOException {
		MultiResultWebSocketHandler result = new MultiResultWebSocketHandler(3,
				jsonFactory);
		final WebSocketSession webSocketSession = webSocketClient.doHandshake(result,
				getWsBaseUrl() + wampEndpointPath()).get();

		webSocketSession.sendMessage(new TextMessage(new SubscribeMessage("replyTo4_1")
				.toJson(jsonFactory)));
		webSocketSession.sendMessage(new TextMessage(new SubscribeMessage("replyTo4_2")
				.toJson(jsonFactory)));
		webSocketSession.sendMessage(new TextMessage(new SubscribeMessage("replyTo4_3")
				.toJson(jsonFactory)));

		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage("incomingUnsub4");
		webSocketSession.sendMessage(new TextMessage(unsubscribeMessage
				.toJson(jsonFactory)));

		List<WampMessage> resultList = result.getWampMessages();

		for (int i = 0; i < resultList.size(); i++) {
			EventMessage eventMessage = (EventMessage) resultList.get(i);
			assertThat(eventMessage.getTopicURI()).isEqualTo("replyTo4_" + (i + 1));
			assertThat(eventMessage.getEvent()).isEqualTo("returnUnsub4");
		}

		webSocketSession.close();
	}

	@Override
	protected Class<?>[] getAnnotatedConfigClasses() {
		return new Class<?>[] { Config.class };
	}

	@Configuration
	@EnableWamp
	static class Config extends WampConfigurerAdapter {
		@Bean
		public PubSubService pubSubService() {
			return new PubSubService();
		}

		@Bean
		public EventSenderService eventSenderService() {
			return new EventSenderService();
		}
	}

}
