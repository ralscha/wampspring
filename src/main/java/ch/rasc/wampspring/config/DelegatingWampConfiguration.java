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

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import ch.rasc.wampspring.EventMessenger;
import ch.rasc.wampspring.cra.AuthenticationHandler;
import ch.rasc.wampspring.cra.AuthenticationSecretProvider;
import ch.rasc.wampspring.handler.AnnotationMethodHandler;
import ch.rasc.wampspring.handler.PubSubHandler;
import ch.rasc.wampspring.handler.WampMessageSender;
import ch.rasc.wampspring.handler.WampWebsocketHandler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * A configuration class that is imported by the {@link EnableWamp} annotation.
 * It detects any implementation of the {@link WampConfigurer} interface and
 * configures the WAMP support accordingly.
 */
@Configuration
class DelegatingWampConfiguration implements WebSocketConfigurer {

	private WampConfigurer configurer;

	private JsonFactory jsonFactory;

	@Autowired(required = false)
	void setConfigurers(Collection<WampConfigurer> configurers) {
		if (CollectionUtils.isEmpty(configurers)) {
			return;
		}

		if (configurers.size() > 1) {
			throw new IllegalStateException("Only one WampConfigurer may exist");
		} else if (configurers.size() == 1) {
			configurer = configurers.iterator().next();
		}

		jsonFactory = new MappingJsonFactory(configurer.objectMapper());
	}

	@PostConstruct
	void postConstruct() {
		if (configurer == null) {
			configurer = new WampConfigurerAdapter();
			jsonFactory = new MappingJsonFactory(configurer.objectMapper());
		}
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		String wampEndpointPath = configurer.wampEndpointPath();
		String path;
		if (wampEndpointPath != null) {
			if (!wampEndpointPath.startsWith("/")) {
				throw new IllegalArgumentException("wampEndpointPath must start with an /");
			}
			path = wampEndpointPath;
		} else {
			path = "/wamp";
		}

		WebSocketHandlerRegistration reg = registry.addHandler(wampWebsocketHandler(), path);
		configurer.configureWampWebsocketHandler(reg);
	}

	@Bean
	public EventMessenger eventMessenger() {
		return new EventMessenger(pubSubHandler());
	}

	@Bean
	public WampMessageSender wampMessageSender() {
		return new WampMessageSender(configurer.outboundExecutor(), jsonFactory);
	}

	@Bean
	public PubSubHandler pubSubHandler() {
		return new PubSubHandler(wampMessageSender());
	}

	@Bean
	public AnnotationMethodHandler annotationMethodHandler() {
		return new AnnotationMethodHandler(wampMessageSender(), pubSubHandler(), configurer.objectMapper(),
				configurer.conversionService());
	}

	@Bean
	public WampWebsocketHandler wampWebsocketHandler() {
		return new WampWebsocketHandler(annotationMethodHandler(), pubSubHandler(), wampMessageSender(), jsonFactory);
	}

	@Bean
	public AuthenticationSecretProvider authenticationSecretProvider() {
		return configurer.authenticationSecretProvider();
	}

	@Bean
	public AuthenticationHandler authenticationHandler() {
		return configurer.authenticationHandler();
	}

}
