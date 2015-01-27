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
package ch.rasc.wampspring.broker;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.websocket.Session;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;

/**
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class SimpleBrokerMessageHandlerTests {

	private SimpleBrokerMessageHandler messageHandler;

	@Mock
	private SubscribableChannel clientInboundChannel;

	@Mock
	private MessageChannel clientOutboundChannel;

	@Mock
	private SubscribableChannel brokerChannel;

	@Captor
	ArgumentCaptor<EventMessage> messageCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.messageHandler = new SimpleBrokerMessageHandler(this.clientInboundChannel,
				this.clientOutboundChannel, this.brokerChannel,
				new DefaultSubscriptionRegistry(new AntPathMatcher()));
		this.messageHandler.start();
	}

	@Test
	public void testStartStop() {
		assertTrue(this.messageHandler.isRunning());
		this.messageHandler.stop();
		assertFalse(this.messageHandler.isRunning());

		this.messageHandler.start();
		assertTrue(this.messageHandler.isRunning());
	}

	@Test
	public void testSubscribe() {

		this.messageHandler.handleMessage(subscribeMessage("sess1", "/foo"));
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/foo"));

		this.messageHandler.handleMessage(subscribeMessage("sess2", "/foo"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/foo"));

		this.messageHandler.handleMessage(eventMessage("sess1", "/foo", "message1"));
		this.messageHandler.handleMessage(eventMessage("sess2", "/bar", "message2"));

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());

		assertCapturedMessage(eventMessage("sess1", "/foo", "message1"),
				eventMessage("sess2", "/foo", "message1"));
	}

	@Test
	public void testUnsubscribe() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/foo"));
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/bar"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/foo"));

		this.messageHandler.handleMessage(unsubscribeMessage("sess1", "/foo"));

		this.messageHandler.handleMessage(eventMessage("sess1", "/foo", "message1"));
		this.messageHandler.handleMessage(eventMessage("sess2", "/bar", "message2"));

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess1", "/bar", "message2"),
				eventMessage("sess2", "/foo", "message1"));
	}

	@Test
	public void testUnsubscribeAll() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/foo"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/bar"));

		this.messageHandler.handleMessage(unsubscribeMessage("sess1", "/foo"));
		this.messageHandler.handleMessage(unsubscribeMessage("sess2", "/bar"));

		this.messageHandler.handleMessage(eventMessage("sess1", "/foo", "message1"));
		this.messageHandler.handleMessage(eventMessage("sess1", "/bar", "message2"));

		verify(this.clientOutboundChannel, Mockito.never()).send(
				this.messageCaptor.capture());
	}

	@Test
	public void testNoneSubscribed() {
		this.messageHandler.handleMessage(eventMessage("sess1", "/foo", "message1"));
		this.messageHandler.handleMessage(eventMessage("sess1", "/bar", "message2"));
		verify(this.clientOutboundChannel, Mockito.never()).send(
				this.messageCaptor.capture());
	}

	@Test
	public void testPublishMessage() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		this.messageHandler.handleMessage(publishMessage("sess1", "/topic",
				"publishMessage1"));

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess1", "/topic", "publishMessage1"),
				eventMessage("sess2", "/topic", "publishMessage1"));
	}

	@Test
	public void testPublishMessageExcludeMe() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage1",
				true);
		publishMessage.setSessionId("sess1");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess2", "/topic", "publishMessage1"));
	}

	@Test
	public void testPublishMessageExclude() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> exclude = new HashSet<>();
		exclude.add("sess2");
		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage1",
				exclude);
		publishMessage.setSessionId("sess1");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess1", "/topic", "publishMessage1"));
	}

	@Test
	public void testPublishMessageExcludeAll() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> exclude = new HashSet<>();
		exclude.add("sess1");
		exclude.add("sess2");
		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage1",
				exclude);
		publishMessage.setSessionId("sess1");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, Mockito.never()).send(
				this.messageCaptor.capture());
	}

	@Test
	public void testPublishMessageExcludeNone() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> exclude = new HashSet<>();
		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage1",
				exclude);
		publishMessage.setSessionId("sess1");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess1", "/topic", "publishMessage1"),
				eventMessage("sess2", "/topic", "publishMessage1"));
	}

	@Test
	public void testPublishMessageEligible() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> eligible = new HashSet<>();
		eligible.add("sess1");
		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage1",
				null, eligible);
		publishMessage.setSessionId("sess2");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess1", "/topic", "publishMessage1"));
	}

	@Test
	public void testPublishMessageEligibleAll() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> eligible = new HashSet<>();
		eligible.add("sess1");
		eligible.add("sess2");
		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage1",
				null, eligible);
		publishMessage.setSessionId("sess2");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess1", "/topic", "publishMessage1"),
				eventMessage("sess2", "/topic", "publishMessage1"));
	}

	@Test
	public void testPublishMessageEligiblNone() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> eligible = new HashSet<>();
		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage1",
				null, eligible);
		publishMessage.setSessionId("sess2");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, Mockito.never()).send(
				this.messageCaptor.capture());
	}

	@Test
	public void testPublishMessageExcludeAndEligible1() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess3", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess4", "/topic"));

		Set<String> exclude = new HashSet<>();
		Set<String> eligible = new HashSet<>();
		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage",
				exclude, eligible);
		publishMessage.setSessionId("sess1");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, Mockito.never()).send(
				this.messageCaptor.capture());
	}

	@Test
	public void testPublishMessageExcludeAndEligible2() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess3", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess4", "/topic"));

		Set<String> exclude = new HashSet<>();
		exclude.add("sess1");
		Set<String> eligible = new HashSet<>();
		eligible.add("sess1");
		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage",
				exclude, eligible);
		publishMessage.setSessionId("sess2");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, Mockito.never()).send(
				this.messageCaptor.capture());
	}

	@Test
	public void testPublishMessageExcludeAndEligible3() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess3", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess4", "/topic"));

		Set<String> exclude = new HashSet<>();
		exclude.add("sess1");
		Set<String> eligible = new HashSet<>();
		eligible.add("sess2");
		eligible.add("sess3");
		PublishMessage publishMessage = new PublishMessage("/topic", "publishMessage",
				exclude, eligible);
		publishMessage.setSessionId("sess2");
		this.messageHandler.handleMessage(publishMessage);

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess2", "/topic", "publishMessage1"),
				eventMessage("sess3", "/topic", "publishMessage1"));
	}

	@Test
	public void testEventMessageExclude() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> exclude = new HashSet<>();
		exclude.add("sess2");
		EventMessage eventMessage = new EventMessage("/topic", "eventMessage");
		eventMessage.setSessionId("sess1");
		eventMessage.setExcludeSessionIds(exclude);
		this.messageHandler.handleMessage(eventMessage);

		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess1", "/topic", "eventMessage"));
	}

	@Test
	public void testEventMessageExcludeAll() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> exclude = new HashSet<>();
		exclude.add("sess1");
		exclude.add("sess2");
		EventMessage eventMessage = new EventMessage("/topic", "eventMessage");
		eventMessage.setSessionId("sess1");
		eventMessage.setExcludeSessionIds(exclude);
		this.messageHandler.handleMessage(eventMessage);

		verify(this.clientOutboundChannel, Mockito.never()).send(
				this.messageCaptor.capture());
	}

	@Test
	public void testEventMessageExcludeNone() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> exclude = new HashSet<>();
		EventMessage eventMessage = new EventMessage("/topic", "eventMessage");
		eventMessage.setSessionId("sess1");
		eventMessage.setExcludeSessionIds(exclude);
		this.messageHandler.handleMessage(eventMessage);

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess1", "/topic", "eventMessage"),
				eventMessage("sess2", "/topic", "eventMessage"));
	}

	@Test
	public void testEventMessageEligible() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> eligible = new HashSet<>();
		eligible.add("sess2");
		EventMessage eventMessage = new EventMessage("/topic", "eventMessage");
		eventMessage.setSessionId("sess1");
		eventMessage.setEligibleSessionIds(eligible);
		this.messageHandler.handleMessage(eventMessage);

		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess2", "/topic", "eventMessage"));
	}

	@Test
	public void testEventMessageEligibleAll() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> eligible = new HashSet<>();
		eligible.add("sess1");
		eligible.add("sess2");
		EventMessage eventMessage = new EventMessage("/topic", "eventMessage");
		eventMessage.setSessionId("sess1");
		eventMessage.setEligibleSessionIds(eligible);
		this.messageHandler.handleMessage(eventMessage);

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess2", "/topic", "eventMessage"),
				eventMessage("sess1", "/topic", "eventMessage"));
	}

	@Test
	public void testEventMessageEligibleNone() {
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/topic"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/topic"));

		Set<String> eligible = new HashSet<>();
		EventMessage eventMessage = new EventMessage("/topic", "eventMessage");
		eventMessage.setSessionId("sess1");
		eventMessage.setEligibleSessionIds(eligible);
		this.messageHandler.handleMessage(eventMessage);

		verify(this.clientOutboundChannel, Mockito.never()).send(
				this.messageCaptor.capture());
	}

	@SuppressWarnings("resource")
	@Test
	public void testCleanupMessage() {

		this.messageHandler.handleMessage(subscribeMessage("sess1", "/foo"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/foo"));

		Session nativeSession = Mockito.mock(Session.class);
		Mockito.when(nativeSession.getId()).thenReturn("sess1");
		StandardWebSocketSession wsSession = new StandardWebSocketSession(null, null,
				null, null);
		wsSession.initializeNativeSession(nativeSession);
		UnsubscribeMessage cleanupMessage = UnsubscribeMessage
				.createCleanupMessage(wsSession);
		this.messageHandler.handleMessage(cleanupMessage);

		this.messageHandler.handleMessage(eventMessage("sess1", "/foo", "message1"));
		this.messageHandler.handleMessage(eventMessage("sess2", "/bar", "message2"));

		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		assertCapturedMessage(eventMessage("sess2", "/foo", "message1"));
	}

	private static SubscribeMessage subscribeMessage(String sessionId, String topicURI) {
		SubscribeMessage message = new SubscribeMessage(topicURI);
		message.setSessionId(sessionId);
		return message;
	}

	private static UnsubscribeMessage unsubscribeMessage(String sessionId, String topicURI) {
		UnsubscribeMessage message = new UnsubscribeMessage(topicURI);
		message.setSessionId(sessionId);
		return message;
	}

	private static EventMessage eventMessage(String sessionId, String topicURI,
			Object payload) {
		EventMessage eventMessage = new EventMessage(topicURI, payload);
		eventMessage.setSessionId(sessionId);
		return eventMessage;
	}

	private static PublishMessage publishMessage(String sessionId, String topicURI,
			Object payload) {
		PublishMessage publishMessage = new PublishMessage(topicURI, payload);
		publishMessage.setSessionId(sessionId);
		return publishMessage;
	}

	private void assertCapturedMessage(EventMessage... expectedMessages) {
		List<EventMessage> allCapturedMessages = this.messageCaptor.getAllValues();

		if (!ObjectUtils.isEmpty(expectedMessages)) {
			assertThat(allCapturedMessages).hasSize(expectedMessages.length);

			for (EventMessage expectedMessage : expectedMessages) {

				EventMessage found = null;
				for (EventMessage capturedMessage : allCapturedMessages) {
					if (capturedMessage.getPayload().equals(expectedMessage.getPayload())
							&& capturedMessage.getDestination().equals(
									expectedMessage.getDestination())
							&& capturedMessage.getSessionId().equals(
									expectedMessage.getSessionId())) {
						found = capturedMessage;
					}
				}

				assertThat(found).isNotNull();
			}

		}
		else {
			assertThat(allCapturedMessages).isEmpty();
		}

	}
}
