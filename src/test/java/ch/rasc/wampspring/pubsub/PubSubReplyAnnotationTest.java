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
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.config.DefaultWampConfiguration;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.testsupport.BaseWampTest;
import ch.rasc.wampspring.testsupport.CompletableFutureWebSocketHandler;

@SpringApplicationConfiguration(classes = PubSubReplyAnnotationTest.Config.class)
public class PubSubReplyAnnotationTest extends BaseWampTest {

	@Test
	public void testPublish1() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo1");
			webSocketSession
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			PublishMessage pm = new PublishMessage("pubSubService.incomingPublish1",
					"testPublish1");
			webSocketSession.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("replyTo1");
			assertThat(event.getEvent()).isEqualTo("return1:testPublish1");

		}
	}

	@Test
	public void testPublish2() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {

		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo2");
			webSocketSession1
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));
			webSocketSession2
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			PublishMessage pm = new PublishMessage("pubSubService.incomingPublish2",
					"testPublish2");
			webSocketSession1.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result2.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("replyTo2");
			assertThat(event.getEvent()).isEqualTo("return2:testPublish2");

			event = null;
			try {
				event = (EventMessage) result1.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();
		}

	}

	@Test
	public void testPublish3() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo3");
			webSocketSession
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			PublishMessage pm = new PublishMessage("incomingPublish3", "testPublish3");
			webSocketSession.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("replyTo3");
			assertThat(event.getEvent()).isEqualTo("return3:testPublish3");

		}
	}

	@Test
	public void testPublish4() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				3, this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			webSocketSession.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo4_1").toJson(this.jsonFactory)));
			webSocketSession.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo4_2").toJson(this.jsonFactory)));
			webSocketSession.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo4_3").toJson(this.jsonFactory)));

			PublishMessage pm = new PublishMessage("incomingPublish4", "testPublish4");
			webSocketSession.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			List<WampMessage> resultList = result.getWampMessages();

			for (int i = 0; i < resultList.size(); i++) {
				EventMessage eventMessage = (EventMessage) resultList.get(i);
				assertThat(eventMessage.getTopicURI()).isEqualTo("replyTo4_" + (i + 1));
				assertThat(eventMessage.getEvent()).isEqualTo("return4:testPublish4");
			}
		}
	}

	@Test
	public void testPublish5() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			webSocketSession1.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo5").toJson(this.jsonFactory)));
			webSocketSession2.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo5").toJson(this.jsonFactory)));

			PublishMessage pm = new PublishMessage("incomingPublish5", "testPublish5");
			webSocketSession1.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event1 = (EventMessage) result1.getWampMessage();
			assertThat(event1.getTopicURI()).isEqualTo("replyTo5");
			assertThat(event1.getEvent()).isEqualTo("return5:testPublish5");

			EventMessage event = null;
			try {
				event = (EventMessage) result2.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();
		}
	}

	@Test
	public void testPublish6()
			throws InterruptedException, ExecutionException, IOException {
		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			webSocketSession1.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo6").toJson(this.jsonFactory)));
			webSocketSession2.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo6").toJson(this.jsonFactory)));

			PublishMessage pm = new PublishMessage("incomingPublish6", "testPublish6");
			webSocketSession1.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event = null;
			try {
				event = (EventMessage) result1.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();

			try {
				event = (EventMessage) result2.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();
		}
	}

	@Test
	public void testSubscribe1() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo1");
			webSocketSession
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			subscribeMsg = new SubscribeMessage("pubSubService.incomingSubscribe1");
			webSocketSession
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("replyTo1");
			assertThat(event.getEvent()).isEqualTo("returnSub1");

		}
	}

	@Test
	public void testSubscribe2() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo2");
			webSocketSession1
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));
			webSocketSession2
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			subscribeMsg = new SubscribeMessage("pubSubService.incomingSubscribe2");
			webSocketSession1
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result2.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("replyTo2");
			assertThat(event.getEvent()).isEqualTo("returnSub2");

			event = null;
			try {
				event = (EventMessage) result1.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();
		}
	}

	@Test
	public void testSubscribe3() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo3");
			webSocketSession
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			subscribeMsg = new SubscribeMessage("incomingSub3");
			webSocketSession
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("replyTo3");
			assertThat(event.getEvent()).isEqualTo("returnSub3");

		}
	}

	@Test
	public void testSubscribe4() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				3, this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			webSocketSession.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo4_1").toJson(this.jsonFactory)));
			webSocketSession.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo4_2").toJson(this.jsonFactory)));
			webSocketSession.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo4_3").toJson(this.jsonFactory)));

			SubscribeMessage subscribeMsg = new SubscribeMessage("incomingSub4");
			webSocketSession
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			List<WampMessage> resultList = result.getWampMessages();

			for (int i = 0; i < resultList.size(); i++) {
				EventMessage eventMessage = (EventMessage) resultList.get(i);
				assertThat(eventMessage.getTopicURI()).isEqualTo("replyTo4_" + (i + 1));
				assertThat(eventMessage.getEvent()).isEqualTo("returnSub4");
			}
		}
	}

	@Test
	public void testSubscribe5() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			webSocketSession1.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo5").toJson(this.jsonFactory)));
			webSocketSession2.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo5").toJson(this.jsonFactory)));

			SubscribeMessage subscribeMsg = new SubscribeMessage("incomingSub5");
			webSocketSession1
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			EventMessage event1 = (EventMessage) result1.getWampMessage();
			assertThat(event1.getTopicURI()).isEqualTo("replyTo5");
			assertThat(event1.getEvent()).isEqualTo("returnSub5");

			EventMessage event = null;
			try {
				event = (EventMessage) result2.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();
		}
	}

	@Test
	public void testSubscribe6()
			throws InterruptedException, ExecutionException, IOException {
		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			webSocketSession1.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo6").toJson(this.jsonFactory)));
			webSocketSession2.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo6").toJson(this.jsonFactory)));

			SubscribeMessage subscribeMsg = new SubscribeMessage("incomingSub6");
			webSocketSession1
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			EventMessage event = null;
			try {
				event = (EventMessage) result1.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();

			try {
				event = (EventMessage) result2.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();
		}
	}

	@Test
	public void testUnsubscribe1() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo1");
			webSocketSession
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
					"pubSubService.incomingUnsubscribe1");
			webSocketSession.sendMessage(
					new TextMessage(unsubscribeMessage.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("replyTo1");
			assertThat(event.getEvent()).isEqualTo("returnUnsub1");

		}
	}

	@Test
	public void testUnsubscribe2() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {

		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo2");
			webSocketSession1
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));
			webSocketSession2
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
					"pubSubService.incomingUnsubscribe2");
			webSocketSession1.sendMessage(
					new TextMessage(unsubscribeMessage.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result2.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("replyTo2");
			assertThat(event.getEvent()).isEqualTo("returnUnsub2");

			event = null;
			try {
				event = (EventMessage) result1.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();
		}

	}

	@Test
	public void testUnsubscribe3() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("replyTo3");
			webSocketSession
					.sendMessage(new TextMessage(subscribeMsg.toJson(this.jsonFactory)));

			UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
					"incomingUnsub3");
			webSocketSession.sendMessage(
					new TextMessage(unsubscribeMessage.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("replyTo3");
			assertThat(event.getEvent()).isEqualTo("returnUnsub3");

		}
	}

	@Test
	public void testUnsubscribe4() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result = new CompletableFutureWebSocketHandler(
				3, this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			webSocketSession.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo4_1").toJson(this.jsonFactory)));
			webSocketSession.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo4_2").toJson(this.jsonFactory)));
			webSocketSession.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo4_3").toJson(this.jsonFactory)));

			UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
					"incomingUnsub4");
			webSocketSession.sendMessage(
					new TextMessage(unsubscribeMessage.toJson(this.jsonFactory)));

			List<WampMessage> resultList = result.getWampMessages();

			for (int i = 0; i < resultList.size(); i++) {
				EventMessage eventMessage = (EventMessage) resultList.get(i);
				assertThat(eventMessage.getTopicURI()).isEqualTo("replyTo4_" + (i + 1));
				assertThat(eventMessage.getEvent()).isEqualTo("returnUnsub4");
			}

		}
	}

	@Test
	public void testUnsubscribe5() throws InterruptedException, ExecutionException,
			IOException, TimeoutException {
		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			webSocketSession1.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo5").toJson(this.jsonFactory)));
			webSocketSession2.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo5").toJson(this.jsonFactory)));

			UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
					"incomingUnsub5");
			webSocketSession1.sendMessage(
					new TextMessage(unsubscribeMessage.toJson(this.jsonFactory)));

			EventMessage event1 = (EventMessage) result1.getWampMessage();
			assertThat(event1.getTopicURI()).isEqualTo("replyTo5");
			assertThat(event1.getEvent()).isEqualTo("returnUnsub5");

			EventMessage event = null;
			try {
				event = (EventMessage) result2.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();
		}
	}

	@Test
	public void testUnsubscribe6()
			throws InterruptedException, ExecutionException, IOException {
		CompletableFutureWebSocketHandler result1 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);
		CompletableFutureWebSocketHandler result2 = new CompletableFutureWebSocketHandler(
				this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			webSocketSession1.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo6").toJson(this.jsonFactory)));
			webSocketSession2.sendMessage(new TextMessage(
					new SubscribeMessage("replyTo6").toJson(this.jsonFactory)));

			UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
					"incomingUnsub6");
			webSocketSession1.sendMessage(
					new TextMessage(unsubscribeMessage.toJson(this.jsonFactory)));

			EventMessage event = null;
			try {
				event = (EventMessage) result1.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();

			try {
				event = (EventMessage) result2.getWampMessage();
				Assert.fail("call has to timeout");
			}
			catch (Exception e) {
				assertThat(e).isInstanceOf(TimeoutException.class);
			}
			assertThat(event).isNull();
		}
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config extends DefaultWampConfiguration {
		@Bean
		public PubSubService pubSubService() {
			return new PubSubService();
		}

		@Override
		public Executor clientInboundChannelExecutor() {
			return new SyncTaskExecutor();
		}

		@Override
		public Executor clientOutboundChannelExecutor() {
			return new SyncTaskExecutor();
		}

	}

}
