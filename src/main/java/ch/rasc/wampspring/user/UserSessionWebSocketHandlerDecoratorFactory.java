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

import org.springframework.messaging.simp.user.UserSessionRegistry;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * WebSocket handler decorator that manages the relationship between a
 * {@link Principal#getName()} to a WebSocket session id. The mappings are stored in an
 * {@link org.springframework.messaging.simp.user.UserSessionRegistry} bean.
 *
 * The UserSessionRegistry handling is not enabled by default! See
 * {@link AbstractUserWampConfigurer} for configuration.
 */
public final class UserSessionWebSocketHandlerDecoratorFactory
		implements WebSocketHandlerDecoratorFactory {

	private final UserSessionRegistry userSessionRegistry;

	public UserSessionWebSocketHandlerDecoratorFactory(
			UserSessionRegistry userSessionRegistry) {
		Assert.notNull(userSessionRegistry, "'userSessionRegistry' is required ");
		this.userSessionRegistry = userSessionRegistry;
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
				UserSessionWebSocketHandlerDecoratorFactory.this.userSessionRegistry
						.registerSessionId(principal.getName(), webSocketSession.getId());
			}
		}

		@Override
		public void afterConnectionClosed(WebSocketSession webSocketSession,
				CloseStatus closeStatus) throws Exception {

			Principal principal = webSocketSession.getPrincipal();
			if (principal != null) {
				String userName = principal.getName();
				UserSessionWebSocketHandlerDecoratorFactory.this.userSessionRegistry
						.unregisterSessionId(userName, webSocketSession.getId());
			}

			super.afterConnectionClosed(webSocketSession, closeStatus);
		}

	}
}
