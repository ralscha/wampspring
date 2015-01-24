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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.security.config.annotation.web.configurers.RememberMeConfigurer;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.messaging.access.expression.ExpressionBasedMessageSecurityMetadataSourceFactory;
import org.springframework.security.messaging.access.intercept.MessageSecurityMetadataSource;
import org.springframework.security.messaging.util.matcher.MessageMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import ch.rasc.wampspring.message.WampMessageType;

/**
 * Allows mapping security constraints using {@link MessageMatcher} to the security
 * expressions.
 *
 * @author Rob Winch
 * @author Ralph Schaer
 */
public class WampMessageSecurityMetadataSourceRegistry {
	private static final String permitAll = "permitAll";
	private static final String denyAll = "denyAll";
	private static final String anonymous = "anonymous";
	private static final String authenticated = "authenticated";
	private static final String fullyAuthenticated = "fullyAuthenticated";
	private static final String rememberMe = "rememberMe";

	private final LinkedHashMap<MatcherBuilder, String> matcherToExpression = new LinkedHashMap<>();

	private PathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * Maps any {@link Message} to a security expression.
	 *
	 * @return the Expression to associate
	 */
	public Constraint anyMessage() {
		return matchers(MessageMatcher.ANY_MESSAGE);
	}

	/**
	 * Maps any {@link Message} that has a null destination
	 * header 
	 *
	 * @return the Expression to associate
	 */
	public Constraint nullDestMatcher() {
		return matchers(WampDestinationMessageMatcher.NULL_DESTINATION_MATCHER);
	}

	/**
	 * Maps a {@link List} of {@link WampDestinationMessageMatcher} instances.
	 *
	 * @param typesToMatch the {@link WampMessageType} instance to match on
	 * @return the {@link Constraint} associated to the matchers.
	 */
	public Constraint wampTypeMatchers(WampMessageType... typesToMatch) {
		MessageMatcher<?>[] typeMatchers = new MessageMatcher<?>[typesToMatch.length];
		for (int i = 0; i < typesToMatch.length; i++) {
			WampMessageType typeToMatch = typesToMatch[i];
			typeMatchers[i] = new WampMessageTypeMatcher(typeToMatch);
		}
		return matchers(typeMatchers);
	}

	/**
	 * Maps a {@link List} of {@link WampDestinationMessageMatcher} instances without
	 * regard to the {@link WampMessageType}. If no destination is found on the Message,
	 * then the Matcher returns false.
	 *
	 * @param patterns the patterns to create {@link WampDestinationMessageMatcher} from.
	 * Uses {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 * .
	 *
	 * @return the {@link Constraint} that is associated to the {@link MessageMatcher}
	 * @see {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 */
	public Constraint wampDestMatchers(String... patterns) {
		return wampDestMatchers(null, patterns);
	}

	/**
	 * Maps a {@link List} of {@link WampDestinationMessageMatcher} instances that match
	 * on {@code WampMessageType.CALL}. If no destination is found on the Message, then
	 * the Matcher returns false.
	 *
	 * @param patterns the patterns to create {@link WampDestinationMessageMatcher} from.
	 * Uses {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 * .
	 *
	 * @return the {@link Constraint} that is associated to the {@link MessageMatcher}
	 * @see {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 */
	public Constraint wampDestCallMatchers(String... patterns) {
		return wampDestMatchers(WampMessageType.CALL, patterns);
	}

	/**
	 * Maps a {@link List} of {@link WampDestinationMessageMatcher} instances that match
	 * on {@code WampMessageType.PUBLISH}. If no destination is found on the Message, then
	 * the Matcher returns false.
	 *
	 * @param patterns the patterns to create {@link WampDestinationMessageMatcher} from.
	 * Uses {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 * .
	 *
	 * @return the {@link Constraint} that is associated to the {@link MessageMatcher}
	 * @see {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 */
	public Constraint wampDestPublishMatchers(String... patterns) {
		return wampDestMatchers(WampMessageType.PUBLISH, patterns);
	}

	/**
	 * Maps a {@link List} of {@link WampDestinationMessageMatcher} instances that match
	 * on {@code WampMessageType.SUBSCRIBE}. If no destination is found on the Message,
	 * then the Matcher returns false.
	 *
	 * @param patterns the patterns to create {@link WampDestinationMessageMatcher} from.
	 * Uses {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 * .
	 *
	 * @return the {@link Constraint} that is associated to the {@link MessageMatcher}
	 * @see {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 */
	public Constraint wampDestSubscribeMatchers(String... patterns) {
		return wampDestMatchers(WampMessageType.SUBSCRIBE, patterns);
	}

	/**
	 * Maps a {@link List} of {@link WampDestinationMessageMatcher} instances that match
	 * on {@code WampMessageType.UNSUBSCRIBE}. If no destination is found on the Message,
	 * then the Matcher returns false.
	 *
	 * @param patterns the patterns to create {@link WampDestinationMessageMatcher} from.
	 * Uses {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 * .
	 *
	 * @return the {@link Constraint} that is associated to the {@link MessageMatcher}
	 * @see {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 */
	public Constraint wampDestUnsubscribeMatchers(String... patterns) {
		return wampDestMatchers(WampMessageType.UNSUBSCRIBE, patterns);
	}

	/**
	 * Maps a {@link List} of {@link WampDestinationMessageMatcher} instances. If no
	 * destination is found on the Message, then the Matcher returns false.
	 *
	 * @param type the {@link WampMessageType} to match on. If null, the
	 * {@link WampMessageType} is not considered for matching.
	 * @param patterns the patterns to create {@link WampDestinationMessageMatcher} from.
	 * Uses {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 * .
	 *
	 * @return the {@link Constraint} that is associated to the {@link MessageMatcher}
	 * @see {@link MessageSecurityMetadataSourceRegistry#wampDestPathMatcher(PathMatcher)}
	 */
	private Constraint wampDestMatchers(WampMessageType type, String... patterns) {
		List<MatcherBuilder> matchers = new ArrayList<>(patterns.length);
		for (String pattern : patterns) {
			matchers.add(new PathMatcherMessageMatcherBuilder(pattern, type));
		}
		return new Constraint(matchers);
	}

	/**
	 * The {@link PathMatcher} to be used with the
	 * {@link MessageSecurityMetadataSourceRegistry#wampDestMatchers(String...)}. The
	 * default is to use the default constructor of {@link AntPathMatcher}.
	 *
	 * @param pathMatcher the {@link PathMatcher} to use. Cannot be null.
	 * @return the {@link MessageSecurityMetadataSourceRegistry} for further
	 * customization.
	 */
	public WampMessageSecurityMetadataSourceRegistry wampDestPathMatcher(
			PathMatcher matcher) {
		Assert.notNull(matcher, "pathMatcher cannot be null");
		this.pathMatcher = matcher;
		return this;
	}

	/**
	 * Maps a {@link List} of {@link MessageMatcher} instances to a security expression.
	 *
	 * @param matchers the {@link MessageMatcher} instances to map.
	 * @return The {@link Constraint} that is associated to the {@link MessageMatcher}
	 * instances
	 */
	public Constraint matchers(MessageMatcher<?>... matchers) {
		List<MatcherBuilder> builders = new ArrayList<>(matchers.length);
		for (MessageMatcher<?> matcher : matchers) {
			builders.add(new PreBuiltMatcherBuilder(matcher));
		}
		return new Constraint(builders);
	}

	/**
	 * Allows subclasses to create creating a {@link MessageSecurityMetadataSource}.
	 *
	 * <p>
	 * This is not exposed so as not to confuse users of the API, which should never
	 * invoke this method.
	 * </p>
	 *
	 * @return the {@link MessageSecurityMetadataSource} to use
	 */
	protected MessageSecurityMetadataSource createMetadataSource() {
		LinkedHashMap<MessageMatcher<?>, String> mte = new LinkedHashMap<>();
		for (Map.Entry<MatcherBuilder, String> entry : this.matcherToExpression
				.entrySet()) {
			mte.put(entry.getKey().build(), entry.getValue());
		}
		return ExpressionBasedMessageSecurityMetadataSourceFactory
				.createExpressionMessageMetadataSource(mte);
	}

	/**
	 * Allows determining if a mapping was added.
	 *
	 * <p>
	 * This is not exposed so as not to confuse users of the API, which should never need
	 * to invoke this method.
	 * </p>
	 *
	 * @return true if a mapping was added, else false
	 */
	protected boolean containsMapping() {
		return !this.matcherToExpression.isEmpty();
	}

	/**
	 * Represents the security constraint to be applied to the {@link MessageMatcher}
	 * instances.
	 */
	public class Constraint {
		private final List<? extends MatcherBuilder> messageMatchers;

		/**
		 * Creates a new instance
		 *
		 * @param messageMatchers the {@link MessageMatcher} instances to map to this
		 * constraint
		 */
		private Constraint(List<? extends MatcherBuilder> messageMatchers) {
			Assert.notEmpty(messageMatchers, "messageMatchers cannot be null or empty");
			this.messageMatchers = messageMatchers;
		}

		/**
		 * Shortcut for specifying {@link Message} instances require a particular role. If
		 * you do not want to have "ROLE_" automatically inserted see
		 * {@link #hasAuthority(String)}.
		 *
		 * @param role the role to require (i.e. USER, ADMIN, etc). Note, it should not
		 * start with "ROLE_" as this is automatically inserted.
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 */
		public WampMessageSecurityMetadataSourceRegistry hasRole(String role) {
			return access(WampMessageSecurityMetadataSourceRegistry.hasRole(role));
		}

		/**
		 * Shortcut for specifying {@link Message} instances require any of a number of
		 * roles. If you do not want to have "ROLE_" automatically inserted see
		 * {@link #hasAnyAuthority(String...)}
		 *
		 * @param roles the roles to require (i.e. USER, ADMIN, etc). Note, it should not
		 * start with "ROLE_" as this is automatically inserted.
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 */
		public WampMessageSecurityMetadataSourceRegistry hasAnyRole(String... roles) {
			return access(WampMessageSecurityMetadataSourceRegistry.hasAnyRole(roles));
		}

		/**
		 * Specify that {@link Message} instances require a particular authority.
		 *
		 * @param authority the authority to require (i.e. ROLE_USER, ROLE_ADMIN, etc).
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 */
		public WampMessageSecurityMetadataSourceRegistry hasAuthority(String authority) {
			return access(WampMessageSecurityMetadataSourceRegistry
					.hasAuthority(authority));
		}

		/**
		 * Specify that {@link Message} instances requires any of a number authorities.
		 *
		 * @param authorities the requests require at least one of the authorities (i.e.
		 * "ROLE_USER","ROLE_ADMIN" would mean either "ROLE_USER" or "ROLE_ADMIN" is
		 * required).
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 */
		public WampMessageSecurityMetadataSourceRegistry hasAnyAuthority(
				String... authorities) {
			return access(WampMessageSecurityMetadataSourceRegistry
					.hasAnyAuthority(authorities));
		}

		/**
		 * Specify that Messages are allowed by anyone.
		 *
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 */
		public WampMessageSecurityMetadataSourceRegistry permitAll() {
			return access(permitAll);
		}

		/**
		 * Specify that Messages are allowed by anonymous users.
		 *
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 */
		public WampMessageSecurityMetadataSourceRegistry anonymous() {
			return access(anonymous);
		}

		/**
		 * Specify that Messages are allowed by users that have been remembered.
		 *
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 * @see {@link RememberMeConfigurer}
		 */
		public WampMessageSecurityMetadataSourceRegistry rememberMe() {
			return access(rememberMe);
		}

		/**
		 * Specify that Messages are not allowed by anyone.
		 *
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 */
		public WampMessageSecurityMetadataSourceRegistry denyAll() {
			return access(denyAll);
		}

		/**
		 * Specify that Messages are allowed by any authenticated user.
		 *
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 */
		public WampMessageSecurityMetadataSourceRegistry authenticated() {
			return access(authenticated);
		}

		/**
		 * Specify that Messages are allowed by users who have authenticated and were not
		 * "remembered".
		 *
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 * @see {@link RememberMeConfigurer}
		 */
		public WampMessageSecurityMetadataSourceRegistry fullyAuthenticated() {
			return access(fullyAuthenticated);
		}

		/**
		 * Allows specifying that Messages are secured by an arbitrary expression
		 *
		 * @param attribute the expression to secure the URLs (i.e.
		 * "hasRole('ROLE_USER') and hasRole('ROLE_SUPER')")
		 * @return the {@link WampMessageSecurityMetadataSourceRegistry} for further
		 * customization
		 */
		public WampMessageSecurityMetadataSourceRegistry access(String attribute) {
			for (MatcherBuilder messageMatcher : this.messageMatchers) {
				WampMessageSecurityMetadataSourceRegistry.this.matcherToExpression.put(
						messageMatcher, attribute);
			}
			return WampMessageSecurityMetadataSourceRegistry.this;
		}
	}

	private static String hasAnyRole(String... authorities) {
		String anyAuthorities = StringUtils.arrayToDelimitedString(authorities,
				"','ROLE_");
		return "hasAnyRole('ROLE_" + anyAuthorities + "')";
	}

	private static String hasRole(String role) {
		Assert.notNull(role, "role cannot be null");
		if (role.startsWith("ROLE_")) {
			throw new IllegalArgumentException(
					"role should not start with 'ROLE_' since it is automatically inserted. Got '"
							+ role + "'");
		}
		return "hasRole('ROLE_" + role + "')";
	}

	private static String hasAuthority(String authority) {
		return "hasAuthority('" + authority + "')";
	}

	private static String hasAnyAuthority(String... authorities) {
		String anyAuthorities = StringUtils.arrayToDelimitedString(authorities, "','");
		return "hasAnyAuthority('" + anyAuthorities + "')";
	}

	private class PreBuiltMatcherBuilder implements MatcherBuilder {
		private final MessageMatcher<?> matcher;

		private PreBuiltMatcherBuilder(MessageMatcher<?> matcher) {
			this.matcher = matcher;
		}

		@Override
		public MessageMatcher<?> build() {
			return this.matcher;
		}
	}

	private class PathMatcherMessageMatcherBuilder implements MatcherBuilder {
		private final String pattern;
		private final WampMessageType type;

		private PathMatcherMessageMatcherBuilder(String pattern, WampMessageType type) {
			this.pattern = pattern;
			this.type = type;
		}

		@Override
		public MessageMatcher<?> build() {
			if (type == null) {
				return new WampDestinationMessageMatcher(pattern, pathMatcher);
			}
			else if (WampDestinationMessageMatcher.isTypeWithDestination(type)) {
				return new WampDestinationMessageMatcher(pattern, type, pathMatcher);
			}
			throw new IllegalStateException(type
					+ " is not supported since it does not have a destination");
		}
	}

	private interface MatcherBuilder {
		MessageMatcher<?> build();
	}
}
