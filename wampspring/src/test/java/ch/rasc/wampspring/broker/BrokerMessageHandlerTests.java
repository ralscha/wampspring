/**
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class BrokerMessageHandlerTests {

	private TestBrokerMesageHandler handler;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.handler = new TestBrokerMesageHandler();
	}

	@Test
	public void startShouldUpdateIsRunning() {
		assertFalse(this.handler.isRunning());
		this.handler.start();
		assertTrue(this.handler.isRunning());
	}

	@Test
	public void stopShouldUpdateIsRunning() {

		this.handler.start();
		assertTrue(this.handler.isRunning());

		this.handler.stop();
		assertFalse(this.handler.isRunning());
	}

	@Test
	public void startAndStopShouldNotPublishBrokerAvailabilityEvents() {
		this.handler.start();
		this.handler.stop();
		assertEquals(Collections.emptyList(), this.handler.availabilityEvents);
	}

	@Test
	public void handleMessageWhenBrokerNotRunning() {
		this.handler.handleMessage(new GenericMessage<Object>("payload"));
		assertEquals(Collections.emptyList(), this.handler.messages);
	}

	private static class TestBrokerMesageHandler extends AbstractBrokerMessageHandler {

		private final List<Message<?>> messages = new ArrayList<>();

		private final List<Boolean> availabilityEvents = new ArrayList<>();

		private TestBrokerMesageHandler() {
			super(mock(SubscribableChannel.class), mock(MessageChannel.class),
					mock(SubscribableChannel.class));
		}

		@Override
		protected void handleMessageInternal(Message<?> message) {
			this.messages.add(message);
		}

	}

}
