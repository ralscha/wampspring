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
package ch.rasc.wampspring.handler;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.AntPathMatcher;

import ch.rasc.wampspring.EventMessenger;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.WampMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class WampAnnotationMethodMessageHandlerTests {

	private WampAnnotationMethodMessageHandler messageHandler;

	@Mock
	private SubscribableChannel clientInboundChannel;

	@Mock
	private MessageChannel clientOutboundChannel;

	@Mock
	private SubscribableChannel brokerChannel;

	@Mock
	private EventMessenger eventMessenger;

	@Captor
	ArgumentCaptor<WampMessage> messageCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		when(clientOutboundChannel.send(any(WampMessage.class))).thenReturn(true);

		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
		MethodParameterConverter paramConverter = new MethodParameterConverter(
				new ObjectMapper(), conversionService);
		messageHandler = new WampAnnotationMethodMessageHandler(clientInboundChannel,
				clientOutboundChannel, eventMessenger, conversionService, paramConverter,
				new AntPathMatcher());

		@SuppressWarnings("resource")
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerPrototype("callService", CallService.class);
		// applicationContext.refresh();
		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();

		messageHandler.start();
	}

	@Test
	public void testCall() {
		CallMessage callMessage = new CallMessage("12", "sum/one", 3, 4);
		messageHandler.handleMessage(callMessage);

		callMessage = new CallMessage("13", "sum/two", 2, 1);
		messageHandler.handleMessage(callMessage);

		verify(clientOutboundChannel, times(2)).send(messageCaptor.capture());
		List<WampMessage> results = messageCaptor.getAllValues();

		assertThat(results).hasSize(2);
		assertThat(results.get(0)).isInstanceOf(CallResultMessage.class);
		assertThat(results.get(1)).isInstanceOf(CallResultMessage.class);

		assertThat(((CallResultMessage) results.get(0)).getCallID()).isEqualTo("12");
		assertThat(((CallResultMessage) results.get(1)).getCallID()).isEqualTo("13");
	}

	// @Test
	// public void registeredMappings() {
	//
	// Map<String, HandlerMethod> handlerMethods =
	// this.messageHandler.getHandlerMethods();
	//
	// assertNotNull(handlerMethods);
	// assertThat(handlerMethods.keySet(), Matchers.hasSize(3));
	// }
	//
	// @Test
	// public void antPatchMatchWildcard() throws Exception {
	//
	// Method method =
	// this.testController.getClass().getMethod("handlerPathMatchWildcard");
	// this.messageHandler.registerHandlerMethod(this.testController, method,
	// "/handlerPathMatch*");
	//
	// this.messageHandler.handleMessage(toDestination("/test/handlerPathMatchFoo"));
	//
	// assertEquals("pathMatchWildcard", this.testController.method);
	// }
	//
	// @Test
	// public void bestMatchWildcard() throws Exception {
	//
	// Method method = this.testController.getClass().getMethod("bestMatch");
	// this.messageHandler.registerHandlerMethod(this.testController, method,
	// "/bestmatch/{foo}/path");
	//
	// method = this.testController.getClass().getMethod("secondBestMatch");
	// this.messageHandler.registerHandlerMethod(this.testController, method,
	// "/bestmatch/*/*");
	//
	// this.messageHandler.handleMessage(toDestination("/test/bestmatch/bar/path"));
	//
	// assertEquals("bestMatch", this.testController.method);
	// }
	//
	// @Test
	// public void argumentResolution() {
	//
	// this.messageHandler.handleMessage(toDestination("/test/handlerArgumentResolver"));
	//
	// assertEquals("handlerArgumentResolver", this.testController.method);
	// assertNotNull(this.testController.arguments.get("message"));
	// }
	//
	// @Test
	// public void exceptionHandled() {
	//
	// this.messageHandler.handleMessage(toDestination("/test/handlerThrowsExc"));
	//
	// assertEquals("illegalStateException", this.testController.method);
	// assertNotNull(this.testController.arguments.get("exception"));
	// }
	//
	// private Message<?> toDestination(String destination) {
	// return MessageBuilder.withPayload(new byte[0]).setHeader(DESTINATION_HEADER,
	// destination).build();
	// }
	//
	//
	// @SuppressWarnings("unused")
	// private static class TestController {
	//
	// public String method;
	//
	// private Map<String, Object> arguments = new LinkedHashMap<String, Object>();
	//
	// public void handlerPathMatchWildcard() {
	// this.method = "pathMatchWildcard";
	// }
	//
	// @SuppressWarnings("rawtypes")
	// public void handlerArgumentResolver(Message message) {
	// this.method = "handlerArgumentResolver";
	// this.arguments.put("message", message);
	// }
	//
	// public void handlerThrowsExc() {
	// throw new IllegalStateException();
	// }
	//
	// public void bestMatch() {
	// this.method = "bestMatch";
	// }
	//
	// public void secondBestMatch() {
	// this.method = "secondBestMatch";
	// }
	//
	// public void illegalStateException(IllegalStateException exception) {
	// this.method = "illegalStateException";
	// this.arguments.put("exception", exception);
	// }
	//
	// }
	//
	// @SuppressWarnings("unused")
	// private static class DuplicateMappingsController {
	//
	// public void handlerFoo() { }
	//
	// public void handlerFoo(String arg) { }
	// }

	// private static class TestMethodMessageHandler extends
	// AbstractMethodMessageHandler<String> {
	//
	// private PathMatcher pathMatcher = new AntPathMatcher();
	//
	// public void registerHandler(Object handler) {
	// super.detectHandlerMethods(handler);
	// }
	//
	// public void registerHandlerMethod(Object handler, Method method, String mapping) {
	// super.registerHandlerMethod(handler, method, mapping);
	// }
	//
	// @Override
	// protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
	// List<HandlerMethodArgumentResolver> resolvers = new
	// ArrayList<HandlerMethodArgumentResolver>();
	// resolvers.add(new MessageMethodArgumentResolver());
	// resolvers.addAll(getCustomArgumentResolvers());
	// return resolvers;
	// }
	//
	// @Override
	// protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers()
	// {
	// List<HandlerMethodReturnValueHandler> handlers = new
	// ArrayList<HandlerMethodReturnValueHandler>();
	// handlers.addAll(getCustomReturnValueHandlers());
	// return handlers;
	// }
	//
	// @Override
	// protected boolean isHandler(Class<?> beanType) {
	// return beanType.getName().contains("Controller");
	// }
	//
	// @Override
	// protected String getMappingForMethod(Method method, Class<?> handlerType) {
	// String methodName = method.getName();
	// if (methodName.startsWith("handler")) {
	// return "/" + methodName;
	// }
	// return null;
	// }
	//
	// @Override
	// protected Set<String> getDirectLookupDestinations(String mapping) {
	// Set<String> result = new LinkedHashSet<String>();
	// if (!this.pathMatcher.isPattern(mapping)) {
	// result.add(mapping);
	// }
	// return result;
	// }
	//
	// @Override
	// protected String getDestination(Message<?> message) {
	// return (String) message.getHeaders().get(DESTINATION_HEADER);
	// }
	//
	// @Override
	// protected String getMatchingMapping(String mapping, Message<?> message) {
	//
	// String destination = getLookupDestination(getDestination(message));
	// if (mapping.equals(destination) || this.pathMatcher.match(mapping, destination)) {
	// return mapping;
	// }
	// return null;
	// }
	//
	// @Override
	// protected Comparator<String> getMappingComparator(final Message<?> message) {
	// return new Comparator<String>() {
	// @Override
	// public int compare(String info1, String info2) {
	// DestinationPatternsMessageCondition cond1 = new
	// DestinationPatternsMessageCondition(info1);
	// DestinationPatternsMessageCondition cond2 = new
	// DestinationPatternsMessageCondition(info2);
	// return cond1.compareTo(cond2, message);
	// }
	// };
	// }
	//
	// @Override
	// protected AbstractExceptionHandlerMethodResolver
	// createExceptionHandlerMethodResolverFor(Class<?> beanType) {
	// return new TestExceptionHandlerMethodResolver(beanType);
	// }
	// }
	//
	// private static class TestExceptionHandlerMethodResolver extends
	// AbstractExceptionHandlerMethodResolver {
	//
	// public TestExceptionHandlerMethodResolver(Class<?> handlerType) {
	// super(initExceptionMappings(handlerType));
	// }
	//
	// private static Map<Class<? extends Throwable>, Method>
	// initExceptionMappings(Class<?> handlerType) {
	// Map<Class<? extends Throwable>, Method> result = new HashMap<Class<? extends
	// Throwable>, Method>();
	// for (Method method : HandlerMethodSelector.selectMethods(handlerType,
	// EXCEPTION_HANDLER_METHOD_FILTER)) {
	// for(Class<? extends Throwable> exception :
	// getExceptionsFromMethodSignature(method)) {
	// result.put(exception, method);
	// }
	// }
	// return result;
	// }
	//
	// public final static MethodFilter EXCEPTION_HANDLER_METHOD_FILTER = new
	// MethodFilter() {
	//
	// @Override
	// public boolean matches(Method method) {
	// return method.getName().contains("Exception");
	// }
	// };
	//
	// }

}
