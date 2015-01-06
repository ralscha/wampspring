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
package ch.rasc.wampspring.handler;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessageHeader;

public class PubSubHandlerTest {

	@Test
	public void testSendToAll() {
		WampMessageSender messageSender = Mockito.mock(WampMessageSender.class);
		PubSubHandler pubSubHandler = new PubSubHandler(messageSender);

		SubscribeMessage subMessage = new SubscribeMessage("theTopic");
		subMessage.addHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, "1");
		pubSubHandler.handleMessage(subMessage);

		Set<String> sessions = new HashSet<>();
		sessions.add("1");
		EventMessage eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendToAll(eventMessage);

		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(sessions,
				eventMessage);

		subMessage = new SubscribeMessage("theTopic");
		subMessage.addHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, "2");
		pubSubHandler.handleMessage(subMessage);

		sessions.add("2");
		eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendToAll(eventMessage);

		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(sessions,
				eventMessage);

		UnsubscribeMessage unsubMessage = new UnsubscribeMessage("theTopic");
		unsubMessage.addHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, "1");
		pubSubHandler.handleMessage(unsubMessage);

		sessions.remove("1");
		eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendToAll(eventMessage);

		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(sessions,
				eventMessage);

		pubSubHandler.unregisterSessionFromAllSubscriptions("2");
		sessions.remove("2");
		eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendToAll(eventMessage);

		Mockito.verify(messageSender, Mockito.never()).sendMessageToClient(sessions,
				eventMessage);
	}

	@Test
	public void testSendToAllExcept() {
		WampMessageSender messageSender = Mockito.mock(WampMessageSender.class);
		PubSubHandler pubSubHandler = new PubSubHandler(messageSender);

		SubscribeMessage subMessage = new SubscribeMessage("theTopic");
		subMessage.addHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, "1");
		pubSubHandler.handleMessage(subMessage);

		subMessage = new SubscribeMessage("theTopic");
		subMessage.addHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, "2");
		pubSubHandler.handleMessage(subMessage);

		Set<String> excludeSessions = new HashSet<>();
		Set<String> expectedSessions = new HashSet<>();
		expectedSessions.add("1");
		expectedSessions.add("2");
		EventMessage eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendToAllExcept(eventMessage, excludeSessions);
		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(
				expectedSessions, eventMessage);

		excludeSessions = new HashSet<>();
		expectedSessions = new HashSet<>();
		excludeSessions.add("1");
		expectedSessions.add("2");
		eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendToAllExcept(eventMessage, excludeSessions);
		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(
				expectedSessions, eventMessage);

		excludeSessions = new HashSet<>();
		expectedSessions = new HashSet<>();
		excludeSessions.add("1");
		excludeSessions.add("2");
		eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendToAllExcept(eventMessage, excludeSessions);
		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(
				expectedSessions, eventMessage);

		excludeSessions = new HashSet<>();
		expectedSessions = new HashSet<>();
		excludeSessions.add("1");
		excludeSessions.add("2");
		excludeSessions.add("3");
		excludeSessions.add("4");
		eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendToAllExcept(eventMessage, excludeSessions);
		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(
				expectedSessions, eventMessage);
	}

	@Test
	public void testSendTo() {
		WampMessageSender messageSender = Mockito.mock(WampMessageSender.class);
		PubSubHandler pubSubHandler = new PubSubHandler(messageSender);

		SubscribeMessage subMessage = new SubscribeMessage("theTopic");
		subMessage.addHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, "1");
		pubSubHandler.handleMessage(subMessage);

		subMessage = new SubscribeMessage("theTopic");
		subMessage.addHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, "2");
		pubSubHandler.handleMessage(subMessage);

		Set<String> eligibleSessions = new HashSet<>();
		Set<String> expectedSessions = new HashSet<>();
		EventMessage eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendTo(eventMessage, eligibleSessions);
		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(
				expectedSessions, eventMessage);

		eligibleSessions = new HashSet<>();
		eligibleSessions.add("1");
		expectedSessions = new HashSet<>();
		expectedSessions.add("1");
		eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendTo(eventMessage, eligibleSessions);
		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(
				expectedSessions, eventMessage);

		eligibleSessions = new HashSet<>();
		eligibleSessions.add("1");
		eligibleSessions.add("2");
		expectedSessions = new HashSet<>();
		expectedSessions.add("1");
		expectedSessions.add("2");
		eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendTo(eventMessage, eligibleSessions);
		Mockito.verify(messageSender, Mockito.times(1)).sendMessageToClient(
				expectedSessions, eventMessage);

		pubSubHandler.unregisterSessionFromAllSubscriptions("1");
		pubSubHandler.unregisterSessionFromAllSubscriptions("2");

		eligibleSessions = new HashSet<>();
		eligibleSessions.add("1");
		eligibleSessions.add("2");
		expectedSessions = new HashSet<>();
		eventMessage = new EventMessage("theTopic", "payload");
		pubSubHandler.sendTo(eventMessage, eligibleSessions);
		Mockito.verify(messageSender, Mockito.never()).sendMessageToClient(
				expectedSessions, eventMessage);
	}

}
