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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;

import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PubSubMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;

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
	ArgumentCaptor<WampMessage> messageCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.messageHandler = new SimpleBrokerMessageHandler(this.clientInboundChannel,
				this.clientOutboundChannel, this.brokerChannel,
				new DefaultSubscriptionRegistry(new AntPathMatcher()));
		this.messageHandler.start();
	}

	@Test
	public void testSubscribe() {

		this.messageHandler.handleMessage(subscribeMessage("sess1", "/foo"));
		this.messageHandler.handleMessage(subscribeMessage("sess1", "/foo"));

		this.messageHandler.handleMessage(subscribeMessage("sess2", "/foo"));
		this.messageHandler.handleMessage(subscribeMessage("sess2", "/foo"));

		this.messageHandler.handleMessage(message("/foo", "message1"));
		this.messageHandler.handleMessage(message("/bar", "message2"));

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		assertCapturedMessage("sess1", "/foo");
		assertCapturedMessage("sess2", "/foo");
	}

	@Test
	public void testUnsubscribe() {
		String sess1 = "sess1";
		String sess2 = "sess2";

		this.messageHandler.handleMessage(subscribeMessage(sess1, "/foo"));
		this.messageHandler.handleMessage(subscribeMessage(sess1, "/bar"));
		this.messageHandler.handleMessage(subscribeMessage(sess2, "/foo"));

		this.messageHandler.handleMessage(unsubscribeMessage(sess1, "/foo"));

		this.messageHandler.handleMessage(message("/foo", "message1"));
		this.messageHandler.handleMessage(message("/bar", "message2"));

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		assertCapturedMessage(sess1, "/bar");
		assertCapturedMessage(sess2, "/foo");

	}

	@Test
	public void testCleanupMessage() {

		String sess1 = "sess1";
		String sess2 = "sess2";

		this.messageHandler.handleMessage(subscribeMessage(sess1, "/foo"));

		this.messageHandler.handleMessage(subscribeMessage(sess2, "/foo"));

		Session nativeSession = Mockito.mock(Session.class);
		Mockito.when(nativeSession.getId()).thenReturn(sess1);

		StandardWebSocketSession wsSession = new StandardWebSocketSession(null, null,
				null, null);
		wsSession.initializeNativeSession(nativeSession);
		UnsubscribeMessage cleanupMessage = UnsubscribeMessage
				.createCleanupMessage(wsSession);
		this.messageHandler.handleMessage(cleanupMessage);

		this.messageHandler.handleMessage(message("/foo", "message1"));
		this.messageHandler.handleMessage(message("/bar", "message2"));

		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		assertCapturedMessage(sess2, "/foo");
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

	private static PubSubMessage message(String destination, String payload) {
		EventMessage eventMessage = new EventMessage(destination, payload);
		return eventMessage;
	}

	protected void assertCapturedMessage(String sessionId, String destination) {
		for (WampMessage message : this.messageCaptor.getAllValues()) {
			if (sessionId.equals(message.getSessionId())) {
				if (destination.equals(message.getDestination())) {
					return;
				}
			}
		}

		fail("captured message does not match");
	}

}
