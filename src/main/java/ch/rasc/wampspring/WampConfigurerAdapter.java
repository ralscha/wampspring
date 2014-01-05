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
package ch.rasc.wampspring;

import java.util.concurrent.Executor;

import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Defines the default implementation of the {@link WampConfigurer} interface. A @Configuration
 * class can implement the {@link WampConfigurer} interface or subclass this
 * class.
 */
public class WampConfigurerAdapter implements WampConfigurer {

	@Override
	public Executor outboundExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("WampOutbound-");
		executor.initialize();
		return executor;
	}

	@Override
	public String wampEndpointPath() {
		return "/wamp";
	}

	@Override
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Override
	public ConversionService conversionService() {
		return new DefaultFormattingConversionService();
	}

	@Override
	public void configureWampWebsocketHandler(WebSocketHandlerRegistration reg) {
		// nothing here
	}

}
