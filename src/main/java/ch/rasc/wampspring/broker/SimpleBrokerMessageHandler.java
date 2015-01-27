/**
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

import ch.rasc.wampspring.handler.WampSession;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WampMessageType;

/**
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class SimpleBrokerMessageHandler implements MessageHandler, SmartLifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private final SubscribableChannel clientInboundChannel;

	private final MessageChannel clientOutboundChannel;

	private final SubscribableChannel brokerChannel;

	private boolean autoStartup = true;

	private volatile boolean running = false;

	private final Object lifecycleMonitor = new Object();

	private final SubscriptionRegistry subscriptionRegistry;

	private boolean authenticationRequiredGlobal = false;

	public SimpleBrokerMessageHandler(SubscribableChannel inboundChannel,
			MessageChannel outboundChannel, SubscribableChannel brokerChannel,
			SubscriptionRegistry subscriptionRegistry) {

		Assert.notNull(inboundChannel, "'inboundChannel' must not be null");
		Assert.notNull(outboundChannel, "'outboundChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");
		Assert.notNull(subscriptionRegistry, "'subscriptionRegistry' must not be null");

		this.clientInboundChannel = inboundChannel;
		this.clientOutboundChannel = outboundChannel;
		this.brokerChannel = brokerChannel;
		this.subscriptionRegistry = subscriptionRegistry;
	}

	public void setAuthenticationRequiredGlobal(boolean authenticationRequiredGlobal) {
		this.authenticationRequiredGlobal = authenticationRequiredGlobal;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.logger.isInfoEnabled()) {
				this.logger.info("Starting...");
			}
			this.clientInboundChannel.subscribe(this);
			this.brokerChannel.subscribe(this);
			this.running = true;
			this.logger.info("Started.");
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.logger.isInfoEnabled()) {
				this.logger.info("Stopping...");
			}
			this.clientInboundChannel.unsubscribe(this);
			this.brokerChannel.unsubscribe(this);
			this.running = false;
			this.logger.info("Stopped.");
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	@Override
	public final boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	@Override
	public void handleMessage(Message<?> message) {
		if (!this.running) {
			if (this.logger.isTraceEnabled()) {
				this.logger.trace(this + " not running yet. Ignoring " + message);
			}
			return;
		}

		WampMessage wampMessage = (WampMessage) message;
		WampMessageType messageType = wampMessage.getType();

		if (messageType == WampMessageType.EVENT) {
			sendMessageToSubscribers((EventMessage) wampMessage);
		}
		else if (messageType == WampMessageType.PUBLISH) {
			checkAuthentication(wampMessage);
			sendMessageToSubscribers((PublishMessage) wampMessage);
		}
		else if (messageType == WampMessageType.SUBSCRIBE) {
			checkAuthentication(wampMessage);
			this.subscriptionRegistry
					.registerSubscription((SubscribeMessage) wampMessage);
		}
		else if (messageType == WampMessageType.UNSUBSCRIBE) {
			UnsubscribeMessage unsubscribeMessage = (UnsubscribeMessage) wampMessage;
			if (unsubscribeMessage.isCleanup()) {
				this.subscriptionRegistry.unregisterSession(unsubscribeMessage
						.getSessionId());
			}
			else {
				checkAuthentication(wampMessage);
				this.subscriptionRegistry.unregisterSubscription(unsubscribeMessage);
			}
		}

	}

	private void checkAuthentication(WampMessage wampMessage) {
		WampSession wampSession = wampMessage.getWampSession();
		if (wampSession != null && !wampSession.isAuthenticated()
				&& this.authenticationRequiredGlobal) {
			throw new SecurityException("Not authenticated");
		}
	}

	protected void sendMessageToSubscribers(EventMessage eventMessage) {
		Set<String> sessionIds = this.subscriptionRegistry
				.findSubscriptions(eventMessage);

		if (sessionIds.size() > 0) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Broadcasting to " + sessionIds.size() + " sessions.");
			}

			Set<String> eligibleSessionIds = eventMessage.getEligibleSessionIds();
			Set<String> excludeSessionIds = eventMessage.getExcludeSessionIds();

			for (String sessionId : sessionIds) {
				if (excludeSessionIds != null) {
					if (!excludeSessionIds.contains(sessionId)) {
						sendEventMessage(eventMessage, sessionId);
					}
				}
				else if (eligibleSessionIds != null) {
					if (eligibleSessionIds.contains(sessionId)) {
						sendEventMessage(eventMessage, sessionId);
					}
				}
				else {
					sendEventMessage(eventMessage, sessionId);
				}
			}
		}
		else {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("No subscriptions found for " + eventMessage);
			}
		}
	}

	protected void sendMessageToSubscribers(PublishMessage publishMessage) {
		Set<String> subscribedSessionIds = this.subscriptionRegistry
				.findSubscriptions(publishMessage);

		if (subscribedSessionIds.size() > 0) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Broadcasting to " + subscribedSessionIds.size()
						+ " sessions.");
			}

			for (String subscriptionSessionId : subscribedSessionIds) {
				if (isSessionEligible(publishMessage, subscriptionSessionId)) {
					sendEventMessage(publishMessage, subscriptionSessionId);
				}
			}
		}
		else {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("No subscriptions found for " + publishMessage);
			}
		}
	}

	protected void sendEventMessage(EventMessage originEventMessage,
			String receiverSessionId) {
		EventMessage eventMessage = new EventMessage(originEventMessage,
				receiverSessionId);
		sendEventMessage(eventMessage);
	}

	protected void sendEventMessage(PublishMessage publishMessage,
			String receiverSessionId) {
		EventMessage eventMessage = new EventMessage(publishMessage, receiverSessionId);
		sendEventMessage(eventMessage);
	}

	protected void sendEventMessage(EventMessage eventMessage) {
		try {
			this.clientOutboundChannel.send(eventMessage);
		}
		catch (Throwable ex) {
			this.logger.error("Failed to send " + eventMessage, ex);
		}
	}

	private static boolean isSessionEligible(PublishMessage publishMessage,
			String receiverSessionId) {

		String myWebSocketSessionId = publishMessage.getSessionId();

		if (publishMessage.getExcludeMe() != null && publishMessage.getExcludeMe()
				&& myWebSocketSessionId.equals(receiverSessionId)) {
			return false;

		}

		if (publishMessage.getEligible() != null
				&& !publishMessage.getEligible().contains(receiverSessionId)) {
			return false;

		}

		if (publishMessage.getExclude() != null
				&& publishMessage.getExclude().contains(receiverSessionId)) {
			return false;

		}

		return true;
	}

	@Override
	public String toString() {
		return "SimpleBrokerMessageHandler [" + this.subscriptionRegistry + "]";
	}

}
