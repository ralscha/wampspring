/**
 * Copyright 2015 Ralph Schaer <ralphschaer@gmail.com>
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
package ch.rasc.wampspring.session;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.session.ExpiringSession;
import org.springframework.session.SessionRepository;
import org.springframework.web.socket.server.HandshakeInterceptor;

import ch.rasc.wampspring.config.AbstractWampConfigurer;
import ch.rasc.wampspring.config.WebSocketTransportRegistration;

public abstract class AbstractSessionWampConfigurer<S extends ExpiringSession> extends
		AbstractWampConfigurer {

	@Autowired
	private SessionRepository<S> sessionRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	/*
	 * WebSocketConnectHandlerDecoratorFactory is added as a
	 * WebSocketHandlerDecoratorFactory to WebSocketTransportRegistration. This ensures a
	 * custom SessionDisconnectEvent is fired that contains the WebSocketSession. The
	 * WebSocketSession is necessary to terminate any WebSocket connections that are still
	 * open when a Spring Session is terminated.
	 */
	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		registration.addDecoratorFactory(new WebSocketConnectHandlerDecoratorFactory(
				this.eventPublisher));
	}

	/*
	 * WebSocketRegistryListener is created as a Spring Bean. This ensures that we have a
	 * mapping of all of the Session id to the corresponding WebSocket connections. By
	 * maintaining this mapping, we can close all the WebSocket connections when a Spring
	 * Session (HttpSession) is terminated.
	 */
	@Bean
	public WebSocketRegistryListener webSocketRegistryListener() {
		return new WebSocketRegistryListener();
	}

	/*
	 * SessionRepositoryMessageInterceptor is added as a HandshakeInterceptor to every
	 * WampWebSocketEndpointRegistration. This ensures that the Session is added to the
	 * WebSocket properties to enable updating the last accessed time.
	 */
	@Override
	public void addHandshakeInterceptors(List<HandshakeInterceptor> handshakeInterceptors) {
		handshakeInterceptors.add(new SessionRepositoryHandshakeInterceptor());
	}

	/*
	 * SessionRepositoryMessageInterceptor is added as a ChannelInterceptor to our inbound
	 * ChannelRegistration. This ensures that every time an inbound message is received,
	 * that the last accessed time of our Spring Session is updated.
	 */
	@Override
	public void configureClientInboundChannel(AbstractMessageChannel channel) {
		channel.addInterceptor(new SessionRepositoryMessageInterceptor<>(
				this.sessionRepository));
	}

}
