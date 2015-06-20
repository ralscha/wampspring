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

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.MessageCondition;

/**
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class WampMessageMappingInfo implements MessageCondition<WampMessageMappingInfo> {

	private final WampMessageTypeMessageCondition messageTypeMessageCondition;

	private final DestinationPatternsMessageCondition destinationConditions;

	public WampMessageMappingInfo(
			WampMessageTypeMessageCondition messageTypeMessageCondition,
			DestinationPatternsMessageCondition destinationConditions) {

		this.messageTypeMessageCondition = messageTypeMessageCondition;
		this.destinationConditions = destinationConditions;
	}

	public WampMessageTypeMessageCondition getMessageTypeMessageCondition() {
		return this.messageTypeMessageCondition;
	}

	public DestinationPatternsMessageCondition getDestinationConditions() {
		return this.destinationConditions;
	}

	@Override
	public WampMessageMappingInfo combine(WampMessageMappingInfo other) {
		WampMessageTypeMessageCondition typeCond = this.getMessageTypeMessageCondition()
				.combine(other.getMessageTypeMessageCondition());
		DestinationPatternsMessageCondition destCond = this.destinationConditions
				.combine(other.getDestinationConditions());
		return new WampMessageMappingInfo(typeCond, destCond);
	}

	@Override
	public WampMessageMappingInfo getMatchingCondition(Message<?> message) {
		WampMessageTypeMessageCondition typeCond = this.messageTypeMessageCondition
				.getMatchingCondition(message);
		if (typeCond == null) {
			return null;
		}
		DestinationPatternsMessageCondition destCond = this.destinationConditions
				.getMatchingCondition(message);
		if (destCond == null) {
			return null;
		}
		return new WampMessageMappingInfo(typeCond, destCond);
	}

	@Override
	public int compareTo(WampMessageMappingInfo other, Message<?> message) {
		int result = this.messageTypeMessageCondition.compareTo(
				other.messageTypeMessageCondition, message);
		if (result != 0) {
			return result;
		}
		result = this.destinationConditions.compareTo(other.destinationConditions,
				message);
		if (result != 0) {
			return result;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof WampMessageMappingInfo) {
			WampMessageMappingInfo other = (WampMessageMappingInfo) obj;
			return this.destinationConditions.equals(other.destinationConditions)
					&& this.messageTypeMessageCondition
							.equals(other.messageTypeMessageCondition);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.destinationConditions.hashCode() * 31
				+ this.messageTypeMessageCondition.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		builder.append(this.destinationConditions);
		builder.append(",messageType=").append(this.messageTypeMessageCondition);
		builder.append('}');
		return builder.toString();
	}

}
