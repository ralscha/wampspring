/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.rasc.wampspring.method;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.rasc.wampspring.EventMessenger;
import ch.rasc.wampspring.config.WampMessageSelectors;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;

/**
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class WampAnnotationMethodMessageHandlerTest {

	private WampAnnotationMethodMessageHandler messageHandler;

	@Mock
	private SubscribableChannel clientInboundChannel;

	@Mock
	private MessageChannel clientOutboundChannel;

	@Mock
	private EventMessenger eventMessenger;

	@Captor
	ArgumentCaptor<WampMessage> messageCaptor;

	@Captor
	ArgumentCaptor<EventMessage> eventMessageCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		when(this.clientOutboundChannel.send(any(WampMessage.class))).thenReturn(true);

		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
		MethodParameterConverter paramConverter = new MethodParameterConverter(
				new ObjectMapper(), conversionService);
		this.messageHandler = new WampAnnotationMethodMessageHandler(
				this.clientInboundChannel, this.clientOutboundChannel,
				this.eventMessenger, conversionService, paramConverter,
				new AntPathMatcher(), WampMessageSelectors.ACCEPT_ALL);

		@SuppressWarnings("resource")
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerPrototype("annotatedTestService",
				AnnotatedTestService.class);
		applicationContext.refresh();
		this.messageHandler.setApplicationContext(applicationContext);
		this.messageHandler.afterPropertiesSet();

		this.messageHandler.start();
	}

	@Test
	public void testCall() {
		CallMessage callMessage = new CallMessage("call1", "annotatedTestService.call", 1,
				2);
		this.messageHandler.handleMessage(callMessage);

		verifyZeroInteractions(this.eventMessenger);
		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		WampMessage msg = this.messageCaptor.getAllValues().get(0);

		assertThat(msg).isInstanceOf(CallResultMessage.class);
		CallResultMessage result = (CallResultMessage) msg;

		assertThat(result.getCallID()).isEqualTo("call1");
		assertThat(result.getResult()).isEqualTo(3);
	}

	@Test
	public void testSubscribe() {
		SubscribeMessage subscribeMessage = new SubscribeMessage(
				"annotatedTestService.subscribe");
		this.messageHandler.handleMessage(subscribeMessage);

		verifyZeroInteractions(this.eventMessenger);
		verifyZeroInteractions(this.clientOutboundChannel);
	}

	@Test
	public void testSubscribeReplyTo() {
		SubscribeMessage subscribeMessage = new SubscribeMessage(
				"annotatedTestService.subscribeReplyTo");
		this.messageHandler.handleMessage(subscribeMessage);

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
		verify(this.eventMessenger, times(1)).sendToAll(stringCaptor.capture(),
				objectCaptor.capture());
		verifyZeroInteractions(this.clientOutboundChannel);
		assertThat(stringCaptor.getValue())
				.isEqualTo("annotatedTestService.subscribeReplyTo");
		assertThat(objectCaptor.getValue()).isEqualTo(3);
	}

	@Test
	public void testSubscribeExcludeMe() {
		SubscribeMessage subscribeMessage = new SubscribeMessage(
				"annotatedTestService.subscribeExcludeMe");
		subscribeMessage.setWebSocketSessionId("ws1");
		this.messageHandler.handleMessage(subscribeMessage);

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
		ArgumentCaptor<String> string2Captor = ArgumentCaptor.forClass(String.class);
		verifyZeroInteractions(this.clientOutboundChannel);
		verify(this.eventMessenger, times(1)).sendToAllExcept(stringCaptor.capture(),
				objectCaptor.capture(), string2Captor.capture());
		assertThat(stringCaptor.getValue())
				.isEqualTo("annotatedTestService.subscribeExcludeMe");
		assertThat(objectCaptor.getValue()).isEqualTo(4);
		assertThat(string2Captor.getValue()).isEqualTo("ws1");
	}

	@Test
	public void testSubscribeBroadcastOff() {
		SubscribeMessage subscribeMessage = new SubscribeMessage(
				"annotatedTestService.subscribeBroadcastOff");
		subscribeMessage.setWebSocketSessionId("ws1");
		this.messageHandler.handleMessage(subscribeMessage);

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
		ArgumentCaptor<String> string2Captor = ArgumentCaptor.forClass(String.class);
		verifyZeroInteractions(this.clientOutboundChannel);
		verify(this.eventMessenger, times(1)).sendTo(stringCaptor.capture(),
				objectCaptor.capture(), string2Captor.capture());
		assertThat(stringCaptor.getValue())
				.isEqualTo("annotatedTestService.subscribeBroadcastOff");
		assertThat(objectCaptor.getValue()).isEqualTo(44);
		assertThat(string2Captor.getValue()).isEqualTo("ws1");
	}

	@Test
	public void testSubscribeBroadcastOffAndExcludeMe() {
		SubscribeMessage subscribeMessage = new SubscribeMessage(
				"annotatedTestService.subscribeBroadcastOffAndExcludeMe");
		subscribeMessage.setWebSocketSessionId("ws1");
		this.messageHandler.handleMessage(subscribeMessage);
		verifyZeroInteractions(this.clientOutboundChannel);
		verifyZeroInteractions(this.eventMessenger);
	}

	@Test
	public void testUnsubscribe() {
		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
				"annotatedTestService.unsubscribe");
		this.messageHandler.handleMessage(unsubscribeMessage);

		verifyZeroInteractions(this.eventMessenger);
		verifyZeroInteractions(this.clientOutboundChannel);
	}

	@Test
	public void testUnsubscribeReplyTo() {
		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
				"annotatedTestService.unsubscribeReplyTo");
		this.messageHandler.handleMessage(unsubscribeMessage);

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
		verify(this.eventMessenger, times(1)).sendToAll(stringCaptor.capture(),
				objectCaptor.capture());
		verifyZeroInteractions(this.clientOutboundChannel);
		assertThat(stringCaptor.getValue())
				.isEqualTo("annotatedTestService.unsubscribeReplyTo");
		assertThat(objectCaptor.getValue()).isEqualTo(6);
	}

	@Test
	public void testUnsubscribeExcludeMe() {
		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
				"annotatedTestService.unsubscribeExcludeMe");
		unsubscribeMessage.setWebSocketSessionId("ws1");
		this.messageHandler.handleMessage(unsubscribeMessage);

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
		ArgumentCaptor<String> string2Captor = ArgumentCaptor.forClass(String.class);
		verifyZeroInteractions(this.clientOutboundChannel);
		verify(this.eventMessenger, times(1)).sendToAllExcept(stringCaptor.capture(),
				objectCaptor.capture(), string2Captor.capture());
		assertThat(stringCaptor.getValue())
				.isEqualTo("annotatedTestService.unsubscribeExcludeMe");
		assertThat(objectCaptor.getValue()).isEqualTo(7);
		assertThat(string2Captor.getValue()).isEqualTo("ws1");
	}

	@Test
	public void testUnsubscribeBroadcastOff() {
		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
				"annotatedTestService.unsubscribeBroadcastOff");
		unsubscribeMessage.setWebSocketSessionId("ws1");
		this.messageHandler.handleMessage(unsubscribeMessage);

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
		ArgumentCaptor<String> string2Captor = ArgumentCaptor.forClass(String.class);
		verifyZeroInteractions(this.clientOutboundChannel);
		verify(this.eventMessenger, times(1)).sendTo(stringCaptor.capture(),
				objectCaptor.capture(), string2Captor.capture());
		assertThat(stringCaptor.getValue())
				.isEqualTo("annotatedTestService.unsubscribeBroadcastOff");
		assertThat(objectCaptor.getValue()).isEqualTo(77);
		assertThat(string2Captor.getValue()).isEqualTo("ws1");
	}

	@Test
	public void testUnsubscribeBroadcastOffAndExcludeMe() {
		UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(
				"annotatedTestService.unsubscribeBroadcastOffAndExcludeMe");
		unsubscribeMessage.setWebSocketSessionId("ws1");
		this.messageHandler.handleMessage(unsubscribeMessage);

		verifyZeroInteractions(this.clientOutboundChannel);
		verifyZeroInteractions(this.eventMessenger);
	}

	@Test
	public void testPublish() {
		PublishMessage publishMessage = new PublishMessage("annotatedTestService.publish",
				null);
		this.messageHandler.handleMessage(publishMessage);

		verifyZeroInteractions(this.eventMessenger);
		verifyZeroInteractions(this.clientOutboundChannel);
	}

	@Test
	public void testPublishReplyTo() {
		PublishMessage publishMessage = new PublishMessage(
				"annotatedTestService.publishReplyTo", null);
		this.messageHandler.handleMessage(publishMessage);

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
		verify(this.eventMessenger, times(1)).sendToAll(stringCaptor.capture(),
				objectCaptor.capture());
		verifyZeroInteractions(this.clientOutboundChannel);
		assertThat(stringCaptor.getValue())
				.isEqualTo("annotatedTestService.publishReplyTo");
		assertThat(objectCaptor.getValue()).isEqualTo(9);
	}

	@Test
	public void testPublishExcludeMe() {
		PublishMessage publishMessage = new PublishMessage(
				"annotatedTestService.publishExcludeMe", null);
		publishMessage.setWebSocketSessionId("ws1");
		this.messageHandler.handleMessage(publishMessage);

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
		ArgumentCaptor<String> string2Captor = ArgumentCaptor.forClass(String.class);
		verifyZeroInteractions(this.clientOutboundChannel);
		verify(this.eventMessenger, times(1)).sendToAllExcept(stringCaptor.capture(),
				objectCaptor.capture(), string2Captor.capture());
		assertThat(stringCaptor.getValue())
				.isEqualTo("annotatedTestService.publishExcludeMe");
		assertThat(objectCaptor.getValue()).isEqualTo(10);
		assertThat(string2Captor.getValue()).isEqualTo("ws1");
	}

	@Test
	public void testPublishBroadcastOff() {
		PublishMessage publishMessage = new PublishMessage(
				"annotatedTestService.publishBroadcastOff", null);
		publishMessage.setWebSocketSessionId("ws1");
		this.messageHandler.handleMessage(publishMessage);

		ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
		ArgumentCaptor<String> string2Captor = ArgumentCaptor.forClass(String.class);
		verifyZeroInteractions(this.clientOutboundChannel);
		verify(this.eventMessenger, times(1)).sendTo(stringCaptor.capture(),
				objectCaptor.capture(), string2Captor.capture());
		assertThat(stringCaptor.getValue())
				.isEqualTo("annotatedTestService.publishBroadcastOff");
		assertThat(objectCaptor.getValue()).isEqualTo(100);
		assertThat(string2Captor.getValue()).isEqualTo("ws1");
	}

	@Test
	public void testPublishBroadcastOffAndExcludeMe() {
		PublishMessage publishMessage = new PublishMessage(
				"annotatedTestService.publishBroadcastOffAndExcludeMe", null);
		publishMessage.setWebSocketSessionId("ws1");
		this.messageHandler.handleMessage(publishMessage);
		verifyZeroInteractions(this.clientOutboundChannel);
		verifyZeroInteractions(this.eventMessenger);
	}
}
