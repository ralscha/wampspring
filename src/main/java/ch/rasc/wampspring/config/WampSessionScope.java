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

package ch.rasc.wampspring.config;

import java.io.Serializable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.web.socket.WebSocketSession;

/**
 * A {@link Scope} implementation exposing the attributes of a WAMP session (e.g.
 * WebSocket session).
 *
 * <p>
 * Relies on a thread-bound {@link WampSession} instance
 *
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class WampSessionScope implements Scope {

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		WampSession wampSession = WampSessionContextHolder.currentAttributes();
		Object value = wampSession.getAttribute(name);
		if (value != null) {
			return value;
		}
		synchronized (wampSession.getSessionMutex()) {
			value = wampSession.getAttribute(name);
			if (value == null) {
				value = objectFactory.getObject();
				wampSession.setAttribute(name, value);
			}
			return value;
		}
	}

	@Override
	public Object remove(String name) {
		WampSession wampSession = WampSessionContextHolder.currentAttributes();
		synchronized (wampSession.getSessionMutex()) {
			Object value = wampSession.getAttribute(name);
			if (value != null) {
				wampSession.removeAttribute(name);
				return value;
			}
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		WampSessionContextHolder.currentAttributes().registerDestructionCallback(name,
				callback);
	}

	@Override
	public Object resolveContextualObject(String key) {
		System.out.println("Resolve Contextual Object: " + key);
		if ("wampsession".equals(key)) {
			WampSessionContextHolder.currentAttributes().getWebSocketSession();
		}

		return null;
	}

	@Override
	public String getConversationId() {
		return WampSessionContextHolder.currentAttributes().getWebSocketSessionId();
	}

	@SuppressWarnings("serial")
	public static class WampSessionObjectFactory implements ObjectFactory<WampSession>,
			Serializable {

		@Override
		public WampSession getObject() {
			return WampSessionContextHolder.currentAttributes();
		}

		@Override
		public String toString() {
			return "Current WampSession";
		}
	}

	@SuppressWarnings("serial")
	public static class WebSocketSessionObjectFactory implements
			ObjectFactory<WebSocketSession>, Serializable {

		@Override
		public WebSocketSession getObject() {
			return WampSessionContextHolder.currentAttributes().getWebSocketSession();
		}

		@Override
		public String toString() {
			return "Current WebSocketSession";
		}
	}

}
