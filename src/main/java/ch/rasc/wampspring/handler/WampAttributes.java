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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import ch.rasc.wampspring.message.WampMessage;

/**
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class WampAttributes {

	/** Key for the mutex session attribute */
	public static final String SESSION_MUTEX_NAME = WampAttributes.class.getName()
			+ ".MUTEX";

	/** Key set after the session is completed */
	public static final String SESSION_COMPLETED_NAME = WampAttributes.class.getName()
			+ ".COMPLETED";

	/** Prefix for the name of session attributes used to store destruction callbacks. */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX = WampAttributes.class
			.getName() + ".DESTRUCTION_CALLBACK.";

	private static final Log logger = LogFactory.getLog(WampAttributes.class);

	private final String sessionId;

	private final Map<String, Object> attributes;

	/**
	 * Constructor wrapping the given session attributes map.
	 * @param sessionId the id of the associated session
	 * @param attributes the attributes
	 */
	public WampAttributes(String sessionId, Map<String, Object> attributes) {
		Assert.notNull(sessionId, "'sessionId' is required");
		Assert.notNull(attributes, "'attributes' is required");
		this.sessionId = sessionId;
		this.attributes = attributes;
	}

	/**
	 * Return the value for the attribute of the given name, if any.
	 * @param name the name of the attribute
	 * @return the current attribute value, or {@code null} if not found
	 */
	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	/**
	 * Set the value with the given name replacing an existing value (if any).
	 * @param name the name of the attribute
	 * @param value the value for the attribute
	 */
	public void setAttribute(String name, Object value) {
		this.attributes.put(name, value);
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
		this.attributes.remove(name);
		removeDestructionCallback(name);
	}

	/**
	 * Retrieve the names of all attributes.
	 * @return the attribute names as String array, never {@code null}
	 */
	public String[] getAttributeNames() {
		return StringUtils.toStringArray(this.attributes.keySet());
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
				throw new IllegalStateException("Session id=" + getSessionId()
						+ " already completed");
			}
			this.attributes.put(DESTRUCTION_CALLBACK_NAME_PREFIX + name, callback);
		}
	}

	private void removeDestructionCallback(String name) {
		synchronized (getSessionMutex()) {
			this.attributes.remove(DESTRUCTION_CALLBACK_NAME_PREFIX + name);
		}
	}

	/**
	 * Return an id for the associated session.
	 * @return the session id as String (never {@code null})
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Expose the object to synchronize on for the underlying session.
	 * @return the session mutex to use (never {@code null})
	 */
	public Object getSessionMutex() {
		Object mutex = this.attributes.get(SESSION_MUTEX_NAME);
		if (mutex == null) {
			mutex = this.attributes;
		}
		return mutex;
	}

	/**
	 * Whether the {@link #sessionCompleted()} was already invoked.
	 */
	public boolean isSessionCompleted() {
		return this.attributes.get(SESSION_COMPLETED_NAME) != null;
	}

	/**
	 * Invoked when the session is completed. Executed completion callbacks.
	 */
	public void sessionCompleted() {
		synchronized (getSessionMutex()) {
			if (!isSessionCompleted()) {
				executeDestructionCallbacks();
				this.attributes.put(SESSION_COMPLETED_NAME, Boolean.TRUE);
			}
		}
	}

	private void executeDestructionCallbacks() {
		for (Map.Entry<String, Object> entry : this.attributes.entrySet()) {
			if (entry.getKey().startsWith(DESTRUCTION_CALLBACK_NAME_PREFIX)) {
				try {
					((Runnable) entry.getValue()).run();
				}
				catch (Throwable ex) {
					logger.error(
							"Uncaught error in session attribute destruction callback",
							ex);
				}
			}
		}
	}

	/**
	 * Extract the WAMP session attributes from the given message and wrap them in a
	 * {@link WampAttributes} instance.
	 * @param message the message to extract session attributes from
	 */
	public static WampAttributes fromMessage(WampMessage message) {
		Assert.notNull(message, "Message must not be null");

		String sessionId = message.getSessionId();
		Map<String, Object> sessionAttributes = message.getSessionAttributes();
		if (sessionId != null && sessionAttributes != null) {
			return new WampAttributes(sessionId, sessionAttributes);
		}
		return null;
	}

}