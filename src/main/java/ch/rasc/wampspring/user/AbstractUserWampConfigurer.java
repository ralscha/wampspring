/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package ch.rasc.wampspring.user;

import java.security.Principal;

import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.user.DefaultUserSessionRegistry;
import org.springframework.messaging.simp.user.UserSessionRegistry;

import ch.rasc.wampspring.EventMessenger;
import ch.rasc.wampspring.config.AbstractWampConfigurer;
import ch.rasc.wampspring.config.WebSocketTransportRegistration;
import ch.rasc.wampspring.message.EventMessage;

/**
 * Configures a {@link UserSessionRegistry} that maps a {@link Principal}s name to a
 * WebSocket session id. Additionally a {@link UserEventMessenger} is configured as a bean
 * that allows sending {@link EventMessage}s to user names in addition to WebSocket
 * session ids.
 *
 * <p>
 * Example:
 * </p>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWamp
 * public class UserWampConfigurer extends AbstractUserWampConfigurer {
 * 
 * 	&#064;Override
 * 	public void registerWampEndpoints(WampEndpointRegistry registry) {
 * 		registry.addEndpoint(&quot;/wamp&quot;).withSockJS();
 * 	}
 * }
 * </pre>
 *
 * @author Rob Winch
 * @author Ralph Schaer
 */
public abstract class AbstractUserWampConfigurer extends AbstractWampConfigurer {

	@Bean
	public UserEventMessenger userEventMessenger(EventMessenger eventMessenger) {
		return new UserEventMessenger(eventMessenger, userSessionRegistry());
	}

	@Bean
	public UserSessionRegistry userSessionRegistry() {
		return new DefaultUserSessionRegistry();
	}

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		registration.addDecoratorFactory(new UserSessionWebSocketHandlerDecoratorFactory(
				userSessionRegistry()));
	}

}
