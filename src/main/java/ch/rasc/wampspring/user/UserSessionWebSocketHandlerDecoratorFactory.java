/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.rasc.wampspring.user;

import java.security.Principal;

import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket handler decorator that manages the relationship between a
 * {@link Principal#getName()} to a WebSocket session id. The mappings are stored in an
 * {@link SimpUserRegistry} bean.
 *
 * The UserSessionRegistry handling is not enabled by default! See
 * {@link AbstractUserWampConfigurer} for configuration.
 */
public final class UserSessionWebSocketHandlerDecoratorFactory
		implements WebSocketHandlerDecoratorFactory {

	private final ApplicationEventPublisher eventPublisher;

	public UserSessionWebSocketHandlerDecoratorFactory(
			ApplicationEventPublisher eventPublisher) {
		Assert.notNull(eventPublisher, "' eventPublisher' is required ");
		this.eventPublisher = eventPublisher;
	}

	@Override
	public WebSocketHandler decorate(WebSocketHandler handler) {
		return new UserSessionWebSocketHandler(handler);
	}

	private final class UserSessionWebSocketHandler extends WebSocketHandlerDecorator {

		public UserSessionWebSocketHandler(WebSocketHandler delegate) {
			super(delegate);
		}

		@Override
		public void afterConnectionEstablished(WebSocketSession webSocketSession)
				throws Exception {
			super.afterConnectionEstablished(webSocketSession);

			Principal principal = webSocketSession.getPrincipal();
			if (principal != null) {
				SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor
						.create(SimpMessageType.MESSAGE);
				accessor.setSessionId(webSocketSession.getId());

				publishEvent(new SessionConnectedEvent(this, MessageBuilder.createMessage(
						new byte[0], accessor.getMessageHeaders()), principal));
			}
		}

		@Override
		public void afterConnectionClosed(WebSocketSession webSocketSession,
				CloseStatus closeStatus) throws Exception {

			Principal principal = webSocketSession.getPrincipal();
			if (principal != null) {
				SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor
						.create(SimpMessageType.MESSAGE);
				accessor.setSessionId(webSocketSession.getId());

				publishEvent(new SessionDisconnectEvent(this,
						MessageBuilder.createMessage(new byte[0],
								accessor.getMessageHeaders()),
						webSocketSession.getId(), closeStatus, principal));
			}

			super.afterConnectionClosed(webSocketSession, closeStatus);
		}

	}

	private void publishEvent(ApplicationEvent event) {
		try {
			this.eventPublisher.publishEvent(event);
		}
		catch (Throwable ex) {
			LogFactory.getLog(this.getClass()).error("Error publishing " + event, ex);
		}
	}

}
