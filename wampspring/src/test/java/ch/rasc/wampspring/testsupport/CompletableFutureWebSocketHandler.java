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
package ch.rasc.wampspring.testsupport;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WelcomeMessage;

import com.fasterxml.jackson.core.JsonFactory;

public class CompletableFutureWebSocketHandler extends AbstractWebSocketHandler {

	private final CompletableFuture<WelcomeMessage> welcomeMessageFuture;
	private CompletableFuture<WampMessage> messageFuture;

	private final JsonFactory jsonFactory;

	public CompletableFutureWebSocketHandler(JsonFactory jsonFactory) {
		this.jsonFactory = jsonFactory;

		this.welcomeMessageFuture = new CompletableFuture<>();
		this.reset();
	}

	public void reset() {
		this.messageFuture = new CompletableFuture<>();
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {

		try {
			WampMessage wampMessage = WampMessage.fromJson(this.jsonFactory,
					message.getPayload());

			if (wampMessage instanceof WelcomeMessage) {
				this.welcomeMessageFuture.complete((WelcomeMessage) wampMessage);
			}
			else {
				this.messageFuture.complete(wampMessage);
			}

		}
		catch (IOException e) {
			this.welcomeMessageFuture.completeExceptionally(e);
			this.messageFuture.completeExceptionally(e);
		}
	}

	public WampMessage getWampMessage() throws InterruptedException, ExecutionException,
			TimeoutException {
		return this.messageFuture.get(2, TimeUnit.SECONDS);
	}

	public WelcomeMessage getWelcomeMessage() throws InterruptedException,
			ExecutionException, TimeoutException {
		return this.welcomeMessageFuture.get(2, TimeUnit.SECONDS);
	}
}
