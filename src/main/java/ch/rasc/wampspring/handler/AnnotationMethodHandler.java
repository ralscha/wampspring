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
package ch.rasc.wampspring.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;

import ch.rasc.wampspring.annotation.WampCallListener;
import ch.rasc.wampspring.annotation.WampPublishListener;
import ch.rasc.wampspring.annotation.WampSubscribeListener;
import ch.rasc.wampspring.annotation.WampUnsubscribeListener;
import ch.rasc.wampspring.message.CallErrorMessage;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PrefixMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WampMessageHeader;
import ch.rasc.wampspring.support.HandlerMethodArgumentResolver;
import ch.rasc.wampspring.support.HandlerMethodArgumentResolverComposite;
import ch.rasc.wampspring.support.InvocableHandlerMethod;
import ch.rasc.wampspring.support.PrincipalMethodArgumentResolver;
import ch.rasc.wampspring.support.WampMessageMethodArgumentResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Internal class that is responsible for calling methods that are annotated
 * with {@link WampCallListener}, {@link WampPublishListener},
 * {@link WampSubscribeListener} or {@link WampUnsubscribeListener}
 * 
 */
public class AnnotationMethodHandler implements ApplicationContextAware, InitializingBean {

	private final Log logger = LogFactory.getLog(getClass());

	private ApplicationContext applicationContext;

	private final MultiValueMap<String, WampHandlerMethod> publishMethods = new LinkedMultiValueMap<>();

	private final MultiValueMap<String, WampHandlerMethod> subscribeMethods = new LinkedMultiValueMap<>();

	private final MultiValueMap<String, WampHandlerMethod> unsubscribeMethods = new LinkedMultiValueMap<>();

	private final MultiValueMap<String, WampHandlerMethod> callMethods = new LinkedMultiValueMap<>();

	/**
	 * SPEC says: The agreement is per-connection, and has a lifetime starting
	 * with the server receiving a PREFIX message establishing a prefix-to-URI
	 * mapping, and ending with the WebSocket connection.
	 */
	private final Map<String, Map<String, String>> sessionIdsPrefixUri = new HashMap<>();

	private List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<>();

	private final HandlerMethodArgumentResolverComposite argumentResolvers = new HandlerMethodArgumentResolverComposite();

	private final WampMessageSender wampMessageSender;

	private final PubSubHandler pubSubHandler;

	private final ObjectMapper objectMapper;

	private final ConversionService conversionService;
	
	private PathMatcher pathMatcher;

	public AnnotationMethodHandler(WampMessageSender wampMessageSender, PubSubHandler pubSubHandler,
			ObjectMapper objectMapper, ConversionService conversionService, PathMatcher pathMatcher) {
		this.wampMessageSender = wampMessageSender;
		this.pubSubHandler = pubSubHandler;
		this.objectMapper = objectMapper;
		this.conversionService = conversionService;
		this.pathMatcher = pathMatcher;
	}

	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		Assert.notNull(customArgumentResolvers, "The 'customArgumentResolvers' cannot be null.");
		this.customArgumentResolvers = customArgumentResolvers;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		String[] beanNames = this.applicationContext.getBeanNamesForType(Object.class);
		for (String beanName : beanNames) {
			detectHandlerMethods(beanName);
		}

		this.argumentResolvers.addResolver(new WampMessageMethodArgumentResolver());
		this.argumentResolvers.addResolvers(this.customArgumentResolvers);
		this.argumentResolvers.addResolver(new PrincipalMethodArgumentResolver());
	}

	final void detectHandlerMethods(String beanName) {
		Class<?> handlerType = this.applicationContext.getType(beanName);
		handlerType = ClassUtils.getUserClass(handlerType);

		initHandlerMethods(beanName, handlerType, WampCallListener.class, this.callMethods);
		initHandlerMethods(beanName, handlerType, WampPublishListener.class, this.publishMethods);
		initHandlerMethods(beanName, handlerType, WampSubscribeListener.class, this.subscribeMethods);
		initHandlerMethods(beanName, handlerType, WampUnsubscribeListener.class, this.unsubscribeMethods);
	}

	private <A extends Annotation> void initHandlerMethods(String beanName, Class<?> handlerType,
			final Class<A> annotationType, MultiValueMap<String, WampHandlerMethod> handlerMethods) {

		Set<Method> methods = HandlerMethodSelector.selectMethods(handlerType, new MethodFilter() {
			@Override
			public boolean matches(Method method) {
				return AnnotationUtils.findAnnotation(method, annotationType) != null;
			}
		});

		for (Method method : methods) {
			A annotation = AnnotationUtils.findAnnotation(method, annotationType);
			String[] destinations = (String[]) AnnotationUtils.getValue(annotation);

			String[] replyTo = (String[]) AnnotationUtils.getValue(annotation, "replyTo");
			Boolean excludeSender = (Boolean) AnnotationUtils.getValue(annotation, "excludeSender");

			Object bean = applicationContext.getBean(beanName);
			WampHandlerMethod newHandlerMethod = new WampHandlerMethod(bean, method, replyTo, excludeSender);
			if (destinations.length > 0) {
				for (String destination : destinations) {
					handlerMethods.add(destination, newHandlerMethod);
					if (logger.isInfoEnabled()) {
						logger.info("Mapped \"@" + annotationType.getSimpleName() + " " + destination + "\" onto "
								+ newHandlerMethod);
					}
				}
			} else {
				// by default use beanName.methodName as destination
				String destination = beanName + "." + method.getName();
				handlerMethods.add(destination, newHandlerMethod);
				if (logger.isInfoEnabled()) {
					logger.info("Mapped \"@" + annotationType.getSimpleName() + " " + destination + "\" onto "
							+ newHandlerMethod);
				}
			}
		}
	}

	public void handleMessage(WampMessage message) {
		switch (message.getType()) {
		case CALL:
			handleCallMessage((CallMessage) message);
			break;
		case PUBLISH:
			PublishMessage publishMessage = (PublishMessage) message;
			handlePubSubMessage(publishMessage, publishMessage.getEvent(), publishMessage.getTopicURI(), publishMethods);
			break;
		case SUBSCRIBE:
			SubscribeMessage subscribeMessage = (SubscribeMessage) message;
			handlePubSubMessage(subscribeMessage, null, subscribeMessage.getTopicURI(), subscribeMethods);
			break;
		case UNSUBSCRIBE:
			UnsubscribeMessage unsubscribeMessage = (UnsubscribeMessage) message;
			handlePubSubMessage(unsubscribeMessage, null, unsubscribeMessage.getTopicURI(), unsubscribeMethods);
			break;
		case PREFIX:
			PrefixMessage prefixMessage = (PrefixMessage) message;
			handlePrefixMessage(prefixMessage);
			break;
		default:
			break;
		}

	}

	private void handlePrefixMessage(PrefixMessage prefixMessage) {
		String sessionId = prefixMessage.getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID);

		if (sessionIdsPrefixUri.containsKey(sessionId)) {
			Map<String, String> prefixUri = sessionIdsPrefixUri.get(sessionId);
			prefixUri.put(prefixMessage.getPrefix(), prefixMessage.getUri());
		} else {
			Map<String, String> prefixUri = new HashMap<>();
			prefixUri.put(prefixMessage.getPrefix(), prefixMessage.getUri());
			sessionIdsPrefixUri.put(sessionId, prefixUri);
		}
	}

	private void handleCallMessage(CallMessage callMessage) {
		String sessionId = callMessage.getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID);

		List<WampHandlerMethod> matches = getHandlerMethod(callMessage.getProcURI(), callMethods);
		if (matches.isEmpty()) {
			matches = searchIfPrefixSet(callMessage, callMessage.getProcURI(), callMethods);
			if (matches == null) {
				if (logger.isTraceEnabled()) {
					logger.trace("No matching method, destination " + callMessage.getProcURI());
				}
				return;
			}
		}

		for (HandlerMethod match : matches) {
			HandlerMethod handlerMethod = match.createWithResolvedBean();

			InvocableHandlerMethod invocableHandlerMethod = new InvocableHandlerMethod(handlerMethod, objectMapper,
					conversionService);
			invocableHandlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);

			try {
				Object[] arguments = null;
				if (callMessage.getArguments() != null) {
					arguments = callMessage.getArguments().toArray();
				}
				Object returnValue = invocableHandlerMethod.invoke(callMessage, arguments);
				CallResultMessage callResultMessage = new CallResultMessage(callMessage.getCallID(), returnValue);
				wampMessageSender.sendMessageToClient(sessionId, callResultMessage);
			} catch (Exception ex) {
				CallErrorMessage callErrorMessage = new CallErrorMessage(callMessage.getCallID(), "", ex.toString());
				wampMessageSender.sendMessageToClient(sessionId, callErrorMessage);
				logger.error("Error while processing message " + callMessage, ex);
			} catch (Throwable ex) {
				CallErrorMessage callErrorMessage = new CallErrorMessage(callMessage.getCallID(), "", ex.toString());
				wampMessageSender.sendMessageToClient(sessionId, callErrorMessage);
				logger.error("Error while processing message " + callErrorMessage, ex);
			}
		}
	}

	private List<WampHandlerMethod> searchIfPrefixSet(WampMessage message, String destination,
			MultiValueMap<String, WampHandlerMethod> handlerMethods) {
		String sessionId = message.getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID);
		List<WampHandlerMethod> matches = null;
		if (sessionIdsPrefixUri.containsKey(sessionId)) {
			Map<String, String> prefixUri = sessionIdsPrefixUri.get(sessionId);
			String[] curie = destination.split(":");
			// if it is a prefix, we search the original URI
			String prefix = prefixUri.get(curie[0]);
			if (null != prefix && curie.length > 1) {
				// we rebuild the original URI
				String uri = String.format("%s%s", prefix, curie[1]);
				matches = getHandlerMethod(uri, handlerMethods);
				// question is? do we cache it or no to accelerate further use
				// and avoid each call search (perf issue)
				// problem is methods maps are cross session and spec says prefix
				// is per session
				// and multiple session can register same prefix
				// something like following works, but how to track efficiently
				// prefix->method per session
				if (!matches.isEmpty()) {
					for (WampHandlerMethod match : matches) {
						handlerMethods.add(destination, match);
					}
				}
			}
		}
		return matches;
	}
	
	private void handlePubSubMessage(WampMessage message, Object argument, String destination,
			MultiValueMap<String, WampHandlerMethod> handlerMethods) {
		Assert.notNull(destination, "destination is required");

		List<WampHandlerMethod> matches = getHandlerMethod(destination, handlerMethods);
		if (matches.isEmpty()) {
			matches = searchIfPrefixSet(message, destination, handlerMethods);
			if (logger.isTraceEnabled()) {
				logger.trace("No matching method, destination " + destination);
			}
			return;
		}

		for (WampHandlerMethod handlerMethod : matches) {

			InvocableHandlerMethod invocableHandlerMethod = new InvocableHandlerMethod(handlerMethod, objectMapper,
					conversionService);
			invocableHandlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);

			try {
				Object returnValue = invocableHandlerMethod.invoke(message, argument);
				if (returnValue != null) {
					Set<String> mySessionId = Collections.singleton(message
							.<String> getHeader(WampMessageHeader.WEBSOCKET_SESSION_ID));
					for (String replyToTopicURI : handlerMethod.getReplyTo()) {
						if (StringUtils.hasText(replyToTopicURI)) {
							if (handlerMethod.isExcludeSender() != null && handlerMethod.isExcludeSender()) {
								pubSubHandler.sendToAllExcept(new EventMessage(replyToTopicURI, returnValue),
										mySessionId);
							} else {
								pubSubHandler.sendToAll(new EventMessage(replyToTopicURI, returnValue));
							}
						}
					}
				}
			} catch (Throwable ex) {
				logger.error("Error while processing message " + message, ex);
			}
		}
	}

    List<WampHandlerMethod> getHandlerMethod(String destination, MultiValueMap<String, WampHandlerMethod> handlerMethods) {
    	List<WampHandlerMethod> matches = new ArrayList<WampHandlerMethod>();
		for (String mappingDestination : handlerMethods.keySet()) {
			if (destination.equals(mappingDestination)) {
				matches.addAll(handlerMethods.get(mappingDestination));
			} 
			if(pathMatcher.isPattern(mappingDestination)){
				if(pathMatcher.match(mappingDestination, destination))
					matches.addAll(handlerMethods.get(mappingDestination));
			}
		}
        return matches;
	}

	public void unregisterSessionFromAllPrefixCurie(String sessionId) {
		if (sessionIdsPrefixUri.containsKey(sessionId)) {
			Map<String, String> prefixUri = sessionIdsPrefixUri.remove(sessionId);
			// question is: now should we remove the mapping prefix->method
			// other sessions may use this prefix->method
			// we cannot for moment as we don't track prefix->method with
			// session
		}

	}
	
	/**
     * Set the PathMatcher implementation to use for matching destinations
     * against configured destination patterns.
     * <p>By default AntPathMatcher is used
     */
    public void setPathMatcher(PathMatcher pathMatcher) {
        Assert.notNull(pathMatcher, "PathMatcher must not be null");
        this.pathMatcher = pathMatcher;
    }

    /**
     * Return the PathMatcher implementation to use for matching destinations
     */
    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }
    
    /**
     * Set the path separator to use for pattern parsing if {@link AntPathMatcher} is used as {@link PathMatcher} .
     * Default is "/", as in Ant.
     */
    public void setPathSeparator(String pathSeparator) {
        Assert.notNull(pathSeparator, "Path separator must not be null");
        if(this.pathMatcher instanceof AntPathMatcher)
            ((AntPathMatcher) this.pathMatcher).setPathSeparator(pathSeparator);
    }

}
