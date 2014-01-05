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

import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WampMessageHeader;
import ch.rasc.wampspring.message.WelcomeMessage;

import com.fasterxml.jackson.core.JsonFactory;

/**
 * A {@link WebSocketHandler} implementation that handles incoming WAMP
 * requests. It registers the {@link WebSocketSession}s in the
 * {@link WampMessageSender} object, converts the incoming String into a
 * {@link WampMessage} and forwards the message to {@link PubSubHandler} and
 * {@link AnnotationMethodHandler}.
 */
public class WampWebsocketHandler implements WebSocketHandler, SubProtocolCapable {

	private static final List<String> SUPPORTED_SUB_PROTOCOL_LIST = Collections.singletonList("wamp");

	private static final String SERVER_IDENTIFIER = "wampspring/1.0";

	private final WampMessageSender wampMessageSender;

	private final AnnotationMethodHandler annotationMethodHandler;

	private final PubSubHandler pubSubHandler;

	private final JsonFactory jsonFactory;

	public WampWebsocketHandler(AnnotationMethodHandler annotationMethodHandler, PubSubHandler pubSubHandler,
			WampMessageSender wampMessageSender, JsonFactory jsonFactory) {
		this.annotationMethodHandler = annotationMethodHandler;
		this.pubSubHandler = pubSubHandler;
		this.wampMessageSender = wampMessageSender;
		this.jsonFactory = jsonFactory;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		wampMessageSender.put(session.getId(), session);
		WelcomeMessage welcomeMessage = new WelcomeMessage(session.getId(), SERVER_IDENTIFIER);
		session.sendMessage(new TextMessage(welcomeMessage.toJson(jsonFactory)));
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {
		Assert.isInstanceOf(TextMessage.class, webSocketMessage);

		WampMessage message = WampMessage.fromJson(jsonFactory, ((TextMessage) webSocketMessage).getPayload());
		message.addHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, session.getId());
		message.addHeader(WampMessageHeader.PRINCIPAL, session.getPrincipal());

		pubSubHandler.handleMessage(message);
		annotationMethodHandler.handleMessage(message);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		// Do nothing for now. Not sure if we should handle this. After calling
		// this method Spring does close the connection and then calls the
		// afterConnectionClosed method
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		String sessionId = session.getId();

		wampMessageSender.remove(sessionId);

		List<String> topicURIs = pubSubHandler.unregisterSessionFromAllSubscriptions(sessionId);

		// topicURIs contains a list of all the topics where the sessionId was
		// subscribed to. This list is not empty when the WebSocket session was
		// closed for any reason and
		// did not send an UnsubscribeMessage beforehand.
		//
		// For a proper cleanup we now create an UnsubscribeMessage for every
		// listed topicURI, send them to the AnnotationMethodHandler so he can
		// call any available method that is annotated with
		// @WampUnsubscribeListener
		for (String topicURI : topicURIs) {
			UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(topicURI);
			unsubscribeMessage.addHeader(WampMessageHeader.WEBSOCKET_SESSION_ID, sessionId);
			annotationMethodHandler.handleMessage(unsubscribeMessage);
		}
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}

	@Override
	public List<String> getSubProtocols() {
		return SUPPORTED_SUB_PROTOCOL_LIST;
	}

}
