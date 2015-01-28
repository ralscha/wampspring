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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.messaging.simp.user.UserSessionRegistry;
import org.springframework.util.Assert;

import ch.rasc.wampspring.EventMessenger;
import ch.rasc.wampspring.broker.SimpleBrokerMessageHandler;
import ch.rasc.wampspring.message.EventMessage;

/**
 * Enhanced {@link EventMessenger} that has additional methods that allow a sender to send
 * {@link EventMessage} to a given username in addition to a WebSocket session id.
 *
 * This class is not registered by default. See {@link AbstractUserWampConfigurer} for
 * configuration.
 *
 * @see AbstractUserWampConfigurer
 * @see EventMessenger
 */
public class UserEventMessenger {

	private final EventMessenger eventMessenger;

	private final UserSessionRegistry userSessionRegistry;

	public UserEventMessenger(EventMessenger eventMessenger,
			UserSessionRegistry userSessionRegistry) {
		Assert.notNull(eventMessenger, "'eventMessenger' must not be null");
		Assert.notNull(userSessionRegistry, "'userSessionRegistry' must not be null");
		this.eventMessenger = eventMessenger;
		this.userSessionRegistry = userSessionRegistry;
	}

	public void setSendTimeout(long sendTimeout) {
		this.eventMessenger.setSendTimeout(sendTimeout);
	}

	public void send(EventMessage eventMessage) {
		this.eventMessenger.send(eventMessage);
	}

	public void sendToAll(String topicURI, Object event) {
		this.eventMessenger.sendToAll(topicURI, event);
	}

	public void sendToAllExcept(String topicURI, Object event, String excludeSessionId) {
		this.eventMessenger.sendToAllExcept(topicURI, event, excludeSessionId);
	}

	public void sendToAllExcept(String topicURI, Object event,
			Set<String> excludeSessionIds) {
		this.eventMessenger.sendToAllExcept(topicURI, event, excludeSessionIds);
	}

	public void sendTo(String topicURI, Object event, Set<String> eligibleSessionIds) {
		this.eventMessenger.sendTo(topicURI, event, eligibleSessionIds);
	}

	public void sendTo(String topicURI, Object event, String eligibleSessionId) {
		this.eventMessenger.sendTo(topicURI, event, eligibleSessionId);
	}

	/**
	 * Send an {@link EventMessage} to every client that is currently subscribed to the
	 * provided topicURI except the one provided with the excludeUser parameter.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param user the user that will be excluded
	 */
	public void sendToAllExceptUser(String topicURI, Object event, String excludeUser) {
		sendToAllExceptUsers(topicURI, event, Collections.singleton(excludeUser));
	}

	/**
	 * Send an {@link EventMessage} to every client that is currently subscribed to the
	 * provided topicURI except the ones listed in the excludeUsers set.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param excludeUsers a set of users that will be excluded. If null or empty no user
	 * will be excluded.
	 */
	public void sendToAllExceptUsers(String topicURI, Object event,
			Set<String> excludeUsers) {

		Set<String> excludeSessionIds = null;
		if (excludeUsers != null && !excludeUsers.isEmpty()) {
			excludeSessionIds = new HashSet<>(excludeUsers.size());

			for (String user : excludeUsers) {
				excludeSessionIds.addAll(this.userSessionRegistry.getSessionIds(user));
			}
		}

		this.eventMessenger.sendToAllExcept(topicURI, event, excludeSessionIds);
	}

	/**
	 * Send an {@link EventMessage} to every client that is currently subscribed to the
	 * given topicURI and are listed in the eligibleUsers set. If no user of the provided
	 * set is subscribed to the topicURI nothing happens.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param eligibleUsers only the users listed here will receive the EVENT message. If
	 * null or empty nobody receives the message.
	 */
	public void sendToUsers(String topicURI, Object event, Set<String> eligibleUsers) {

		Set<String> eligibleSessionIds = null;
		if (eligibleUsers != null && !eligibleUsers.isEmpty()) {
			eligibleSessionIds = new HashSet<>(eligibleUsers.size());

			for (String user : eligibleUsers) {
				eligibleSessionIds.addAll(this.userSessionRegistry.getSessionIds(user));
			}
		}

		this.eventMessenger.sendTo(topicURI, event, eligibleSessionIds);
	}

	/**
	 * Send an {@link EventMessage} to one client that is subscribed to the given
	 * topicURI. If the client with the given user name is not subscribed to the topicURI
	 * nothing happens.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param eligibleUser only the user listed here will receive the message
	 */
	public void sendToUser(String topicURI, Object event, String eligibleUser) {
		sendToUsers(topicURI, event, Collections.singleton(eligibleUser));
	}

	/**
	 * Send an EventMessage directly to each client listed in the users set parameter. A
	 * user is ignored if there is no entry in the {@link UserSessionRegistry} for his
	 * username.
	 * <p>
	 * In contrast to {@link #sendToUsers(String, Object, Set)} this method does not check
	 * if the receivers are subscribed to the destination. The
	 * {@link SimpleBrokerMessageHandler} is not involved in sending these messages.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param users list of receivers for the EVENT message
	 */
	public void sendToUsersDirect(String topicURI, Object event, Set<String> users) {

		Set<String> webSocketSessionIds = null;
		if (users != null && !users.isEmpty()) {
			webSocketSessionIds = new HashSet<>(users.size());

			for (String user : users) {
				webSocketSessionIds.addAll(this.userSessionRegistry.getSessionIds(user));
			}
		}

		this.eventMessenger.sendToDirect(topicURI, event, webSocketSessionIds);
	}

	/**
	 * Send an EventMessage directly to the client specified with the user parameter. If
	 * there is no entry in the {@link UserSessionRegistry} for this user nothing happens.
	 * <p>
	 * In contrast to {@link #sendToUser(String, Object, String)} this method does not
	 * check if the receiver is subscribed to the destination. The
	 * {@link SimpleBrokerMessageHandler} is not involved in sending this message.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param user receiver of the EVENT message
	 */
	public void sendToUserDirect(String topicURI, Object event, String user) {
		sendToUsersDirect(topicURI, event, Collections.singleton(user));
	}
}
