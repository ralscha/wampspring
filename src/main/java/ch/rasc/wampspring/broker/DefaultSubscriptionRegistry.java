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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.util.PathMatcher;

import ch.rasc.wampspring.message.PubSubMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Ralph Schaer
 */
public class DefaultSubscriptionRegistry implements SubscriptionRegistry {

	/** Default maximum number of entries for the destination cache: 1024 */
	private static final int DEFAULT_CACHE_LIMIT = 1024;

	/** The maximum number of entries in the cache */
	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	private final PathMatcher pathMatcher;

	private final DestinationCache destinationCache = new DestinationCache();

	protected final Log logger = LogFactory.getLog(getClass());

	// webSocketSessionId -> destinations
	private final ConcurrentMap<String, Set<String>> sessionDestinations = new ConcurrentHashMap<>();

	private final Object monitor = new Object();

	public DefaultSubscriptionRegistry(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	@Override
	public final void registerSubscription(SubscribeMessage subscribeMessage) {
		String sessionId = subscribeMessage.getSessionId();
		String destination = subscribeMessage.getTopicURI();
		if (sessionId != null && destination != null) {
			addSessionId(sessionId, destination);
			this.destinationCache.updateAfterNewSession(destination, sessionId);
		}
	}

	@Override
	public final void unregisterSubscription(UnsubscribeMessage unsubscribeMessage) {
		String sessionId = unsubscribeMessage.getSessionId();
		String destination = unsubscribeMessage.getTopicURI();

		if (sessionId != null && destination != null) {
			removeSessionDestination(sessionId, destination);
		}
	}

	@Override
	public final Set<String> findSubscriptions(PubSubMessage pubSubMessge) {
		String destination = pubSubMessge.getDestination();
		return findSubscriptionsInternal(destination);
	}

	@Override
	public boolean hasSubscriptions() {
		return !sessionDestinations.isEmpty();
	}

	/**
	 * Specify the maximum number of entries for the resolved destination cache. Default
	 * is 1024.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	protected void removeSessionDestination(String sessionId, String destination) {

		Set<String> destinations = sessionDestinations.get(sessionId);
		if (destinations != null) {
			String removedDestination = null;
			String removedSessionId = null;
			synchronized (this.monitor) {
				if (destinations.remove(destination)) {
					removedDestination = destination;
					if (destinations.isEmpty()) {
						sessionDestinations.remove(sessionId);
						removedSessionId = sessionId;
					}
				}
			}
			if (removedDestination != null) {
				this.destinationCache.updateAfterRemovedDestination(sessionId,
						removedDestination);
			}
			if (removedSessionId != null) {
				this.destinationCache.updateAfterRemovedSession(removedSessionId);
			}
		}

	}

	@Override
	public void unregisterSession(String sessionId) {
		Set<String> destinations = sessionDestinations.remove(sessionId);
		if (destinations != null) {
			this.destinationCache.updateAfterRemovedSession(sessionId);
		}
	}

	private Set<String> addSessionId(String sessionId, String destination) {
		Set<String> destinations = this.sessionDestinations.get(sessionId);
		if (destinations == null) {
			synchronized (this.monitor) {
				destinations = this.sessionDestinations.get(sessionId);
				if (destinations == null) {
					destinations = new HashSet<>(4);
					sessionDestinations.put(sessionId, destinations);
				}
			}
		}
		destinations.add(destination);
		return destinations;
	}

	private Set<String> findSubscriptionsInternal(String destination) {
		Set<String> sessionIds = this.destinationCache.getSessionIds(destination);
		if (sessionIds != null) {
			return sessionIds;
		}

		sessionIds = new HashSet<>();
		for (Map.Entry<String, Set<String>> subscribedSessions : this.sessionDestinations
				.entrySet()) {
			String sessionId = subscribedSessions.getKey();
			for (String destinationPattern : subscribedSessions.getValue()) {
				if (this.pathMatcher.match(destinationPattern, destination)) {
					sessionIds.add(sessionId);
				}
			}
		}

		if (!sessionIds.isEmpty()) {
			this.destinationCache.addSessionIds(destination, sessionIds);
		}

		return sessionIds;
	}

	/**
	 * A cache for destinations previously resolved via
	 * {@link DefaultSubscriptionRegistry#findSubscriptionsInternal(String, Message)}
	 */
	private class DestinationCache {

		/** Map from destination -> sessionId for fast look-ups */
		private final Map<String, Set<String>> accessCache = new ConcurrentHashMap<>(
				DEFAULT_CACHE_LIMIT);

		/** Map from destination -> sessionId with locking */
		@SuppressWarnings("serial")
		private final Map<String, Set<String>> updateCache = new LinkedHashMap<String, Set<String>>(
				DEFAULT_CACHE_LIMIT, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
				return size() > cacheLimit;
			}
		};

		public Set<String> getSessionIds(String destination) {
			return this.accessCache.get(destination);
		}

		public void addSessionIds(String destination, Set<String> sessionIds) {
			synchronized (this.updateCache) {
				this.updateCache.put(destination, sessionIds);
				this.accessCache.put(destination, new HashSet<>(sessionIds));
			}
		}

		public void updateAfterNewSession(String destination, String sessionId) {
			synchronized (this.updateCache) {
				for (Map.Entry<String, Set<String>> entry : this.updateCache.entrySet()) {
					String cachedDestination = entry.getKey();
					if (pathMatcher.match(destination, cachedDestination)) {
						Set<String> sessionIds = entry.getValue();
						sessionIds.add(sessionId);
						this.accessCache
								.put(cachedDestination, new HashSet<>(sessionIds));
					}
				}
			}
		}

		public void updateAfterRemovedDestination(String sessionId, String destination) {
			synchronized (this.updateCache) {
				Set<String> sessionIds = this.updateCache.get(destination);
				if (sessionIds != null) {
					sessionIds.remove(sessionId);
					if (sessionIds.isEmpty()) {
						this.updateCache.remove(destination);
						this.accessCache.remove(destination);
					}
					else {
						this.accessCache.put(destination, new HashSet<>(sessionIds));
					}
				}
			}
		}

		public void updateAfterRemovedSession(String sessionId) {
			synchronized (this.updateCache) {
				Set<String> destinationsToRemove = new HashSet<>();
				for (Map.Entry<String, Set<String>> entry : this.updateCache.entrySet()) {
					String destination = entry.getKey();
					Set<String> sessiondIds = entry.getValue();
					if (sessiondIds.remove(sessionId)) {
						if (sessiondIds.isEmpty()) {
							destinationsToRemove.add(destination);
						}
						else {
							this.accessCache.put(destination, new HashSet<>(sessiondIds));
						}
					}
				}
				for (String destination : destinationsToRemove) {
					this.updateCache.remove(destination);
					this.accessCache.remove(destination);
				}
			}
		}

		@Override
		public String toString() {
			return "cache[" + this.accessCache.size() + " destination(s)]";
		}
	}

}
