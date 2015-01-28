/**
 * Copyright 2002-2015 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class WampSession {

	private final static String PREFIXES = WampSession.class.getName() + ".PREFIXES";

	private final static String AUTH_KEY = WampSession.class.getName() + ".AUTH_KEY";

	private final static String CHALLENGE = WampSession.class.getName() + ".CHALLENGE";

	private final static String SIGNATURE = WampSession.class.getName() + ".SIGNATURE";

	/** Key for the mutex session attribute */
	public static final String SESSION_MUTEX_NAME = WampSession.class.getName()
			+ ".MUTEX";

	/** Key set after the session is completed */
	public static final String SESSION_COMPLETED_NAME = WampSession.class.getName()
			+ ".COMPLETED";

	/** Prefix for the name of session attributes used to store destruction callbacks. */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX = WampSession.class
			.getName() + ".DESTRUCTION_CALLBACK.";

	private final WebSocketSession webSocketSession;

	public WampSession(WebSocketSession webSocketSession) {
		this.webSocketSession = webSocketSession;
	}

	/**
	 * Return the value for the attribute of the given name, if any.
	 * @param name the name of the attribute
	 * @return the current attribute value, or {@code null} if not found
	 */
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String name) {
		return (T) this.webSocketSession.getAttributes().get(name);
	}

	public WebSocketSession getWebSocketSession() {
		return this.webSocketSession;
	}

	/**
	 * Set the value with the given name replacing an existing value (if any).
	 * @param name the name of the attribute
	 * @param value the value for the attribute
	 */
	public void setAttribute(String name, Object value) {
		this.webSocketSession.getAttributes().put(name, value);
	}

	/**
	 * Remove the attribute of the given name, if it exists.
	 * <p>
	 * Also removes the registered destruction callback for the specified attribute, if
	 * any. However it <i>does not</i> execute</i> the callback. It is assumed the removed
	 * object will continue to be used and destroyed independently at the appropriate
	 * time.
	 * @param name the name of the attribute
	 */
	public void removeAttribute(String name) {
		this.webSocketSession.getAttributes().remove(name);
		removeDestructionCallback(name);
	}

	/**
	 * Retrieve the names of all attributes.
	 * @return the attribute names as String array, never {@code null}
	 */
	public String[] getAttributeNames() {
		return StringUtils.toStringArray(this.webSocketSession.getAttributes().keySet());
	}

	/**
	 * Register a callback to execute on destruction of the specified attribute. The
	 * callback is executed when the session is closed.
	 * @param name the name of the attribute to register the callback for
	 * @param callback the destruction callback to be executed
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		synchronized (getSessionMutex()) {
			if (isSessionCompleted()) {
				throw new IllegalStateException("Session id=" + getWebSocketSessionId()
						+ " already completed");
			}
			setAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name, callback);
		}
	}

	private void removeDestructionCallback(String name) {
		synchronized (getSessionMutex()) {
			this.webSocketSession.getAttributes().remove(
					DESTRUCTION_CALLBACK_NAME_PREFIX + name);
		}
	}

	/**
	 * Return an id for the associated WebSocket session.
	 * @return the session id as String (never {@code null})
	 */
	public String getWebSocketSessionId() {
		return this.webSocketSession.getId();
	}

	/**
	 * Expose the object to synchronize on for the underlying session.
	 * @return the session mutex to use (never {@code null})
	 */
	public Object getSessionMutex() {
		Object mutex = getAttribute(SESSION_MUTEX_NAME);
		if (mutex == null) {
			mutex = this.webSocketSession.getAttributes();
		}
		return mutex;
	}

	/**
	 * Whether the {@link #sessionCompleted()} was already invoked.
	 */
	public boolean isSessionCompleted() {
		return getAttribute(SESSION_COMPLETED_NAME) != null;
	}

	/**
	 * Invoked when the session is completed. Executed completion callbacks.
	 */
	public void sessionCompleted() {
		synchronized (getSessionMutex()) {
			if (!isSessionCompleted()) {
				executeDestructionCallbacks();
				setAttribute(SESSION_COMPLETED_NAME, Boolean.TRUE);
			}
		}
	}

	private void executeDestructionCallbacks() {
		for (Map.Entry<String, Object> entry : this.webSocketSession.getAttributes()
				.entrySet()) {
			if (entry.getKey().startsWith(DESTRUCTION_CALLBACK_NAME_PREFIX)) {
				try {
					((Runnable) entry.getValue()).run();
				}
				catch (Throwable ex) {
					LogFactory.getLog(getClass()).error(
							"Uncaught error in session attribute destruction callback",
							ex);
				}
			}
		}
	}

	public boolean isAuthRequested() {
		return getAuthKey() != null;
	}

	public boolean isAuthenticated() {
		return getSignature() != null;
	}

	public String getAuthKey() {
		return getAttribute(AUTH_KEY);
	}

	public void setAuthKey(String authKey) {
		setAttribute(AUTH_KEY, authKey);
	}

	public String getChallenge() {
		return getAttribute(CHALLENGE);
	}

	public void setChallenge(String challenge) {
		setAttribute(CHALLENGE, challenge);
	}

	public String getSignature() {
		return getAttribute(SIGNATURE);
	}

	public void setSignature(String signature) {
		setAttribute(SIGNATURE, signature);
	}

	public void addPrefix(String prefix, String uri) {
		Map<String, String> prefixes = getPrefixes();
		if (prefixes == null) {
			prefixes = new HashMap<>();
			setAttribute(PREFIXES, prefixes);
		}
		prefixes.put(prefix, uri);
	}

	public boolean hasPrefixes() {
		Map<String, String> prefixes = getPrefixes();
		return prefixes == null || prefixes.isEmpty();
	}

	public String getPrefix(String curie) {
		Map<String, String> prefixes = getPrefixes();
		if (prefixes != null) {
			return prefixes.get(curie);
		}
		return null;
	}

	public Map<String, String> getPrefixes() {
		return getAttribute(PREFIXES);
	}

}
