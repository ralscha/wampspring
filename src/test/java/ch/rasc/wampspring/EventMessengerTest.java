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
package ch.rasc.wampspring;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

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

import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.WampMessage;

public class EventMessengerTest {

	@Mock
	private SubscribableChannel brokerChannel;

	@Mock
	private SubscribableChannel clientOutboundChannel;

	private EventMessenger eventMessenger;

	@Captor
	ArgumentCaptor<EventMessage> messageCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(this.brokerChannel.send(Matchers.any(WampMessage.class)))
				.thenReturn(true);
		Mockito.when(this.clientOutboundChannel.send(Matchers.any(WampMessage.class)))
				.thenReturn(true);
		this.eventMessenger = new EventMessenger(this.brokerChannel,
				this.clientOutboundChannel);
	}

	@Test
	public void testSendWithTimeoutOk() {
		Mockito.when(this.brokerChannel.send(Matchers.any(WampMessage.class),
				Matchers.anyLong())).thenReturn(true);
		this.eventMessenger.setSendTimeout(11);
		this.eventMessenger.send(new EventMessage("topic", "1"));

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

		this.eventMessenger.setSendTimeout(10);
		this.eventMessenger.send(new EventMessage("topic", "2"));
	}

	@Test
	public void testSendDirectWithTimeoutOk() {
		Mockito.when(this.clientOutboundChannel.send(Matchers.any(WampMessage.class),
				Matchers.anyLong())).thenReturn(true);
		this.eventMessenger.setSendTimeout(11);
		EventMessage eventMessage = new EventMessage("topic", "1");
		eventMessage.setWebSocketSessionId("ws1");
		this.eventMessenger.sendDirect(eventMessage);

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

		this.eventMessenger.setSendTimeout(10);
		EventMessage eventMessage = new EventMessage("topic", "2");
		eventMessage.setWebSocketSessionId("ws1");
		this.eventMessenger.send(eventMessage);
	}

	@Test
	public void testSend() {
		this.eventMessenger.send(new EventMessage("send", 2));
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
		this.eventMessenger.sendToAll("all", 3);
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
		this.eventMessenger.sendToAllExcept("all", 3, "ws1");
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
		this.eventMessenger.sendToAllExcept("all", 4,
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
		this.eventMessenger.sendTo("all", 3, "ws1");
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
		this.eventMessenger.sendTo("all", 4,
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
		this.eventMessenger.sendToDirect("all", 5, "ws1");
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
		this.eventMessenger.sendToDirect("all", 6,
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

}
