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
import org.springframework.util.Assert;

import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WampMessageType;

/**
 * A {@link MessageMatcher} that matches if the provided {@link Message} has a type that
 * is the same as the {@link WampMessageType} that was specified in the constructor.
 *
 * @author Rob Winch
 * @author Ralph Schaer
 *
 */
public class WampMessageTypeMatcher implements MessageMatcher<Object> {
	private final WampMessageType typeToMatch;

	/**
	 * Creates a new instance
	 *
	 * @param typeToMatch the {@link WampMessageType} that will result in a match. Cannot
	 * be null.
	 */
	public WampMessageTypeMatcher(WampMessageType typeToMatch) {
		Assert.notNull(typeToMatch, "typeToMatch cannot be null");
		this.typeToMatch = typeToMatch;
	}

	@Override
	public boolean matches(Message<? extends Object> message) {
		if (message instanceof WampMessage) {
			return ((WampMessage) message).getType() == this.typeToMatch;
		}
		return false;
	}
}