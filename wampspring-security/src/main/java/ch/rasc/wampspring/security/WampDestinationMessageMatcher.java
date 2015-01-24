/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package ch.rasc.wampspring.security;

import org.springframework.messaging.Message;
import org.springframework.security.messaging.util.matcher.MessageMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WampMessageType;

/**
 * <p>
 * MessageMatcher which compares a pre-defined pattern against the destination of a
 * {@link Message}. There is also support for optionally matching on a specified
 * {@link WampMessageType}.
 * </p>
 *
 * @since 4.0
 * @author Rob Winch
 * @author Ralph Schaer
 */
public final class WampDestinationMessageMatcher implements MessageMatcher<Object> {
	public static final MessageMatcher<Object> NULL_DESTINATION_MATCHER = new MessageMatcher<Object>() {
		@Override
		public boolean matches(Message<? extends Object> message) {
			if (message instanceof WampMessage) {
				String destination = ((WampMessage) message).getDestination();
				return destination == null;
			}
			return false;
		}
	};

	private final PathMatcher matcher;

	/**
	 * The {@link MessageMatcher} that determines if the type matches. If the type was
	 * null, this matcher will match every Message.
	 */
	private final MessageMatcher<Object> messageTypeMatcher;
	private final String pattern;

	/**
	 * <p>
	 * Creates a new instance with the specified pattern, null {@link WampMessageType}
	 * (matches any type), and a {@link AntPathMatcher} created from the default
	 * constructor.
	 * </p>
	 *
	 * <p>
	 * The mapping matches destinations despite the using the following rules:
	 *
	 * <ul>
	 * <li>? matches one character</li>
	 * <li>* matches zero or more characters</li>
	 * <li>** matches zero or more 'directories' in a path</li>
	 * </ul>
	 *
	 * <p>
	 * Some examples:
	 *
	 * <ul>
	 * <li>{@code com/t?st.jsp} - matches {@code com/test} but also {@code com/tast} or
	 * {@code com/txst}</li>
	 * <li>{@code com/*suffix} - matches all files ending in {@code suffix} in the
	 * {@code com} directory</li>
	 * <li>{@code com/&#42;&#42;/test} - matches all destinations ending with {@code test}
	 * underneath the {@code com} path</li>
	 * </ul>
	 *
	 * @param pattern the pattern to use
	 */
	public WampDestinationMessageMatcher(String pattern) {
		this(pattern, new AntPathMatcher());
	}

	/**
	 * <p>
	 * Creates a new instance with the specified pattern and {@link PathMatcher}.
	 * </p>
	 *
	 * @param pattern the pattern to use
	 * @param pathMatcher the {@link PathMatcher} to use.
	 */
	public WampDestinationMessageMatcher(String pattern, PathMatcher pathMatcher) {
		this(pattern, null, pathMatcher);
	}

	/**
	 * <p>
	 * Creates a new instance with the specified pattern, {@link WampMessageType}, and
	 * {@link PathMatcher}.
	 * </p>
	 *
	 * @param pattern the pattern to use
	 * @param type the {@link WampMessageType} to match on or null if any
	 * {@link WampMessageType} should be matched.
	 * @param pathMatcher the {@link PathMatcher} to use.
	 */
	public WampDestinationMessageMatcher(String pattern, WampMessageType type,
			PathMatcher pathMatcher) {
		Assert.notNull(pattern, "pattern cannot be null");
		Assert.notNull(pathMatcher, "pathMatcher cannot be null");

		if (!isTypeWithDestination(type)) {
			throw new IllegalArgumentException("WampMessageType " + type
					+ " does not contain a destination and so cannot be matched on.");
		}

		this.matcher = pathMatcher;
		this.messageTypeMatcher = type == null ? ANY_MESSAGE
				: new WampMessageTypeMatcher(type);
		this.pattern = pattern;
	}

	@Override
	public boolean matches(Message<? extends Object> message) {
		if (!this.messageTypeMatcher.matches(message)) {
			return false;
		}

		if (message instanceof WampMessage) {
			String destination = ((WampMessage) message).getDestination();
			return destination != null && this.matcher.match(this.pattern, destination);
		}

		return false;
	}

	@Override
	public String toString() {
		return "WampDestinationMessageMatcher [matcher=" + this.matcher
				+ ", messageTypeMatcher=" + this.messageTypeMatcher + ", pattern="
				+ this.pattern + "]";
	}

	public static boolean isTypeWithDestination(WampMessageType type) {
		if (type == null) {
			return true;
		}
		return type == WampMessageType.CALL || type == WampMessageType.PUBLISH
				|| type == WampMessageType.SUBSCRIBE
				|| type == WampMessageType.UNSUBSCRIBE;
	}
}