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
package ch.rasc.wampspring.support;

import java.security.Principal;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

import ch.rasc.wampspring.message.WampMessage;

/**
 * Argument resolver that handles parameters of type {@link Principal}
 *
 * @author Rossen Stoyanchev
 * @author Ralph Schaer
 */
public class PrincipalMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return Principal.class.isAssignableFrom(paramType);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message)
			throws Exception {
		Principal user = ((WampMessage) message).getPrincipal();

		if (user == null) {
			throw new MissingPrincipalException(message);
		}

		return user;
	}

}
