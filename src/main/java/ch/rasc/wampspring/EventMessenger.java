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

import java.util.Collections;
import java.util.Set;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.util.Assert;

import ch.rasc.wampspring.message.EventMessage;

/**
 * A messenger that allows the calling code to send {@link EventMessage}s to the broker.
 * The EventMessenger is by default configured as a spring managed bean and can be
 * autowired into any other spring bean.
 *
 * e.g.
 *
 * <pre class="code">
 * &#064;Service
 * public class MyService {
 * 	&#064;Autowired
 * 	EventMessenger eventMessenger;
 * 
 * 	public void doSomething() {
 * 		eventMessenger.sendToAll(&quot;aTopic&quot;, &quot;the message&quot;);
 * 	}
 * }
 * </pre>
 */
public class EventMessenger {

	private final MessageChannel messageChannel;

	private volatile long sendTimeout = -1;

	public EventMessenger(MessageChannel messageChannel) {
		Assert.notNull(messageChannel, "'messageChannel' must not be null");
		this.messageChannel = messageChannel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void send(EventMessage eventMessage) {
		long timeout = this.sendTimeout;
		boolean sent = timeout >= 0 ? this.messageChannel.send(eventMessage, timeout)
				: this.messageChannel.send(eventMessage);

		if (!sent) {
			throw new MessageDeliveryException(eventMessage,
					"Failed to send message with destination '"
							+ eventMessage.getDestination() + "' within timeout: "
							+ timeout);
		}
	}

	/**
	 * Send a {@link EventMessage} to every client that is currently subscribed to the
	 * given topicURI
	 *
	 * @param topicURI the name of the topic
	 * @param event the message
	 */
	public void sendToAll(String topicURI, Object event) {
		send(new EventMessage(topicURI, event));
	}

	/**
	 * Send a {@link EventMessage} to every client that is currently subscribed to the
	 * provided topicURI except the one provided with the excludeSessionId parameter.
	 *
	 * @param topicURI the name of the topic
	 * @param event the message
	 * @param excludeSessionId a WebSocket session id that will be excluded
	 */
	public void sendToAllExcept(String topicURI, Object event, String excludeSessionId) {
		sendToAllExcept(topicURI, event, Collections.singleton(excludeSessionId));
	}

	/**
	 * Send a {@link EventMessage} to every client that is currently subscribed to the
	 * provided topicURI except the ones listed in the excludeSessionIds set.
	 *
	 * @param topicURI the name of the topic
	 * @param event the message
	 * @param excludeSessionIds a set of WebSocket session ids that will be excluded
	 */
	public void sendToAllExcept(String topicURI, Object event,
			Set<String> excludeSessionIds) {
		EventMessage eventMessage = new EventMessage(topicURI, event);
		eventMessage.setExcludeSessionIds(excludeSessionIds);
		send(eventMessage);
	}

	/**
	 * Send an {@link EventMessage} to the clients that are subscribed to the given
	 * topicURI and are listed in the eligibleSessionIds set. If no session of the
	 * provided set is subscribed to the topicURI nothing happens.
	 *
	 * @param topicURI the name of the topic
	 * @param event the message
	 * @param eligibleSessionIds only the WebSocket session ids listed here will receive
	 * the message
	 */
	public void sendTo(String topicURI, Object event, Set<String> eligibleSessionIds) {
		EventMessage eventMessage = new EventMessage(topicURI, event);
		eventMessage.setEligibleSessionIds(eligibleSessionIds);
		send(eventMessage);
	}

	/**
	 * Send an {@link EventMessage} to one client that is subscribed to the given
	 * topicURI. If the client with the given sessionId is not subscribed to the topicURI
	 * nothing happens.
	 *
	 * @param topicURI the name of the topic
	 * @param event the message
	 * @param eligibleSessionIds only the WebSocket session ids listed here will receive
	 * the message
	 */
	public void sendTo(String topicURI, Object event, String eligibleSessionId) {
		sendTo(topicURI, event, Collections.singleton(eligibleSessionId));
	}
}
