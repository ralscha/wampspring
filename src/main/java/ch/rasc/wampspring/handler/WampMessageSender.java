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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import ch.rasc.wampspring.message.WampMessage;

import com.fasterxml.jackson.core.JsonFactory;

/**
 * Internal class that is responsible for sending {@link WampMessage}s back to the client.
 */
public class WampMessageSender {

	private final Log logger = LogFactory.getLog(getClass());

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	private final Executor outboundExecutor;

	private final JsonFactory jsonFactory;

	public WampMessageSender(Executor outboundExecutor, JsonFactory jsonFactory) {
		this.outboundExecutor = outboundExecutor;
		this.jsonFactory = jsonFactory;
	}

	void put(String sessionId, WebSocketSession session) {
		sessions.put(sessionId, session);
	}

	void remove(String sessionId) {
		sessions.remove(sessionId);
	}

	void sendMessageToClient(String sessionId, WampMessage message) {
		sendMessageToClient(Collections.singleton(sessionId), message);
	}

	void sendMessageToClient(final Set<String> sessionIds, WampMessage message) {

		if (sessionIds == null || sessionIds.isEmpty()) {
			return;
		}

		String json = null;
		try {
			json = message.toJson(jsonFactory);
		} catch (IOException e) {
			logger.error("Conversion of message " + message + " into json failed", e);
		}

		if (this.outboundExecutor == null) {
			for (String sessionId : sessionIds) {
				sendMessage(sessionId, json);
			}
		} else {
			final String textMessage = json;
			this.outboundExecutor.execute(new Runnable() {
				@Override
				public void run() {

					for (String sessionId : sessionIds) {
						sendMessage(sessionId, textMessage);
					}

				}
			});
		}
	}

	private void sendMessage(String sessionId, String json) {

		final WebSocketSession session = sessions.get(sessionId);
		if (session == null) {
			logger.error("Session not found for sessionId " + sessionId);
			return;
		}

		try {
			if (session.isOpen()) {
				session.sendMessage(new TextMessage(json));
			} else {
				sessions.remove(session.getId());
			}
		} catch (IOException e) {
			logger.error("Sending of message '" + json + "' failed", e);
		}
	}
}
