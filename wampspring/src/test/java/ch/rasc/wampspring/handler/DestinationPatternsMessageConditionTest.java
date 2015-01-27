/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.rasc.wampspring.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.util.AntPathMatcher;

import ch.rasc.wampspring.message.CallMessage;

/**
 * Unit tests for {@link DestinationPatternsMessageCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class DestinationPatternsMessageConditionTest {

	@Test
	public void prependSlashWithCustomPathSeparator() {
		DestinationPatternsMessageCondition c = new DestinationPatternsMessageCondition(
				new String[] { "foo" }, new AntPathMatcher("."));

		assertEquals(
				"Pre-pending should be disabled when not using '/' as path separator",
				"foo", c.getPatterns().iterator().next());
	}

	@Test
	public void prependNonEmptyPatternsOnly() {
		DestinationPatternsMessageCondition c = condition("");
		assertEquals("", c.getPatterns().iterator().next());
	}

	@Test
	public void combineEmptySets() {
		DestinationPatternsMessageCondition c1 = condition();
		DestinationPatternsMessageCondition c2 = condition();

		assertEquals(condition(""), c1.combine(c2));
	}

	@Test
	public void combineOnePatternWithEmptySet() {
		DestinationPatternsMessageCondition c1 = condition("/type1", "/type2");
		DestinationPatternsMessageCondition c2 = condition();

		assertEquals(condition("/type1", "/type2"), c1.combine(c2));

		c1 = condition();
		c2 = condition("/method1", "/method2");

		assertEquals(condition("/method1", "/method2"), c1.combine(c2));
	}

	@Test
	public void combineMultiplePatterns() {
		DestinationPatternsMessageCondition c1 = condition("/t1", "/t2");
		DestinationPatternsMessageCondition c2 = condition("/m1", "/m2");

		assertEquals(new DestinationPatternsMessageCondition(new String[] { "/t1/m1",
				"/t1/m2", "/t2/m1", "/t2/m2" }, new AntPathMatcher()), c1.combine(c2));
	}

	@Test
	public void matchDirectPath() {
		DestinationPatternsMessageCondition condition = condition("/foo");
		DestinationPatternsMessageCondition match = condition
				.getMatchingCondition(messageTo("/foo"));

		assertNotNull(match);
	}

	@Test
	public void matchPattern() {
		DestinationPatternsMessageCondition condition = condition("/foo/*");
		DestinationPatternsMessageCondition match = condition
				.getMatchingCondition(messageTo("/foo/bar"));

		assertNotNull(match);
	}

	@Test
	public void matchSortPatterns() {
		DestinationPatternsMessageCondition condition = condition("/**", "/foo/bar",
				"/foo/*");
		DestinationPatternsMessageCondition match = condition
				.getMatchingCondition(messageTo("/foo/bar"));
		DestinationPatternsMessageCondition expected = condition("/foo/bar", "/foo/*",
				"/**");

		assertEquals(expected, match);
	}

	@Test
	public void compareEqualPatterns() {
		DestinationPatternsMessageCondition c1 = condition("/foo*");
		DestinationPatternsMessageCondition c2 = condition("/foo*");

		assertEquals(0, c1.compareTo(c2, messageTo("/foo")));
	}

	@Test
	public void comparePatternSpecificity() {
		DestinationPatternsMessageCondition c1 = condition("/fo*");
		DestinationPatternsMessageCondition c2 = condition("/foo");

		assertEquals(1, c1.compareTo(c2, messageTo("/foo")));
	}

	@Test
	public void compareNumberOfMatchingPatterns() throws Exception {
		Message<?> message = messageTo("/foo");

		DestinationPatternsMessageCondition c1 = condition("/foo", "/bar");
		DestinationPatternsMessageCondition c2 = condition("/foo", "/f*");

		DestinationPatternsMessageCondition match1 = c1.getMatchingCondition(message);
		DestinationPatternsMessageCondition match2 = c2.getMatchingCondition(message);

		assertEquals(1, match1.compareTo(match2, message));
	}

	private static DestinationPatternsMessageCondition condition(String... patterns) {
		return new DestinationPatternsMessageCondition(patterns, new AntPathMatcher());
	}

	private static CallMessage messageTo(String destination) {
		return new CallMessage(UUID.randomUUID().toString(), destination);
	}

}
