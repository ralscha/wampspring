/**
 * Copyright 2014-2014 Ralph Schaer <ralphschaer@gmail.com>
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
package ch.rasc.wampspring.support;

import java.security.Principal;

import org.springframework.core.MethodParameter;

import ch.rasc.wampspring.message.WampMessage;
import ch.rasc.wampspring.message.WampMessageHeader;

/**
 * Argument resolver that handles parameters of type {@link Principal}
 * <p>
 * Credit goes to the Spring class
 * {@link org.springframework.messaging.simp.annotation.support.PrincipalMethodArgumentResolver} . This class is just a
 * copy where the resolveArgument parameter is changed to {@link WampMessage}
 */
public class PrincipalMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return Principal.class.isAssignableFrom(paramType);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, WampMessage message) throws Exception {
		return message.getHeader(WampMessageHeader.PRINCIPAL);
	}

}
