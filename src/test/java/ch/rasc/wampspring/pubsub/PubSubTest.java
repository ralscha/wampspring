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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.call.TestDto;
import ch.rasc.wampspring.config.EnableWamp;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.support.BaseWampTest;
import ch.rasc.wampspring.support.ResultWebSocketHandler;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = PubSubTest.Config.class)
@WebIntegrationTest({ "server.port=0" })
public class PubSubTest extends BaseWampTest {

	@Test
	public void testSimplePublishEvent() throws InterruptedException, ExecutionException,
			IOException {

		ResultWebSocketHandler result = new ResultWebSocketHandler(this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("topicURI");
			webSocketSession.sendMessage(new TextMessage(subscribeMsg
					.toJson(this.jsonFactory)));

			PublishMessage pm = new PublishMessage("topicURI", "a message");
			webSocketSession.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("topicURI");
			assertThat(event.getEvent()).isEqualTo("a message");

			result.reset();
			UnsubscribeMessage unsubscribeMsg = new UnsubscribeMessage("topicURI");
			webSocketSession.sendMessage(new TextMessage(unsubscribeMsg
					.toJson(this.jsonFactory)));

			pm = new PublishMessage("topicURI", "a second message");
			webSocketSession.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			event = (EventMessage) result.getWampMessage();
			assertThat(event).isNull();

		}
	}

	@Test
	public void testDtoPublishEvent() throws InterruptedException, ExecutionException,
			IOException {
		ResultWebSocketHandler result = new ResultWebSocketHandler(this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage(
					"pubSubService.dto.result");
			webSocketSession.sendMessage(new TextMessage(subscribeMsg
					.toJson(this.jsonFactory)));

			TestDto testDto = new TestDto();
			testDto.setName("Hello PubSub");
			PublishMessage pm = new PublishMessage("pubSubService.dto", testDto);
			webSocketSession.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("pubSubService.dto.result");
			assertThat(event.getEvent()).isEqualTo("Server says: Hello PubSub");

		}
	}

	@Test
	public void testEventMessenger() throws InterruptedException, ExecutionException,
			IOException {

		ResultWebSocketHandler result = new ResultWebSocketHandler(this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("secondTopic");
			webSocketSession.sendMessage(new TextMessage(subscribeMsg
					.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("secondTopic");
			assertThat(event.getEvent()).isEqualTo("a simple message");

		}
	}

	@Test
	public void testPublishToMethod() throws InterruptedException, ExecutionException,
			IOException {

		ResultWebSocketHandler result = new ResultWebSocketHandler(this.jsonFactory);
		try (WebSocketSession webSocketSession = startWebSocketSession(result)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("resultTopic");
			webSocketSession.sendMessage(new TextMessage(subscribeMsg
					.toJson(this.jsonFactory)));

			PublishMessage pm = new PublishMessage("sumTopic", Arrays.asList(1, 2, 3, 4));
			webSocketSession.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event = (EventMessage) result.getWampMessage();
			assertThat(event.getTopicURI()).isEqualTo("resultTopic");
			assertThat(event.getEvent()).isEqualTo(10);
		}
	}

	private void testExcludeEligible(Boolean excludeMe, List<Integer> exclude,
			List<Integer> eligible, List<Integer> expectedReceiver)
			throws InterruptedException, ExecutionException, IOException {
		ResultWebSocketHandler result1 = new ResultWebSocketHandler(this.jsonFactory);
		ResultWebSocketHandler result2 = new ResultWebSocketHandler(this.jsonFactory);

		try (WebSocketSession webSocketSession1 = startWebSocketSession(result1);
				WebSocketSession webSocketSession2 = startWebSocketSession(result2)) {

			SubscribeMessage subscribeMsg = new SubscribeMessage("anotherTopic");
			String json = subscribeMsg.toJson(this.jsonFactory);
			webSocketSession1.sendMessage(new TextMessage(json));
			webSocketSession2.sendMessage(new TextMessage(json));

			PublishMessage pm;
			if (excludeMe != null) {
				pm = new PublishMessage("anotherTopic", "the test message", excludeMe);
			}
			else if (exclude != null) {
				Set<String> excludeSet = new HashSet<>();
				if (exclude.contains(1)) {
					excludeSet.add(result1.getSessionId());
				}
				if (exclude.contains(2)) {
					excludeSet.add(result2.getSessionId());
				}

				if (eligible != null) {
					Set<String> eligibleSet = new HashSet<>();
					if (eligible.contains(1)) {
						eligibleSet.add(result1.getSessionId());
					}
					if (eligible.contains(2)) {
						eligibleSet.add(result2.getSessionId());
					}

					pm = new PublishMessage("anotherTopic", "the test message",
							excludeSet, eligibleSet);
				}
				else {
					pm = new PublishMessage("anotherTopic", "the test message",
							excludeSet);
				}
			}
			else {
				pm = new PublishMessage("anotherTopic", "the test message");
			}
			webSocketSession1.sendMessage(new TextMessage(pm.toJson(this.jsonFactory)));

			EventMessage event1 = (EventMessage) result1.getWampMessage();
			if (expectedReceiver.contains(1)) {
				assertThat(event1.getTopicURI()).isEqualTo("anotherTopic");
				assertThat(event1.getEvent()).isEqualTo("the test message");
			}
			else {
				assertThat(event1).isNull();
			}

			EventMessage event2 = (EventMessage) result2.getWampMessage();
			if (expectedReceiver.contains(2)) {
				assertThat(event2.getTopicURI()).isEqualTo("anotherTopic");
				assertThat(event2.getEvent()).isEqualTo("the test message");
			}
			else {
				assertThat(event2).isNull();
			}

		}
	}

	@Test
	public void testExcludeEligible() throws InterruptedException, ExecutionException,
			IOException {
		// excludeMe, exclude, eligible, expectedReceivers
		testExcludeEligible(null, null, null, Arrays.asList(1, 2));
		testExcludeEligible(Boolean.TRUE, null, null, Arrays.asList(2));
		testExcludeEligible(Boolean.FALSE, null, null, Arrays.asList(1, 2));

		testExcludeEligible(null, Collections.<Integer> emptyList(), null,
				Arrays.asList(1, 2));
		testExcludeEligible(null, Arrays.asList(1), null, Arrays.asList(2));
		testExcludeEligible(null, Arrays.asList(2), null, Arrays.asList(1));
		testExcludeEligible(null, Arrays.asList(1, 2), null,
				Collections.<Integer> emptyList());

		testExcludeEligible(null, Collections.<Integer> emptyList(),
				Collections.<Integer> emptyList(), Collections.<Integer> emptyList());
		testExcludeEligible(null, Collections.<Integer> emptyList(), Arrays.asList(1),
				Arrays.asList(1));
		testExcludeEligible(null, Collections.<Integer> emptyList(), Arrays.asList(2),
				Arrays.asList(2));
		testExcludeEligible(null, Collections.<Integer> emptyList(), Arrays.asList(1, 2),
				Arrays.asList(1, 2));

		testExcludeEligible(null, Arrays.asList(1), Collections.<Integer> emptyList(),
				Collections.<Integer> emptyList());
		testExcludeEligible(null, Arrays.asList(1), Arrays.asList(1),
				Collections.<Integer> emptyList());
		testExcludeEligible(null, Arrays.asList(1), Arrays.asList(2), Arrays.asList(2));
		testExcludeEligible(null, Arrays.asList(1), Arrays.asList(1, 2), Arrays.asList(2));

		testExcludeEligible(null, Arrays.asList(2), Collections.<Integer> emptyList(),
				Collections.<Integer> emptyList());
		testExcludeEligible(null, Arrays.asList(2), Arrays.asList(1), Arrays.asList(1));
		testExcludeEligible(null, Arrays.asList(2), Arrays.asList(2),
				Collections.<Integer> emptyList());
		testExcludeEligible(null, Arrays.asList(2), Arrays.asList(1, 2), Arrays.asList(1));

		testExcludeEligible(null, Arrays.asList(1, 2), Collections.<Integer> emptyList(),
				Collections.<Integer> emptyList());
		testExcludeEligible(null, Arrays.asList(1, 2), Arrays.asList(1),
				Collections.<Integer> emptyList());
		testExcludeEligible(null, Arrays.asList(1, 2), Arrays.asList(2),
				Collections.<Integer> emptyList());
		testExcludeEligible(null, Arrays.asList(1, 2), Arrays.asList(1, 2),
				Collections.<Integer> emptyList());
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableWamp
	static class Config {

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
