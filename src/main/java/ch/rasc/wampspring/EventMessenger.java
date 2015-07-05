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

import ch.rasc.wampspring.broker.SimpleBrokerMessageHandler;
import ch.rasc.wampspring.message.EventMessage;

/**
 * A messenger that allows the calling code to send {@link EventMessage}s to either the
 * broker or directly to client. The EventMessenger is by default configured as a spring
 * managed bean and can be autowired into any other spring bean.
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

	private final MessageChannel brokerChannel;

	private final MessageChannel clientOutboundChannel;

	private volatile long sendTimeout = -1;

	public EventMessenger(MessageChannel brokerChannel,
			MessageChannel clientOutboundChannel) {

		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");
		Assert.notNull(clientOutboundChannel, "'clientOutboundChannel' must not be null");

		this.brokerChannel = brokerChannel;
		this.clientOutboundChannel = clientOutboundChannel;
	}

	/**
	 * Send an {@link EventMessage} to every client that is currently subscribed to the
	 * given topicURI
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 */
	public void sendToAll(String topicURI, Object event) {
		send(new EventMessage(topicURI, event));
	}

	/**
	 * Send an {@link EventMessage} to every client that is currently subscribed to the
	 * provided topicURI except the one provided with the excludeSessionId parameter.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param excludeWebSocketSessionId a WebSocket session id that will be excluded
	 */
	public void sendToAllExcept(String topicURI, Object event,
			String excludeWebSocketSessionId) {
		sendToAllExcept(topicURI, event,
				Collections.singleton(excludeWebSocketSessionId));
	}

	/**
	 * Send an {@link EventMessage} to every client that is currently subscribed to the
	 * provided topicURI except the ones listed in the excludeSessionIds set.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param excludeWebSocketSessionIds a set of WebSocket session ids that will be
	 * excluded. If null or empty no client will be excluded.
	 */
	public void sendToAllExcept(String topicURI, Object event,
			Set<String> excludeWebSocketSessionIds) {
		EventMessage eventMessage = new EventMessage(topicURI, event);
		eventMessage.setExcludeWebSocketSessionIds(excludeWebSocketSessionIds);
		send(eventMessage);
	}

	/**
	 * Send an {@link EventMessage} to every client that is currently subscribed to the
	 * given topicURI and is listed in the eligibleSessionIds set. If no session of the
	 * provided set is subscribed to the topicURI nothing happens.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param eligibleWebSocketSessionIds only the WebSocket session ids listed here will
	 * receive the EVENT message. If null or empty nobody receives the message.
	 */
	public void sendTo(String topicURI, Object event,
			Set<String> eligibleWebSocketSessionIds) {
		EventMessage eventMessage = new EventMessage(topicURI, event);
		eventMessage.setEligibleWebSocketSessionIds(eligibleWebSocketSessionIds);
		send(eventMessage);
	}

	/**
	 * Send an {@link EventMessage} to one client that is subscribed to the given
	 * topicURI. If the client with the given WebSocket session id is not subscribed to
	 * the topicURI nothing happens.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param eligibleWebSocketSessionId only the client with the WebSocket session id
	 * listed here will receive the EVENT message
	 */
	public void sendTo(String topicURI, Object event, String eligibleWebSocketSessionId) {
		sendTo(topicURI, event, Collections.singleton(eligibleWebSocketSessionId));
	}

	/**
	 * Send an EventMessage directly to each client listed in the webSocketSessionId set
	 * parameter. If parameter webSocketSessionIds is null or empty no messages are sent.
	 * <p>
	 * In contrast to {@link #sendTo(String, Object, Set)} this method does not check if
	 * the receivers are subscribed to the destination. The
	 * {@link SimpleBrokerMessageHandler} is not involved in sending these messages.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param webSocketSessionIds list of receivers for the EVENT message
	 */
	public void sendToDirect(String topicURI, Object event,
			Set<String> webSocketSessionIds) {
		if (webSocketSessionIds != null) {
			for (String webSocketSessionId : webSocketSessionIds) {
				EventMessage eventMessage = new EventMessage(topicURI, event);
				eventMessage.setWebSocketSessionId(webSocketSessionId);
				sendDirect(eventMessage);
			}
		}
	}

	/**
	 * Send an EventMessage directly to the client specified with the webSocketSessionId
	 * parameter.
	 * <p>
	 * In contrast to {@link #sendTo(String, Object, String)} this method does not check
	 * if the receiver is subscribed to the destination. The
	 * {@link SimpleBrokerMessageHandler} is not involved in sending this message.
	 *
	 * @param topicURI the name of the topic
	 * @param event the payload of the {@link EventMessage}
	 * @param webSocketSessionId receiver of the EVENT message
	 */
	public void sendToDirect(String topicURI, Object event, String webSocketSessionId) {
		Assert.notNull(webSocketSessionId, "WebSocket session id must not be null");

		sendToDirect(topicURI, event, Collections.singleton(webSocketSessionId));
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * Send an EventMessage to the {@link SimpleBrokerMessageHandler}. The broker looks up
	 * if the receiver of the message ({@link EventMessage#getWebSocketSessionId()}) is
	 * subscribed to the destination ({@link EventMessage#getDestination()}). If the
	 * receiver is subscribed the broker sends the message to him.
	 *
	 * @param eventMessage The event message
	 */
	public void send(EventMessage eventMessage) {
		long timeout = this.sendTimeout;
		boolean sent = timeout >= 0 ? this.brokerChannel.send(eventMessage, timeout)
				: this.brokerChannel.send(eventMessage);

		if (!sent) {
			throw new MessageDeliveryException(eventMessage,
					"Failed to send message with destination '"
							+ eventMessage.getDestination() + "' within timeout: "
							+ timeout);
		}
	}

	/**
	 * Send an EventMessage directly to the client (
	 * {@link EventMessage#getWebSocketSessionId()}).
	 * <p>
	 * In contrast to {@link #send(EventMessage)} this method does not check if the
	 * receiver is subscribed to the destination. The {@link SimpleBrokerMessageHandler}
	 * is not involved in sending this message.
	 *
	 * @param eventMessage The event message
	 *
	 * @see #send(EventMessage)
	 */
	public void sendDirect(EventMessage eventMessage) {
		Assert.notNull(eventMessage.getWebSocketSessionId(),
				"WebSocket session id must not be null");

		long timeout = this.sendTimeout;
		boolean sent = timeout >= 0
				? this.clientOutboundChannel.send(eventMessage, timeout)
				: this.clientOutboundChannel.send(eventMessage);

		if (!sent) {
			throw new MessageDeliveryException(eventMessage,
					"Failed to direct send message with destination '"
							+ eventMessage.getDestination() + "' within timeout: "
							+ timeout);
		}
	}

}
