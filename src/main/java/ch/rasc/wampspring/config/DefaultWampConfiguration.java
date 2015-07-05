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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.rasc.wampspring.EventMessenger;
import ch.rasc.wampspring.annotation.WampCallListener;
import ch.rasc.wampspring.annotation.WampPublishListener;
import ch.rasc.wampspring.annotation.WampSubscribeListener;
import ch.rasc.wampspring.annotation.WampUnsubscribeListener;
import ch.rasc.wampspring.broker.DefaultSubscriptionRegistry;
import ch.rasc.wampspring.broker.SimpleBrokerMessageHandler;
import ch.rasc.wampspring.broker.SubscriptionRegistry;
import ch.rasc.wampspring.cra.AuthenticationHandler;
import ch.rasc.wampspring.cra.AuthenticationSecretProvider;
import ch.rasc.wampspring.cra.DefaultAuthenticationHandler;
import ch.rasc.wampspring.method.MethodParameterConverter;
import ch.rasc.wampspring.method.WampAnnotationMethodMessageHandler;

/**
 * To enable WAMP support create a @Configuration class that extends this class. Or add
 * {@link EnableWamp} to any @Configuration class.
 * <p>
 * If you overwrite a method annotated with @Bean, don't forget to add the
 * annotation @Bean to the overridden method as well.
 */
@Configuration
public class DefaultWampConfiguration {

	private final List<WampConfigurer> configurers = new ArrayList<>();

	private ObjectMapper objectMapper = null;

	protected ObjectMapper internalObjectMapper = null;

	protected PathMatcher internalPathMatcher = null;

	protected ConversionService internalConversionService = null;

	@Autowired(required = false)
	public void setConfigurers(List<WampConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addAll(configurers);
		}
	}

	@Autowired(required = false)
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Register WAMP endpoints mapping each to a specific URL and (optionally) enabling
	 * and configuring SockJS fallback options.
	 */
	protected void registerWampEndpoints(WampEndpointRegistry registry) {
		if (this.configurers.isEmpty()) {
			registry.addEndpoint("/wamp");
		}
		else {
			for (WampConfigurer wc : this.configurers) {
				wc.registerWampEndpoints(registry);
			}
		}
	}

	/**
	 * Channel for inbound messages between {@link WampSubProtocolHandler},
	 * {@link #brokerMessageHandler()} and {@link #annotationMethodMessageHandler()}
	 */
	@Bean
	public SubscribableChannel clientInboundChannel() {
		ExecutorSubscribableChannel executorSubscribableChannel = new ExecutorSubscribableChannel(
				clientInboundChannelExecutor());
		configureClientInboundChannel(executorSubscribableChannel);
		return executorSubscribableChannel;
	}

	protected void configureClientInboundChannel(AbstractMessageChannel channel) {
		for (WampConfigurer wc : this.configurers) {
			wc.configureClientInboundChannel(channel);
		}
	}

	@Bean
	public Executor clientInboundChannelExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("wampClientInboundChannel-");
		executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
		executor.setMaxPoolSize(Integer.MAX_VALUE);
		executor.setKeepAliveSeconds(60);
		executor.setQueueCapacity(Integer.MAX_VALUE);
		executor.setAllowCoreThreadTimeOut(true);

		return executor;
	}

	/**
	 * Channel for outbound messages sent back to WebSocket clients.
	 */
	@Bean
	public SubscribableChannel clientOutboundChannel() {
		return new ExecutorSubscribableChannel(clientOutboundChannelExecutor());
	}

	@Bean
	public Executor clientOutboundChannelExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("wampClientOutboundChannel-");
		executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
		executor.setMaxPoolSize(Integer.MAX_VALUE);
		executor.setKeepAliveSeconds(60);
		executor.setQueueCapacity(Integer.MAX_VALUE);
		executor.setAllowCoreThreadTimeOut(true);

		return executor;
	}

	/**
	 * Channel from the application to the {@link #brokerMessageHandler()}
	 */
	@Bean
	public SubscribableChannel brokerChannel() {
		return new ExecutorSubscribableChannel(brokerChannelExecutor());
	}

	/**
	 * Executor used by the {@link #brokerChannel()}. By default messages send through the
	 * brokerChannel are processed synchronously.
	 */
	public Executor brokerChannelExecutor() {
		return null;
	}

	@Bean
	public MessageHandler brokerMessageHandler() {
		SimpleBrokerMessageHandler messageHandler = new SimpleBrokerMessageHandler(
				clientInboundChannel(), clientOutboundChannel(), brokerChannel(),
				subscriptionRegistry(), brokerMessageHandlerMessageSelector());

		messageHandler.setAuthenticationRequiredGlobal(authenticationRequired());

		return messageHandler;
	}

	@Bean
	public SubscriptionRegistry subscriptionRegistry() {
		return new DefaultSubscriptionRegistry(pathMatcher());
	}

	protected WampMessageSelector brokerMessageHandlerMessageSelector() {
		return WampMessageSelectors.ACCEPT_ALL;
	}

	@Bean
	public MessageHandler annotationMethodMessageHandler(
			ConfigurableApplicationContext configurableApplicationContext) {

		AuthenticationHandler authenticationHandler = authenticationHandler();
		if (authenticationHandler != null) {
			configurableApplicationContext.getBeanFactory()
					.registerSingleton("authenticationHandler", authenticationHandler);
		}

		WampAnnotationMethodMessageHandler messageHandler = new WampAnnotationMethodMessageHandler(
				clientInboundChannel(), clientOutboundChannel(), eventMessenger(),
				conversionService(), methodParameterConverter(), pathMatcher(),
				methodMessageHandlerMessageSelector());

		messageHandler.setAuthenticationRequiredGlobal(authenticationRequired());

		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();
		addArgumentResolvers(argumentResolvers);
		messageHandler.setCustomArgumentResolvers(argumentResolvers);

		return messageHandler;
	}

	protected MethodParameterConverter methodParameterConverter() {
		return new MethodParameterConverter(lookupObjectMapper(), conversionService());
	}

	protected WampMessageSelector methodMessageHandlerMessageSelector() {
		return WampMessageSelectors.ACCEPT_ALL;
	}

	public void addArgumentResolvers(
			List<HandlerMethodArgumentResolver> argumentResolvers) {
		for (WampConfigurer wc : this.configurers) {
			wc.addArgumentResolvers(argumentResolvers);
		}
	}

	@Bean
	public EventMessenger eventMessenger() {
		return new EventMessenger(brokerChannel(), clientOutboundChannel());
	}

	/**
	 * Returns an instance of a {@link PathMatcher}. Used by the messageHandlers for
	 * matching the topicURI to a destination
	 */
	protected PathMatcher pathMatcher() {
		if (this.internalPathMatcher == null) {
			this.internalPathMatcher = new AntPathMatcher();
		}
		return this.internalPathMatcher;
	}

	/**
	 * Returns a Jackson's {@link ObjectMapper} instance. This mapper is used for
	 * serializing and deserializing WAMP messages. When this method returns null the
	 * library tries to find an {@link ObjectMapper} bean in the current spring context.
	 * If no bean exists the library creates a new instance of {@link ObjectMapper}.
	 */
	protected ObjectMapper objectMapper() {
		return null;
	}

	private ObjectMapper lookupObjectMapper() {
		if (this.internalObjectMapper == null) {
			this.internalObjectMapper = objectMapper();
		}

		if (this.internalObjectMapper == null) {
			this.internalObjectMapper = this.objectMapper;
		}

		if (this.internalObjectMapper == null) {
			this.internalObjectMapper = new ObjectMapper();
		}

		return this.internalObjectMapper;
	}

	/**
	 * Returns a ConversionService that is used for argument conversion
	 */
	public ConversionService conversionService() {
		if (this.internalConversionService == null) {
			this.internalConversionService = new DefaultFormattingConversionService();
		}
		return this.internalConversionService;
	}

	/**
	 * Configures an implementation of the secret provider interface
	 * {@link AuthenticationSecretProvider} for authentication.
	 */
	public AuthenticationSecretProvider authenticationSecretProvider() {
		return null;
	}

	/**
	 * Configures an implementation of the {@link AuthenticationHandler} interface. When
	 * {@link #authenticationSecretProvider()} returns null this method returns null and
	 * creates no {@link AuthenticationHandler}
	 */
	public AuthenticationHandler authenticationHandler() {
		if (authenticationSecretProvider() != null) {
			return new DefaultAuthenticationHandler(authenticationSecretProvider());
		}
		return null;
	}

	/**
	 * When this method returns true, all calls to a wamp server endpoint (methods
	 * annotated with {@link WampCallListener}, {@link WampPublishListener},
	 * {@link WampSubscribeListener} or {@link WampUnsubscribeListener}) have to be
	 * authenticated.
	 */
	public boolean authenticationRequired() {
		return false;
	}

	private WebSocketTransportRegistration transportRegistration;

	@Bean
	public HandlerMapping wampWebSocketHandlerMapping() {
		WebSocketHandler handler = subProtocolWebSocketHandler();
		handler = decorateWebSocketHandler(handler);

		WebMvcWampEndpointRegistry registry = new WebMvcWampEndpointRegistry(handler,
				getTransportRegistration(), messageBrokerSockJsTaskScheduler(),
				new MappingJsonFactory(lookupObjectMapper()));

		List<HandshakeInterceptor> handshakeInterceptors = new ArrayList<>();
		addHandshakeInterceptors(handshakeInterceptors);
		registry.addHandshakeInterceptors(handshakeInterceptors);

		registerWampEndpoints(registry);

		return registry.getHandlerMapping();
	}

	@Bean
	public WebSocketHandler subProtocolWebSocketHandler() {
		return new SubProtocolWebSocketHandler(clientInboundChannel(),
				clientOutboundChannel());
	}

	protected void addHandshakeInterceptors(
			List<HandshakeInterceptor> handshakeInterceptors) {
		for (WampConfigurer wc : this.configurers) {
			wc.addHandshakeInterceptors(handshakeInterceptors);
		}
	}

	protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
		WebSocketHandler decoratedHandler = handler;
		for (WebSocketHandlerDecoratorFactory factory : getTransportRegistration()
				.getDecoratorFactories()) {
			decoratedHandler = factory.decorate(decoratedHandler);
		}
		return decoratedHandler;
	}

	protected final WebSocketTransportRegistration getTransportRegistration() {
		if (this.transportRegistration == null) {
			this.transportRegistration = new WebSocketTransportRegistration();
			configureWebSocketTransport(this.transportRegistration);
		}
		return this.transportRegistration;
	}

	/**
	 * Configure options related to the processing of messages received from and sent to
	 * WebSocket clients.
	 */
	public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		for (WampConfigurer wc : this.configurers) {
			wc.configureWebSocketTransport(registration);
		}
	}

	/**
	 * The default TaskScheduler to use if none is configured via
	 * {@link SockJsServiceRegistration#setTaskScheduler(org.springframework.scheduling.TaskScheduler)}
	 * , i.e.
	 *
	 * <pre class="code">
	 * &#064;Configuration
	 * public class WampConfig extends DefaultWampConfiguration {
	 *
	 * 	public void registerWampEndpoints(WampEndpointRegistry registry) {
	 * 		registry.addEndpoint(&quot;/wamp&quot;).withSockJS().setTaskScheduler(myScheduler());
	 * 	}
	 *
	 * 	// ...
	 * }
	 * </pre>
	 */
	@Bean
	public ThreadPoolTaskScheduler messageBrokerSockJsTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("WampSockJS-");
		scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
		scheduler.setRemoveOnCancelPolicy(true);
		return scheduler;
	}

	@Bean
	public static CustomScopeConfigurer webSocketScopeConfigurer(
			ConfigurableListableBeanFactory beanFactory) {

		beanFactory.registerResolvableDependency(WebSocketSession.class,
				new WampSessionScope.WebSocketSessionObjectFactory());
		beanFactory.registerResolvableDependency(WampSession.class,
				new WampSessionScope.WampSessionObjectFactory());

		CustomScopeConfigurer configurer = new CustomScopeConfigurer();
		configurer.addScope("wampsession", new WampSessionScope());
		return configurer;
	}

}
