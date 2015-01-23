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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.user.UserSessionRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UrlPathHelper;

import ch.rasc.wampspring.handler.WampSubProtocolHandler;

import com.fasterxml.jackson.core.JsonFactory;

/**
 * @author Rossen Stoyanchev
 * @author Artem Bilan
 * @author Ralph Schaer
 */
public class WebMvcWampEndpointRegistry implements WampEndpointRegistry {

	private final WebSocketHandler webSocketHandler;

	private final SubProtocolWebSocketHandler subProtocolWebSocketHandler;

	private final WampSubProtocolHandler wampSubProtocolHandler;

	private final List<WebMvcWampWebSocketEndpointRegistration> registrations = new ArrayList<>();

	private final TaskScheduler sockJsScheduler;

	private HandshakeInterceptor defaultHandshakeInterceptors;

	private int order = 1;

	private UrlPathHelper urlPathHelper;

	public WebMvcWampEndpointRegistry(WebSocketHandler webSocketHandler,
			WebSocketTransportRegistration transportRegistration,
			TaskScheduler defaultSockJsTaskScheduler, JsonFactory jsonFactory) {

		Assert.notNull(webSocketHandler, "'webSocketHandler' is required ");
		Assert.notNull(transportRegistration, "'transportRegistration' is required");
		Assert.notNull(jsonFactory, "'jsonFactory' is required");

		this.webSocketHandler = webSocketHandler;
		this.subProtocolWebSocketHandler = unwrapSubProtocolWebSocketHandler(webSocketHandler);

		if (transportRegistration.getSendTimeLimit() != null) {
			this.subProtocolWebSocketHandler.setSendTimeLimit(transportRegistration
					.getSendTimeLimit());
		}
		if (transportRegistration.getSendBufferSizeLimit() != null) {
			this.subProtocolWebSocketHandler.setSendBufferSizeLimit(transportRegistration
					.getSendBufferSizeLimit());
		}

		this.wampSubProtocolHandler = new WampSubProtocolHandler(jsonFactory);
		this.sockJsScheduler = defaultSockJsTaskScheduler;
	}

	public void setUserSessionRegistry(UserSessionRegistry userSessionRegistry) {
		this.wampSubProtocolHandler.setUserSessionRegistry(userSessionRegistry);
	}

	public void setDefaultHandshakeInterceptors(
			HandshakeInterceptor defaultHandshakeInterceptors) {
		this.defaultHandshakeInterceptors = defaultHandshakeInterceptors;
	}

	private static SubProtocolWebSocketHandler unwrapSubProtocolWebSocketHandler(
			WebSocketHandler wsHandler) {
		WebSocketHandler actual = WebSocketHandlerDecorator.unwrap(wsHandler);
		Assert.isInstanceOf(SubProtocolWebSocketHandler.class, actual,
				"No SubProtocolWebSocketHandler in " + wsHandler);
		return (SubProtocolWebSocketHandler) actual;
	}

	@Override
	public WampWebSocketEndpointRegistration addEndpoint(String... paths) {
		this.subProtocolWebSocketHandler.addProtocolHandler(this.wampSubProtocolHandler);
		WebMvcWampWebSocketEndpointRegistration registration = new WebMvcWampWebSocketEndpointRegistration(
				paths, this.webSocketHandler, this.sockJsScheduler);
		this.registrations.add(registration);

		if (this.defaultHandshakeInterceptors != null) {
			registration.addInterceptors(this.defaultHandshakeInterceptors);
		}

		return registration;
	}

	/**
	 * Set the order for the resulting {@link SimpleUrlHandlerMapping} relative to other
	 * handler mappings configured in Spring MVC.
	 * <p>
	 * The default value is 1.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the UrlPathHelper to configure on the {@code SimpleUrlHandlerMapping} used to
	 * map handshake requests.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * Return a handler mapping with the mapped ViewControllers; or {@code null} in case
	 * of no registrations.
	 */
	public AbstractHandlerMapping getHandlerMapping() {
		Map<String, Object> urlMap = new LinkedHashMap<>();
		for (WebMvcWampWebSocketEndpointRegistration registration : this.registrations) {
			MultiValueMap<HttpRequestHandler, String> mappings = registration
					.getMappings();
			for (HttpRequestHandler httpHandler : mappings.keySet()) {
				for (String pattern : mappings.get(httpHandler)) {
					urlMap.put(pattern, httpHandler);
				}
			}
		}
		SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
		hm.setUrlMap(urlMap);
		hm.setOrder(this.order);
		if (this.urlPathHelper != null) {
			hm.setUrlPathHelper(this.urlPathHelper);
		}
		return hm;
	}

}