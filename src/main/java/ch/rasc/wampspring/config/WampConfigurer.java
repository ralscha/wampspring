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
package ch.rasc.wampspring.config;

import java.util.List;

import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Defines methods for configuring WAMP support.
 *
 * <p>
 * Used together with {@link EnableWamp}
 */
public interface WampConfigurer {

	/**
	 * Register WAMP endpoints mapping each to a specific URL and (optionally) enabling
	 * and configuring SockJS fallback options.
	 */
	void registerWampEndpoints(WampEndpointRegistry registry);

	/**
	 * Configure options related to the processing of messages received from and sent to
	 * WebSocket clients.
	 */
	void configureWebSocketTransport(WebSocketTransportRegistration registration);

	/**
	 * Configure the {@link org.springframework.messaging.MessageChannel} used for
	 * incoming messages from WebSocket clients.
	 */
	void configureClientInboundChannel(AbstractMessageChannel channel);

	/**
	 * Add resolvers to support custom controller method argument types.
	 * <p>
	 * This does not override the built-in argument resolvers.
	 * @param argumentResolvers the resolvers to register (initially an empty list)
	 */
	void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers);

	/*
	 * Handshake interceptors that are added to every WAMP endpoint mapping.
	 */
	void addHandshakeInterceptors(List<HandshakeInterceptor> handshakeInterceptors);

}
