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
package ch.rasc.wampspring.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

/**
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class WebMvcWampWebSocketEndpointRegistration implements
		WampWebSocketEndpointRegistration {

	private final String[] paths;

	private final WebSocketHandler webSocketHandler;

	private final TaskScheduler sockJsTaskScheduler;

	private HandshakeHandler handshakeHandler;

	private final List<HandshakeInterceptor> interceptors = new ArrayList<>();

	private final List<String> allowedOrigins = new ArrayList<>();

	private WampSockJsServiceRegistration registration;

	public WebMvcWampWebSocketEndpointRegistration(String[] paths,
			WebSocketHandler webSocketHandler, TaskScheduler sockJsTaskScheduler) {

		Assert.notEmpty(paths, "No paths specified");
		Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");

		this.paths = paths;
		this.webSocketHandler = webSocketHandler;
		this.sockJsTaskScheduler = sockJsTaskScheduler;
	}

	@Override
	public WampWebSocketEndpointRegistration setHandshakeHandler(
			HandshakeHandler handshakeHandler) {
		Assert.notNull(handshakeHandler, "'handshakeHandler' must not be null");
		this.handshakeHandler = handshakeHandler;
		return this;
	}

	@Override
	public WampWebSocketEndpointRegistration addInterceptors(
			HandshakeInterceptor... handshakeInterceptors) {
		if (!ObjectUtils.isEmpty(handshakeInterceptors)) {
			this.interceptors.addAll(Arrays.asList(handshakeInterceptors));
		}
		return this;
	}

	@Override
	public WampWebSocketEndpointRegistration setAllowedOrigins(String... origins) {
		this.allowedOrigins.clear();
		if (!ObjectUtils.isEmpty(origins)) {
			this.allowedOrigins.addAll(Arrays.asList(origins));
		}
		return this;
	}

	@Override
	public SockJsServiceRegistration withSockJS() {
		this.registration = new WampSockJsServiceRegistration(this.sockJsTaskScheduler);
		HandshakeInterceptor[] handshakeInterceptors = getInterceptors();
		if (handshakeInterceptors.length > 0) {
			this.registration.setInterceptors(handshakeInterceptors);
		}
		if (this.handshakeHandler != null) {
			WebSocketTransportHandler transportHandler = new WebSocketTransportHandler(
					this.handshakeHandler);
			this.registration.setTransportHandlerOverrides(transportHandler);
		}
		if (!this.allowedOrigins.isEmpty()) {
			this.registration.setAllowedOrigins(this.allowedOrigins
					.toArray(new String[this.allowedOrigins.size()]));
		}
		return this.registration;
	}

	protected HandshakeInterceptor[] getInterceptors() {
		List<HandshakeInterceptor> handshakeInterceptors = new ArrayList<>();
		handshakeInterceptors.addAll(this.interceptors);
		handshakeInterceptors.add(new OriginHandshakeInterceptor(this.allowedOrigins));
		return handshakeInterceptors
				.toArray(new HandshakeInterceptor[handshakeInterceptors.size()]);
	}

	public final MultiValueMap<HttpRequestHandler, String> getMappings() {
		MultiValueMap<HttpRequestHandler, String> mappings = new LinkedMultiValueMap<>();
		if (this.registration != null) {
			SockJsService sockJsService = this.registration.getSockJsService();
			for (String path : this.paths) {
				String pattern = path.endsWith("/") ? path + "**" : path + "/**";
				SockJsHttpRequestHandler handler = new SockJsHttpRequestHandler(
						sockJsService, this.webSocketHandler);
				mappings.add(handler, pattern);
			}
		}
		else {
			for (String path : this.paths) {
				WebSocketHttpRequestHandler handler;
				if (this.handshakeHandler != null) {
					handler = new WebSocketHttpRequestHandler(this.webSocketHandler,
							this.handshakeHandler);
				}
				else {
					handler = new WebSocketHttpRequestHandler(this.webSocketHandler);
				}
				HandshakeInterceptor[] handshakeInterceptors = getInterceptors();
				if (handshakeInterceptors.length > 0) {
					handler.setHandshakeInterceptors(Arrays.asList(handshakeInterceptors));
				}
				mappings.add(handler, path);
			}
		}
		return mappings;
	}

	private static class WampSockJsServiceRegistration extends SockJsServiceRegistration {

		public WampSockJsServiceRegistration(TaskScheduler defaultTaskScheduler) {
			super(defaultTaskScheduler);
		}

		@Override
		protected SockJsService getSockJsService() {
			return super.getSockJsService();
		}

		@Override
		protected SockJsServiceRegistration setAllowedOrigins(String... origins) {
			return super.setAllowedOrigins(origins);
		}

	}

}
