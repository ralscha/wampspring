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
package ch.rasc.wampspring.method;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.AbstractMessageCondition;
import org.springframework.util.Assert;

import ch.rasc.wampspring.message.WampMessageHeader;
import ch.rasc.wampspring.message.WampMessageType;

/**
 * A message condition that checks the message type.
 *
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class WampMessageTypeMessageCondition extends
		AbstractMessageCondition<WampMessageTypeMessageCondition> {

	public static final WampMessageTypeMessageCondition CALL = new WampMessageTypeMessageCondition(
			WampMessageType.CALL);

	public static final WampMessageTypeMessageCondition PUBLISH = new WampMessageTypeMessageCondition(
			WampMessageType.PUBLISH);

	public static final WampMessageTypeMessageCondition SUBSCRIBE = new WampMessageTypeMessageCondition(
			WampMessageType.SUBSCRIBE);

	public static final WampMessageTypeMessageCondition UNSUBSCRIBE = new WampMessageTypeMessageCondition(
			WampMessageType.UNSUBSCRIBE);

	private final WampMessageType messageType;

	public WampMessageTypeMessageCondition(WampMessageType messageType) {
		Assert.notNull(messageType, "MessageType must not be null");
		this.messageType = messageType;
	}

	public WampMessageType getMessageType() {
		return this.messageType;
	}

	@Override
	protected Collection<?> getContent() {
		return Arrays.asList(this.messageType);
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	@Override
	public WampMessageTypeMessageCondition combine(WampMessageTypeMessageCondition other) {
		return other;
	}

	@Override
	public WampMessageTypeMessageCondition getMatchingCondition(Message<?> message) {

		WampMessageType actualMessageType = (WampMessageType) message.getHeaders().get(
				WampMessageHeader.WAMP_MESSAGE_TYPE.name());
		if (actualMessageType != this.messageType) {
			return null;
		}

		return this;
	}

	@Override
	public int compareTo(WampMessageTypeMessageCondition other, Message<?> message) {
		WampMessageType actualMessageType = (WampMessageType) message.getHeaders().get(
				WampMessageHeader.WAMP_MESSAGE_TYPE.name());
		if (actualMessageType != null) {
			if (actualMessageType == this.getMessageType()
					&& actualMessageType == other.getMessageType()) {
				return 0;
			}
			else if (actualMessageType == this.getMessageType()) {
				return -1;
			}
			else if (actualMessageType == other.getMessageType()) {
				return 1;
			}
		}
		return 0;
	}
}
