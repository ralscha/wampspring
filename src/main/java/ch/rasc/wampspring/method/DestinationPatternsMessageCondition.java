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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.AbstractMessageCondition;
import org.springframework.messaging.handler.MessageCondition;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

import ch.rasc.wampspring.message.WampMessage;

/**
 * A {@link MessageCondition} for matching the destination of a Message against one or
 * more destination patterns using a {@link PathMatcher}.
 *
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class DestinationPatternsMessageCondition extends
		AbstractMessageCondition<DestinationPatternsMessageCondition> {

	private final Set<String> patterns;

	private final PathMatcher pathMatcher;

	/**
	 * Alternative constructor accepting a custom PathMatcher.
	 * @param patterns the URL patterns to use; if 0, the condition will match to every
	 * request.
	 * @param pathMatcher the PathMatcher to use
	 */
	public DestinationPatternsMessageCondition(String[] patterns, PathMatcher pathMatcher) {
		this(asList(patterns), pathMatcher);
	}

	private DestinationPatternsMessageCondition(Collection<String> patterns,
			PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "pathMatcher is required");

		this.pathMatcher = pathMatcher;
		this.patterns = Collections.unmodifiableSet(new LinkedHashSet<>(patterns));
	}

	private static List<String> asList(String... patterns) {
		return patterns != null ? Arrays.asList(patterns) : Collections
				.<String> emptyList();
	}

	public Set<String> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<String> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * Returns a new instance with URL patterns from the current instance ("this") and the
	 * "other" instance as follows:
	 * <ul>
	 * <li>If there are patterns in both instances, combine the patterns in "this" with
	 * the patterns in "other" using
	 * {@link org.springframework.util.PathMatcher#combine(String, String)}.
	 * <li>If only one instance has patterns, use them.
	 * <li>If neither instance has patterns, use an empty String (i.e. "").
	 * </ul>
	 */
	@Override
	public DestinationPatternsMessageCondition combine(
			DestinationPatternsMessageCondition other) {
		Set<String> result = new LinkedHashSet<>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (String pattern1 : this.patterns) {
				for (String pattern2 : other.patterns) {
					result.add(this.pathMatcher.combine(pattern1, pattern2));
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			result.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			result.addAll(other.patterns);
		}
		else {
			result.add("");
		}
		return new DestinationPatternsMessageCondition(result, this.pathMatcher);
	}

	/**
	 * Check if any of the patterns match the given Message destination and return an
	 * instance that is guaranteed to contain matching patterns, sorted via
	 * {@link org.springframework.util.PathMatcher#getPatternComparator(String)}.
	 * @param message the message to match to
	 * @return the same instance if the condition contains no patterns; or a new condition
	 * with sorted matching patterns; or {@code null} either if a destination can not be
	 * extracted or there is no match
	 */
	@Override
	public DestinationPatternsMessageCondition getMatchingCondition(Message<?> message) {
		WampMessage wampMessage = (WampMessage) message;
		String destination = wampMessage.getDestination();
		if (destination == null) {
			return null;
		}

		if (this.patterns.isEmpty()) {
			return this;
		}

		List<String> matches = new ArrayList<>();
		for (String pattern : this.patterns) {
			if (pattern.equals(destination)
					|| this.pathMatcher.match(pattern, destination)
					|| "**".equals(destination)) {
				matches.add(pattern);
			}
		}

		if (matches.isEmpty()) {
			return null;
		}

		Collections.sort(matches, this.pathMatcher.getPatternComparator(destination));
		return new DestinationPatternsMessageCondition(matches, this.pathMatcher);
	}

	/**
	 * Compare the two conditions based on the destination patterns they contain. Patterns
	 * are compared one at a time, from top to bottom via
	 * {@link org.springframework.util.PathMatcher#getPatternComparator(String)}. If all
	 * compared patterns match equally, but one instance has more patterns, it is
	 * considered a closer match.
	 * <p>
	 * It is assumed that both instances have been obtained via
	 * {@link #getMatchingCondition(Message)} to ensure they contain only patterns that
	 * match the request and are sorted with the best matches on top.
	 */
	@Override
	public int compareTo(DestinationPatternsMessageCondition other, Message<?> message) {
		WampMessage wampMessage = (WampMessage) message;
		String destination = wampMessage.getDestination();

		Comparator<String> patternComparator = this.pathMatcher
				.getPatternComparator(destination);

		Iterator<String> iterator = this.patterns.iterator();
		Iterator<String> iteratorOther = other.patterns.iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
			if (result != 0) {
				return result;
			}
		}
		if (iterator.hasNext()) {
			return -1;
		}
		else if (iteratorOther.hasNext()) {
			return 1;
		}
		else {
			return 0;
		}
	}

}