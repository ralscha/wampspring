/**
 * Copyright 2002-2014 the original author or authors.
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
package ch.rasc.wampspring.broker;

import java.util.Set;

import ch.rasc.wampspring.message.PubSubMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;

/**
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public interface SubscriptionRegistry {

	/**
	 * Register a subscription represented by the given message.
	 * @param subscribeMessage the subscription request
	 */
	void registerSubscription(SubscribeMessage subscribeMessage);

	/**
	 * Unregister a subscription.
	 * @param unsubscribeMessage the request to unsubscribe
	 */
	void unregisterSubscription(UnsubscribeMessage unsubscribeMessage);

	/**
	 * Remove session
	 */
	void unregisterSession(String sessionId);

	/**
	 * Find all sessionIds that should receive the given message.
	 * @param message the message
	 * @return a {@link Set} of session ids, possibly empty.
	 */
	Set<String> findSubscriptions(PubSubMessage pubSubMessage);

}