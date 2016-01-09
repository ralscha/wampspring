/**
 * Copyright 2014-2016 Ralph Schaer <ralphschaer@gmail.com>
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
 * A convenient implementation of the {@link WampConfigurer} interface, providing empty
 * method.
 */
public class AbstractWampConfigurer implements WampConfigurer {

	@Override
	public void registerWampEndpoints(WampEndpointRegistry registry) {
		// by default nothing here
	}

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		// by default nothing here
	}

	@Override
	public void configureClientInboundChannel(AbstractMessageChannel channel) {
		// by default nothing here
	}

	@Override
	public void addArgumentResolvers(
			List<HandlerMethodArgumentResolver> argumentResolvers) {
		// by default nothing here
	}

	@Override
	public void addHandshakeInterceptors(
			List<HandshakeInterceptor> handshakeInterceptors) {
		// by default nothing here
	}

}
