/**
 * Copyright 2014-2017 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.stereotype.Service;

import ch.rasc.wampspring.EventMessenger;
import ch.rasc.wampspring.annotation.WampPublishListener;
import ch.rasc.wampspring.annotation.WampSubscribeListener;
import ch.rasc.wampspring.annotation.WampUnsubscribeListener;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;

@Service
public class DirectService {

	private final Map<String, Integer> wsToId = new ConcurrentHashMap<>();

	private final EventMessenger eventMessenger;

	@Autowired
	public DirectService(EventMessenger eventMessenger) {
		this.eventMessenger = eventMessenger;
	}

	@WampSubscribeListener("/topic/{id}")
	public void subscribe(SubscribeMessage subscribeMessage,
			@DestinationVariable("id") Integer clientId) {
		this.eventMessenger.sendToDirect("/topic", "join:" + clientId,
				this.wsToId.keySet());
		this.eventMessenger.sendTo("/topic", "IGNORED join:" + clientId,
				this.wsToId.keySet());

		this.wsToId.put(subscribeMessage.getWebSocketSessionId(), clientId);
	}

	@WampUnsubscribeListener("/topic")
	public void unsubscribe(UnsubscribeMessage unsubscribeMessage) {
		String wsId = unsubscribeMessage.getWebSocketSessionId();
		Integer clientId = this.wsToId.remove(wsId);
		if (clientId != null) {
			this.eventMessenger.sendToDirect("/topic", "leave:" + clientId,
					this.wsToId.keySet());
			this.eventMessenger.sendTo("/topic", "IGNORED leave:" + clientId,
					this.wsToId.keySet());
		}
	}

	@WampPublishListener("/topic")
	public void publish(PublishMessage publishMessage) {
		String wsId = publishMessage.getWebSocketSessionId();
		Integer clientId = this.wsToId.get(wsId);
		if (clientId != null) {
			this.eventMessenger.sendToDirect("/topic",
					"publish:" + publishMessage.getEvent() + ":" + clientId,
					this.wsToId.keySet());
			this.eventMessenger.sendTo("/topic",
					"IGNORED publish:" + publishMessage.getEvent() + ":" + clientId,
					this.wsToId.keySet());
		}
	}

}
