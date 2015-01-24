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
package ch.rasc.wampspring.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.messaging.access.expression.MessageExpressionVoter;
import org.springframework.security.messaging.access.intercept.ChannelSecurityInterceptor;
import org.springframework.security.messaging.access.intercept.MessageSecurityMetadataSource;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;

import ch.rasc.wampspring.config.WampConfigurerAdapter;
import ch.rasc.wampspring.message.WampMessageHeader;

/**
 * Allows configuring WAMP messages authorization.
 *
 * <p>
 * For example:
 * </p>
 *
 * <pre>
 * &#064;Configuration
 * public class WampSecurityConfigurer extends AbstractSecurityWampConfigurer {
 * 
 * 	&#064;Override
 * 	protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
 * 		messages.wampDestPublishMatchers(&quot;/user/queue/errors&quot;).permitAll().wampDestSubscribeMatchers(&quot;/admin/**&quot;)
 * 				.hasRole(&quot;ADMIN&quot;).anyMessage().authenticated();
 * 	}
 * }
 * </pre>
 *
 * @author Rob Winch
 * @author Ralph Schaer
 */
public abstract class AbstractSecurityWampConfigurer extends WampConfigurerAdapter {

	private final WampMessageSecurityMetadataSourceRegistry inboundRegistry = new WampMessageSecurityMetadataSourceRegistry();

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
	}

	@Override
	public void configureClientInboundChannel(AbstractMessageChannel channel) {
		ChannelSecurityInterceptor inboundChannelSecurity = inboundChannelSecurity();
		if (this.inboundRegistry.containsMapping()) {
			channel.addInterceptor(securityContextChannelInterceptor());
			channel.addInterceptor(inboundChannelSecurity);
		}
	}

	@Bean
	public ChannelSecurityInterceptor inboundChannelSecurity() {
		ChannelSecurityInterceptor channelSecurityInterceptor = new ChannelSecurityInterceptor(
				inboundMessageSecurityMetadataSource());
		List<AccessDecisionVoter<? extends Object>> voters = new ArrayList<>();
		voters.add(new MessageExpressionVoter<>());
		AffirmativeBased manager = new AffirmativeBased(voters);
		channelSecurityInterceptor.setAccessDecisionManager(manager);
		return channelSecurityInterceptor;
	}

	@Bean
	public SecurityContextChannelInterceptor securityContextChannelInterceptor() {
		return new SecurityContextChannelInterceptor(WampMessageHeader.PRINCIPAL.name());
	}

	@Bean
	public MessageSecurityMetadataSource inboundMessageSecurityMetadataSource() {
		configureInbound(this.inboundRegistry);
		return this.inboundRegistry.createMetadataSource();
	}

	protected void configureInbound(
			@SuppressWarnings("unused") WampMessageSecurityMetadataSourceRegistry messages) {
		// by default nothing here
	}

}
