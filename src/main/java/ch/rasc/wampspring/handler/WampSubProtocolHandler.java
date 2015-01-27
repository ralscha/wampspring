/**
 * Copyright 2002-2015 the original author or authors.
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.messaging.SubProtocolHandler;

import ch.rasc.wampspring.message.CallErrorMessage;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WampMessageHeader;
import ch.rasc.wampspring.message.WelcomeMessage;

import com.fasterxml.jackson.core.JsonFactory;

/**
 * A WebSocket {@link SubProtocolHandler} for the WAMP v1 protocol.
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 * @author Ralph Schaer
 */
public class WampSubProtocolHandler implements SubProtocolHandler {

	public static final int MINIMUM_WEBSOCKET_MESSAGE_SIZE = 16 * 1024 + 256;

	private static final Log logger = LogFactory.getLog(WampSubProtocolHandler.class);

	private static final String SERVER_IDENTIFIER = "wampspring/1.1";

	private final JsonFactory jsonFactory;

	public WampSubProtocolHandler(JsonFactory jsonFactory) {
		this.jsonFactory = jsonFactory;
	}

	@Override
	public List<String> getSupportedProtocols() {
		return Collections.singletonList("wamp");
	}

	/**
	 * Handle incoming WebSocket messages from clients.
	 */
	@Override
	public void handleMessageFromClient(WebSocketSession session,
			WebSocketMessage<?> webSocketMessage, MessageChannel outputChannel) {

		Assert.isInstanceOf(TextMessage.class, webSocketMessage);
		WampMessage wampMessage = null;
		try {
			wampMessage = WampMessage.fromJson(session, this.jsonFactory,
					((TextMessage) webSocketMessage).getPayload());
		}
		catch (Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Failed to parse " + webSocketMessage + " in session "
						+ session.getId() + ".", ex);
			}
			return;
		}

		try {
			WampSessionContextHolder.setAttributesFromMessage(wampMessage);
			outputChannel.send(wampMessage);
		}
		catch (Throwable ex) {
			logger.error(
					"Failed to send client message to application via MessageChannel"
							+ " in session " + session.getId() + ".", ex);

			if (wampMessage != null && wampMessage instanceof CallMessage) {

				CallErrorMessage callErrorMessage = new CallErrorMessage(
						(CallMessage) wampMessage, "", ex.toString());

				try {
					String json = callErrorMessage.toJson(this.jsonFactory);
					session.sendMessage(new TextMessage(json));
				}
				catch (Throwable t) {
					// Could be part of normal workflow (e.g. browser tab closed)
					logger.debug("Failed to send error to client.", t);
				}

			}
		}
		finally {
			WampSessionContextHolder.resetAttributes();
		}
	}

	/**
	 * Handle WAMP messages going back out to WebSocket clients.
	 */
	@Override
	public void handleMessageToClient(WebSocketSession session, Message<?> message) {
		if (!(message instanceof WampMessage)) {
			logger.error("Expected WampMessage. Ignoring " + message + ".");
			return;
		}

		boolean closeWebSocketSession = false;
		try {
			String json = ((WampMessage) message).toJson(this.jsonFactory);
			session.sendMessage(new TextMessage(json));
		}
		catch (SessionLimitExceededException ex) {
			// Bad session, just get out
			throw ex;
		}
		catch (Throwable ex) {
			// Could be part of normal workflow (e.g. browser tab closed)
			logger.debug("Failed to send WebSocket message to client in session "
					+ session.getId() + ".", ex);
			closeWebSocketSession = true;
		}
		finally {
			if (closeWebSocketSession) {
				try {
					session.close(CloseStatus.PROTOCOL_ERROR);
				}
				catch (IOException ex) {
					// Ignore
				}
			}
		}
	}

	@Override
	public String resolveSessionId(Message<?> message) {
		return (String) message.getHeaders().get(
				WampMessageHeader.WEBSOCKET_SESSION_ID.name());
	}

	@Override
	public void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) {
		if (session.getTextMessageSizeLimit() < MINIMUM_WEBSOCKET_MESSAGE_SIZE) {
			session.setTextMessageSizeLimit(MINIMUM_WEBSOCKET_MESSAGE_SIZE);
		}

		WelcomeMessage welcomeMessage = new WelcomeMessage(session.getId(),
				SERVER_IDENTIFIER);
		try {
			session.sendMessage(new TextMessage(welcomeMessage.toJson(this.jsonFactory)));
		}
		catch (IOException e) {
			logger.error(
					"Failed to send welcome message to client in session "
							+ session.getId() + ".", e);
		}
	}

	@Override
	public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus,
			MessageChannel outputChannel) {
		/*
		 * To cleanup we send an internal messages to the handlers. It might be possible
		 * that this is an unexpected session end and the client did not unsubscribe his
		 * subscriptions.
		 */
		WampMessage message = UnsubscribeMessage.createCleanupMessage(session);

		try {
			WampSessionContextHolder.setAttributesFromMessage(message);
			outputChannel.send(message);
		}
		finally {
			WampSessionContextHolder.resetAttributes();
			message.getWampSession().sessionCompleted();
		}
	}

	@Override
	public String toString() {
		return "WampSubProtocolHandler " + getSupportedProtocols();
	}

}
