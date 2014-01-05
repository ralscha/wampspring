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
package ch.rasc.wampspring.pubsub;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WelcomeMessage;
import ch.rasc.wampspring.support.AbstractTestWebSocketHandler;

public abstract class PubSubClientWebSocketHandler extends AbstractTestWebSocketHandler {

	PubSubClientWebSocketHandler() {
		this(2);
	}

	private PubSubClientWebSocketHandler(int countDownLatchCount) {
		countDownLatch = new CountDownLatch(countDownLatchCount);
	}

	private final CountDownLatch countDownLatch;

	@Override
	public void afterConnectionEstablished(WebSocketSession ws) throws Exception {
		ws.sendMessage(new TextMessage(createSubscribeMessage().toJson(jsonFactory)));
		PublishMessage publishMessage = createPublishMessage();
		if (publishMessage != null) {
			ws.sendMessage(new TextMessage(publishMessage.toJson(jsonFactory)));
		}
	}

	public abstract SubscribeMessage createSubscribeMessage();

	PublishMessage createPublishMessage() {
		return null;
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		System.out.println(message.getPayload());
		WampMessage wampMessage = WampMessage.fromJson(jsonFactory, message.getPayload());

		if (wampMessage instanceof WelcomeMessage) {
			WelcomeMessage wm = (WelcomeMessage) wampMessage;
			assertThat(wm.getProtocolVersion()).isEqualTo(1);
			assertThat(wm.getServerIdent()).isEqualTo("wampspring/1.0");

			countDownLatch.countDown();
		} else if (wampMessage instanceof EventMessage) {
			assertEventResult((EventMessage) wampMessage);
			countDownLatch.countDown();
		}

	}

	@SuppressWarnings("unused")
	public void assertEventResult(EventMessage crm) {
		// nothing here
	}

	@Override
	public void waitForConversationEnd() throws InterruptedException {
		assertThat(countDownLatch.await(15, TimeUnit.SECONDS)).isTrue();
	}

}
