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
package ch.rasc.wampspring.user;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.user.DefaultUserSessionRegistry;
import org.springframework.messaging.simp.user.UserSessionRegistry;

import ch.rasc.wampspring.EventMessenger;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.WampMessage;

public class UserEventMessengerTest {

	@Mock
	private SubscribableChannel brokerChannel;

	@Mock
	private SubscribableChannel clientOutboundChannel;

	private UserEventMessenger userEventMessenger;

	private UserSessionRegistry userSessionRegistry;

	@Captor
	ArgumentCaptor<EventMessage> messageCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(this.brokerChannel.send(Matchers.any(WampMessage.class)))
				.thenReturn(true);
		Mockito.when(this.clientOutboundChannel.send(Matchers.any(WampMessage.class)))
				.thenReturn(true);

		this.userSessionRegistry = new DefaultUserSessionRegistry();
		this.userSessionRegistry.registerSessionId("A", "ws1");
		this.userSessionRegistry.registerSessionId("B", "ws2");
		this.userSessionRegistry.registerSessionId("C", "ws3");
		this.userSessionRegistry.registerSessionId("D", "ws4");

		this.userEventMessenger = new UserEventMessenger(
				new EventMessenger(this.brokerChannel, this.clientOutboundChannel),
				this.userSessionRegistry);
	}

	@Test
	public void testSendWithTimeoutOk() {
		Mockito.when(this.brokerChannel.send(Matchers.any(WampMessage.class),
				Matchers.anyLong())).thenReturn(true);
		this.userEventMessenger.setSendTimeout(11);
		this.userEventMessenger.send(new EventMessage("topic", "1"));

		ArgumentCaptor<Long> timeOutArgument = ArgumentCaptor.forClass(Long.class);
		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture(), timeOutArgument.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);
		assertThat(timeOutArgument.getValue()).isEqualTo(11);
		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("topic");
		assertThat(msg.getEvent()).isEqualTo("1");
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
	}

	@Test(expected = MessageDeliveryException.class)
	public void testSendWithTimeoutException() {
		Mockito.when(this.brokerChannel.send(Matchers.any(WampMessage.class),
				Matchers.anyLong())).thenReturn(false);

		this.userEventMessenger.setSendTimeout(10);
		this.userEventMessenger.send(new EventMessage("topic", "2"));
	}

	@Test
	public void testSendDirectWithTimeoutOk() {
		Mockito.when(this.clientOutboundChannel.send(Matchers.any(WampMessage.class),
				Matchers.anyLong())).thenReturn(true);
		this.userEventMessenger.setSendTimeout(11);
		EventMessage eventMessage = new EventMessage("topic", "1");
		eventMessage.setWebSocketSessionId("ws1");
		this.userEventMessenger.sendDirect(eventMessage);

		ArgumentCaptor<Long> timeOutArgument = ArgumentCaptor.forClass(Long.class);
		Mockito.verify(this.clientOutboundChannel, Mockito.times(1))
				.send(this.messageCaptor.capture(), timeOutArgument.capture());
		Mockito.verifyZeroInteractions(this.brokerChannel);
		assertThat(timeOutArgument.getValue()).isEqualTo(11);
		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("topic");
		assertThat(msg.getEvent()).isEqualTo("1");
		assertThat(msg.getWebSocketSessionId()).isEqualTo("ws1");
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
	}

	@Test(expected = MessageDeliveryException.class)
	public void testSendDirectWithTimeoutException() {
		Mockito.when(this.clientOutboundChannel.send(Matchers.any(WampMessage.class),
				Matchers.anyLong())).thenReturn(false);

		this.userEventMessenger.setSendTimeout(10);
		EventMessage eventMessage = new EventMessage("topic", "2");
		eventMessage.setWebSocketSessionId("ws1");
		this.userEventMessenger.send(eventMessage);
	}

	@Test
	public void testSend() {
		this.userEventMessenger.send(new EventMessage("send", 2));
		Mockito.verify(this.brokerChannel, Mockito.never())
				.send(Matchers.any(EventMessage.class), Matchers.any(Long.class));

		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("send");
		assertThat(msg.getEvent()).isEqualTo(2);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
	}

	@Test
	public void testSendToAll() {
		this.userEventMessenger.sendToAll("all", 3);
		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(3);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
	}

	@Test
	public void testSendToAllExcept() {
		this.userEventMessenger.sendToAllExcept("all", 3, "ws1");
		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(3);
		assertThat(msg.getExcludeWebSocketSessionIds()).containsOnly("ws1");
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
	}

	@Test
	public void testSendToAllExceptSet() {
		this.userEventMessenger.sendToAllExcept("all", 4,
				new HashSet<>(Arrays.asList("ws1", "ws2", "ws3")));
		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(4);
		assertThat(msg.getExcludeWebSocketSessionIds()).containsOnly("ws1", "ws2", "ws3");
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
	}

	@Test
	public void testSendTo() {
		this.userEventMessenger.sendTo("all", 3, "ws1");
		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(3);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).containsOnly("ws1");
	}

	@Test
	public void testSendToSet() {
		this.userEventMessenger.sendTo("all", 4,
				new HashSet<>(Arrays.asList("ws1", "ws2", "ws3")));
		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(4);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).containsOnly("ws1", "ws2",
				"ws3");
	}

	@Test
	public void testSendToDirect() {
		this.userEventMessenger.sendToDirect("all", 5, "ws1");
		Mockito.verify(this.clientOutboundChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.brokerChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(5);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
		assertThat(msg.getWebSocketSessionId()).isEqualTo("ws1");
	}

	@Test
	public void testSendToDirectSet() {
		this.userEventMessenger.sendToDirect("all", 6,
				new LinkedHashSet<>(Arrays.asList("ws1", "ws2", "ws3")));

		Mockito.verify(this.clientOutboundChannel, Mockito.times(3))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.brokerChannel);

		List<EventMessage> msgs = this.messageCaptor.getAllValues();

		EventMessage msg = msgs.get(0);
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(6);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
		assertThat(msg.getWebSocketSessionId()).isEqualTo("ws1");

		msg = msgs.get(1);
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(6);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
		assertThat(msg.getWebSocketSessionId()).isEqualTo("ws2");

		msg = msgs.get(2);
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(6);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
		assertThat(msg.getWebSocketSessionId()).isEqualTo("ws3");
	}

	@Test
	public void testSendToAllExceptUser() {
		this.userEventMessenger.sendToAllExceptUser("all", 4, "A");

		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(4);
		assertThat(msg.getExcludeWebSocketSessionIds()).containsOnly("ws1");
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
	}

	@Test
	public void testSendToAllExceptUsers() {
		Set<String> users = new HashSet<>();
		users.add("B");
		users.add("C");
		this.userEventMessenger.sendToAllExceptUsers("all", 5, users);

		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("all");
		assertThat(msg.getEvent()).isEqualTo(5);
		assertThat(msg.getExcludeWebSocketSessionIds()).containsOnly("ws2", "ws3");
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
	}

	@Test
	public void testSendToUser() {
		this.userEventMessenger.sendToUser("one", 6, "D");

		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("one");
		assertThat(msg.getEvent()).isEqualTo(6);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).containsOnly("ws4");
	}

	@Test
	public void testSendToUsers() {
		Set<String> users = new HashSet<>();
		users.add("B");
		users.add("C");
		this.userEventMessenger.sendToUsers("two", 7, users);

		Mockito.verify(this.brokerChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.clientOutboundChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("two");
		assertThat(msg.getEvent()).isEqualTo(7);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).containsOnly("ws2", "ws3");
	}

	@Test
	public void testSendToUserDirect() {
		this.userEventMessenger.sendToUserDirect("one", 6, "D");

		Mockito.verify(this.clientOutboundChannel, Mockito.times(1))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.brokerChannel);

		EventMessage msg = this.messageCaptor.getValue();
		assertThat(msg.getDestination()).isEqualTo("one");
		assertThat(msg.getEvent()).isEqualTo(6);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
		assertThat(msg.getWebSocketSessionId()).isEqualTo("ws4");
	}

	@Test
	public void testSendToUsersDirect() {
		Set<String> users = new HashSet<>();
		users.add("B");
		users.add("C");
		this.userEventMessenger.sendToUsersDirect("two", 7, users);

		Mockito.verify(this.clientOutboundChannel, Mockito.times(2))
				.send(this.messageCaptor.capture());
		Mockito.verifyZeroInteractions(this.brokerChannel);

		List<EventMessage> msgs = this.messageCaptor.getAllValues();

		Set<String> wsIds = new HashSet<>();
		EventMessage msg = msgs.get(0);
		assertThat(msg.getDestination()).isEqualTo("two");
		assertThat(msg.getEvent()).isEqualTo(7);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
		assertThat(msg.getWebSocketSessionId()).isIn("ws2", "ws3");
		wsIds.add(msg.getWebSocketSessionId());

		msg = msgs.get(1);
		assertThat(msg.getDestination()).isEqualTo("two");
		assertThat(msg.getEvent()).isEqualTo(7);
		assertThat(msg.getExcludeWebSocketSessionIds()).isNull();
		assertThat(msg.getEligibleWebSocketSessionIds()).isNull();
		assertThat(msg.getWebSocketSessionId()).isIn("ws2", "ws3");
		wsIds.add(msg.getWebSocketSessionId());

		assertThat(wsIds).containsOnly("ws2", "ws3");
	}
}
