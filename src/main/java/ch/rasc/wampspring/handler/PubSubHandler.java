/**
 * Copyright 2014-2014 Ralph Schaer <ralphschaer@gmail.com>
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WampMessageHeader;

/**
 * Internal handler that handles the Publish and Subscribe part of WAMP.
 * <p>
 * The messages that are handled here are {@link EventMessage}, {@link PublishMessage}, {@link SubscribeMessage} and
 * {@link UnsubscribeMessage}
 * <p>
 * The handler manages a map (topicSessionIds) that holds a set of WebSocket session ids per topicURI.
 */
public class PubSubHandler {

	private final Log logger = LogFactory.getLog(getClass());

	private final Map<String, Set<String>> topicSessionIds = new ConcurrentHashMap<>();

	private final Object monitor = new Object();

	private final WampMessageSender wampMessageSender;

	public PubSubHandler(WampMessageSender wampMessageSender) {
		this.wampMessageSender = wampMessageSender;
	}

	void handleMessage(WampMessage message) {
		switch (message.getType()) {

		case EVENT:
			sendToAll((EventMessage) message);
			break;
		case PUBLISH:
			handlePublishMessage((PublishMessage) message);
			break;
		case SUBSCRIBE:
			handleSubscribeMessage((SubscribeMessage) message);
			break;
		case UNSUBSCRIBE:
			handleUnsubscribeMessage((UnsubscribeMessage) message);
			break;
		default:
			break;
		}
	}

	public void sendToAll(EventMessage eventMessage) {
		Set<String> sessions = topicSessionIds.get(eventMessage.getTopicURI());
		wampMessageSender.sendMessageToClient(sessions, eventMessage);
	}

	public void sendToAllExcept(EventMessage eventMessage, Set<String> excludeSessionIds) {
		Set<String> subscriptionSessions = topicSessionIds.get(eventMessage.getTopicURI());
		if (subscriptionSessions != null) {
			Set<String> eligibleSessions = new HashSet<>();
			for (String sessionId : subscriptionSessions) {
				if (!excludeSessionIds.contains(sessionId)) {
					eligibleSessions.add(sessionId);
				}
			}
			wampMessageSender.sendMessageToClient(eligibleSessions, eventMessage);
		}
	}

	public void sendTo(EventMessage eventMessage, Set<String> eligibleSessionIds) {
		Set<String> subscriptionSessions = topicSessionIds.get(eventMessage.getTopicURI());
		if (subscriptionSessions != null) {
			Set<String> eligibleSessions = new HashSet<>();
			for (String sessionId : subscriptionSessions) {
				if (eligibleSessionIds.contains(sessionId)) {
					eligibleSessions.add(sessionId);
				}
			}
			wampMessageSender.sendMessageToClient(eligibleSessions, eventMessage);
		}
	}

	private void handlePublishMessage(PublishMessage publishMessage) {
		Set<String> subscriptionSessions = topicSessionIds.get(publishMessage.getTopicURI());
		if (subscriptionSessions != null) {
			String mySessionId = publishMessage.getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID);
			Set<String> eligibleSessions = new HashSet<>();
			for (String sessionId : subscriptionSessions) {
				if (isSessionEligible(publishMessage, mySessionId, sessionId)) {
					eligibleSessions.add(sessionId);
				}
			}

			if (!eligibleSessions.isEmpty()) {
				EventMessage eventMessage = new EventMessage(publishMessage.getTopicURI(), publishMessage.getEvent());
				wampMessageSender.sendMessageToClient(eligibleSessions, eventMessage);
			}
		}
	}

	private static boolean isSessionEligible(PublishMessage publishMessage, String mySessionId, String otherSessionId) {

		if (publishMessage.getExcludeMe() != null && publishMessage.getExcludeMe()) {
			if (mySessionId.equals(otherSessionId)) {
				return false;
			}
		}

		if (publishMessage.getEligible() != null) {
			if (!publishMessage.getEligible().contains(otherSessionId)) {
				return false;
			}
		}

		if (publishMessage.getExclude() != null) {
			if (publishMessage.getExclude().contains(otherSessionId)) {
				return false;
			}
		}

		return true;
	}

	private void handleSubscribeMessage(SubscribeMessage message) {
		String sessionId = message.getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID);
		String topicURI = message.getTopicURI();

		synchronized (this.monitor) {
			Set<String> sessions = topicSessionIds.get(topicURI);
			if (sessions == null) {
				sessions = new CopyOnWriteArraySet<>();
				topicSessionIds.put(topicURI, sessions);
			}
			sessions.add(sessionId);
		}
	}

	private void handleUnsubscribeMessage(UnsubscribeMessage message) {
		String sessionId = message.getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID);
		String topicURI = message.getTopicURI();
		if (topicURI == null) {
			logger.error("Ignoring subscription. No topicURI in message: " + message);
			return;
		}

		removeSession(sessionId, topicURI);
	}

	List<String> unregisterSessionFromAllSubscriptions(String sessionId) {
		List<String> topicURIs = new ArrayList<>();
		for (String topicURI : topicSessionIds.keySet()) {
			if (removeSession(sessionId, topicURI)) {
				topicURIs.add(topicURI);
			}
		}
		return topicURIs;
	}

	private boolean removeSession(String sessionId, String topicURI) {
		synchronized (this.monitor) {
			Set<String> sessions = topicSessionIds.get(topicURI);
			if (sessions != null) {
				boolean removed = sessions.remove(sessionId);
				if (removed) {
					if (sessions.isEmpty()) {
						topicSessionIds.remove(topicURI);
					}
				}
				return removed;
			}
			return false;
		}
	}

}
