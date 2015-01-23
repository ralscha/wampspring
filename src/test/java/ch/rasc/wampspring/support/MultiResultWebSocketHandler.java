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
package ch.rasc.wampspring.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WelcomeMessage;

import com.fasterxml.jackson.core.JsonFactory;

public class MultiResultWebSocketHandler extends AbstractWebSocketHandler {

	private final Deferred<List<WampMessage>, Object, Void> resultDeferred = new DeferredObject<>();

	private final Deferred<String, Void, Void> welcomeDeferred = new DeferredObject<>();

	private final JsonFactory jsonFactory;

	private boolean welcomeReceived = false;

	private final int noOfResults;

	private final List<WampMessage> receivedMessages = new ArrayList<>();

	public MultiResultWebSocketHandler(int noOfResults, JsonFactory jsonFactory) {
		this.jsonFactory = jsonFactory;
		this.noOfResults = noOfResults;
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
		try {
			WampMessage wampMessage = WampMessage.fromJson(this.jsonFactory,
					message.getPayload());
			if (wampMessage instanceof WelcomeMessage) {
				this.welcomeReceived = true;
				this.welcomeDeferred.resolve(((WelcomeMessage) wampMessage)
						.getSessionId());
			}
			else {
				if (this.welcomeReceived) {
					this.receivedMessages.add(wampMessage);
					if (this.receivedMessages.size() == this.noOfResults) {
						this.resultDeferred.resolve(this.receivedMessages);
					}
				}
				else {
					this.resultDeferred.reject(wampMessage);
				}
			}

		}
		catch (Exception e) {
			this.resultDeferred.reject(e);
		}
	}

	Promise<List<WampMessage>, Object, Void> getPromise() {
		return this.resultDeferred.promise();
	}

	public List<WampMessage> getWampMessages() throws InterruptedException {
		Promise<List<WampMessage>, Object, Void> promise = getPromise();

		final AtomicReference<List<WampMessage>> ref = new AtomicReference<>();
		promise.done(new DoneCallback<List<WampMessage>>() {
			@Override
			public void onDone(List<WampMessage> wampMessages) {
				ref.set(wampMessages);
			}
		});

		promise.waitSafely(TimeUnit.SECONDS.toMillis(2));
		return ref.get();
	}

	public String getSessionId() throws InterruptedException {
		Promise<String, Void, Void> promise = this.welcomeDeferred.promise();

		final String[] objectWrapper = new String[1];
		promise.done(new DoneCallback<String>() {
			@Override
			public void onDone(String sessionId) {
				objectWrapper[0] = sessionId;
			}
		});

		promise.waitSafely(TimeUnit.SECONDS.toMillis(2));
		return objectWrapper[0];
	}
}
