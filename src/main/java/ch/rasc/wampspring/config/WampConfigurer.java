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
package ch.rasc.wampspring.config;

import java.util.concurrent.Executor;

import org.springframework.core.convert.ConversionService;
import org.springframework.util.PathMatcher;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Defines callback methods to configure the WAMP support via {@link EnableWamp}
 */
public interface WampConfigurer {

	/**
	 * Configures an {@link Executor} which is used for sending outbound WAMP
	 * messages.
	 */
	Executor outboundExecutor();

	/**
	 * Configures the endpoint path where the WebSocket WAMP handler is
	 * listening for requests. This path must start with an / character.
	 */
	String wampEndpointPath();

	/**
	 * Configures Jackson's {@link ObjectMapper} instance. This mapper is used
	 * for serializing and deserializing wamp messages.
	 */
	ObjectMapper objectMapper();

	/**
	 * Configures a ConversionService that is used by the
	 * {@link WampMessageBodyMethodArgumentResolver}
	 */
	ConversionService conversionService();
	
	/**
	 * Configures a PathMatcher to detect wildcard like destination mappings<br>
	 * Default configured is {@link AntPathMatcher}
	 * @return
	 */
	PathMatcher pathMatcher();

	/**
	 * Doing some additional configuration of the WampWebsocketHandler
	 * registration. For example reg.withSockJS() turns SockJS support on.
	 */
	void configureWampWebsocketHandler(WebSocketHandlerRegistration reg);
}