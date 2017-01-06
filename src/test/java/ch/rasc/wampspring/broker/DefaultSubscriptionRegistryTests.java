/**
 * Copyright 2014-2017 Ralph Schaer <ralphschaer@gmail.com>
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;

import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.PubSubMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Ralph Schaer
 */
public class DefaultSubscriptionRegistryTests {

	private DefaultSubscriptionRegistry registry;

	@Before
	public void setup() {
		this.registry = new DefaultSubscriptionRegistry(new AntPathMatcher());
	}

	@Test
	public void registerSubscriptionInvalidInput() {
		String sessId = "sess01";
		String dest = "/foo";

		this.registry.registerSubscription(subscribeMessage(null, dest));
		assertEquals(0, this.registry.findSubscriptions(message(dest)).size());

		this.registry.registerSubscription(subscribeMessage(sessId, null));
		assertEquals(0, this.registry.findSubscriptions(message(dest)).size());
	}

	@Test
	public void registerSubscription() {
		String sessId = "sess01";
		String dest = "/foo";

		this.registry.registerSubscription(subscribeMessage(sessId, dest));
		Set<String> actual = this.registry.findSubscriptions(message(dest));

		assertEquals("Expected one element " + actual, 1, actual.size());
		assertEquals(sessId, actual.iterator().next());
	}

	@Test
	public void registerSubscriptionOneSession() {
		String sessId = "sess01";
		String dest = "/foo";

		this.registry.registerSubscription(subscribeMessage(sessId, dest));
		this.registry.registerSubscription(subscribeMessage(sessId, dest));

		Set<String> actual = this.registry.findSubscriptions(message(dest));

		assertEquals("Expected one element " + actual, 1, actual.size());
		assertEquals(sessId, actual.iterator().next());
	}

	@Test
	public void registerSubscriptionMultipleSessions() {

		List<String> sessIds = Arrays.asList("sess01", "sess02", "sess03");
		String dest = "/foo";

		for (String sessId : sessIds) {
			this.registry.registerSubscription(subscribeMessage(sessId, dest));
			this.registry.registerSubscription(subscribeMessage(sessId, dest));
		}

		List<String> actual = new ArrayList<>(
				this.registry.findSubscriptions(message(dest)));
		Collections.sort(actual);

		assertEquals("Expected three elements " + actual, 3, actual.size());
		assertEquals(sessIds, actual);
	}

	@Test
	public void registerSubscriptionWithDestinationPattern() {

		String sessId = "sess01";
		String destPattern = "/topic/PRICE.STOCK.*.IBM";
		String dest = "/topic/PRICE.STOCK.NASDAQ.IBM";

		this.registry.registerSubscription(subscribeMessage(sessId, destPattern));
		Set<String> actual = this.registry.findSubscriptions(message(dest));

		assertEquals("Expected one element " + actual, 1, actual.size());
		assertEquals(sessId, actual.iterator().next());
	}

	// SPR-11657

	@Test
	public void registerSubscriptionsWithSimpleAndPatternDestinations() {

		String sess1 = "sess01";
		String sess2 = "sess02";

		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.*.IBM"));
		Set<String> actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).hasSize(1).contains(sess1);

		this.registry.registerSubscription(
				subscribeMessage(sess2, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		this.registry.registerSubscription(
				subscribeMessage(sess2, "/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.registerSubscription(
				subscribeMessage(sess2, "/topic/PRICE.STOCK.NASDAQ.GOOG"));
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).hasSize(2).contains(sess1, sess2);

		this.registry.unregisterSession(sess1);
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).hasSize(1).contains(sess2);

		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.*.IBM"));
		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).hasSize(2).contains(sess1, sess2);

		this.registry.unregisterSubscription(
				unsubscribeMessage(sess1, "/topic/PRICE.STOCK.*.IBM"));
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).hasSize(2).contains(sess1, sess2);

		this.registry.unregisterSubscription(
				unsubscribeMessage(sess1, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).hasSize(1).contains(sess2);

		this.registry.unregisterSubscription(
				unsubscribeMessage(sess2, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).isEmpty();
	}

	// SPR-11755

	@Test
	public void registerAndUnregisterMultipleDestinations() {

		String sess1 = "sess01";
		String sess2 = "sess02";

		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.NASDAQ.GOOG"));

		Set<String> actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NYSE.IBM"));
		assertThat(actual).hasSize(1).contains("sess01");
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.GOOG"));
		assertThat(actual).hasSize(1).contains("sess01");
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).hasSize(1).contains("sess01");

		this.registry.unregisterSubscription(
				unsubscribeMessage(sess1, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		this.registry.unregisterSubscription(
				unsubscribeMessage(sess1, "/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.unregisterSubscription(
				unsubscribeMessage(sess1, "/topic/PRICE.STOCK.NASDAQ.GOOG"));

		actual = this.registry.findSubscriptions(message("/topic/PRICE.STOCK.NYSE.IBM"));
		assertThat(actual).isEmpty();
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.GOOG"));
		assertThat(actual).isEmpty();
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).isEmpty();

		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.NASDAQ.IBM"));
		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.NASDAQ.GOOG"));
		this.registry.registerSubscription(
				subscribeMessage(sess1, "/topic/PRICE.STOCK.NYSE.IBM"));
		this.registry.registerSubscription(
				subscribeMessage(sess2, "/topic/PRICE.STOCK.NASDAQ.GOOG"));
		this.registry.unregisterSession(sess1);
		this.registry.unregisterSession(sess2);

		actual = this.registry.findSubscriptions(message("/topic/PRICE.STOCK.NYSE.IBM"));
		assertThat(actual).isEmpty();
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.GOOG"));
		assertThat(actual).isEmpty();
		actual = this.registry
				.findSubscriptions(message("/topic/PRICE.STOCK.NASDAQ.IBM"));
		assertThat(actual).isEmpty();
	}

	@Test
	public void registerSubscriptionWithDestinationPatternRegex() {
		String sessId = "sess01";
		String destPattern = "/topic/PRICE.STOCK.*.{ticker:(IBM|MSFT)}";

		this.registry.registerSubscription(subscribeMessage(sessId, destPattern));
		PubSubMessage message = message("/topic/PRICE.STOCK.NASDAQ.IBM");
		Set<String> actual = this.registry.findSubscriptions(message);
		assertThat(actual).hasSize(1).contains(sessId);

		message = message("/topic/PRICE.STOCK.NASDAQ.MSFT");
		actual = this.registry.findSubscriptions(message);
		assertThat(actual).hasSize(1).contains(sessId);

		message = message("/topic/PRICE.STOCK.NASDAQ.VMW");
		actual = this.registry.findSubscriptions(message);
		assertThat(actual).isEmpty();
	}

	@Test
	public void unregisterSubscription() {
		List<String> sessIds = Arrays.asList("sess01", "sess02", "sess03");
		String dest = "/foo";

		for (String sessId : sessIds) {
			this.registry.registerSubscription(subscribeMessage(sessId, dest));
			this.registry.registerSubscription(subscribeMessage(sessId, dest));
		}

		this.registry.unregisterSubscription(unsubscribeMessage(sessIds.get(0), "/foo"));
		Set<String> actual = this.registry.findSubscriptions(message(dest));
		assertThat(actual).hasSize(2).contains(sessIds.get(1), sessIds.get(2));
	}

	@Test
	public void unregisterAllSubscriptions() {

		List<String> sessIds = Arrays.asList("sess01", "sess02", "sess03");
		String dest = "/foo";

		for (String sessId : sessIds) {
			this.registry.registerSubscription(subscribeMessage(sessId, dest));
			this.registry.registerSubscription(subscribeMessage(sessId, dest));
		}

		this.registry.unregisterSession(sessIds.get(0));
		this.registry.unregisterSession(sessIds.get(1));

		Set<String> actual = this.registry.findSubscriptions(message(dest));
		assertThat(actual).hasSize(1).contains(sessIds.get(2));
	}

	@Test
	public void unregisterAllSubscriptionsNoMatch() {
		this.registry.unregisterSession("bogus");
		// no exceptions
	}

	@Test
	public void findSubscriptionsReturnsMapSafeToIterate() throws Exception {
		this.registry.registerSubscription(subscribeMessage("sess1", "/foo"));
		this.registry.registerSubscription(subscribeMessage("sess2", "/foo"));
		Set<String> subscriptions = this.registry.findSubscriptions(message("/foo"));
		assertEquals(2, subscriptions.size());

		Iterator<String> iterator = subscriptions.iterator();
		iterator.next();

		this.registry.registerSubscription(subscribeMessage("sess3", "/foo"));

		iterator.next();
		// no ConcurrentModificationException
	}

	@Test
	public void findSubscriptionsNoMatches() {
		Set<String> actual = this.registry.findSubscriptions(message("/foo"));
		assertEquals("Expected no elements " + actual, 0, actual.size());
	}

	private static SubscribeMessage subscribeMessage(String sessionId, String topicURI) {
		SubscribeMessage message = new SubscribeMessage(topicURI);
		message.setWebSocketSessionId(sessionId);
		return message;
	}

	private static UnsubscribeMessage unsubscribeMessage(String sessionId,
			String topicURI) {
		UnsubscribeMessage message = new UnsubscribeMessage(topicURI);
		message.setWebSocketSessionId(sessionId);
		return message;
	}

	private static PubSubMessage message(String destination) {
		EventMessage eventMessage = new EventMessage(destination, "the payload");
		return eventMessage;
	}

}
