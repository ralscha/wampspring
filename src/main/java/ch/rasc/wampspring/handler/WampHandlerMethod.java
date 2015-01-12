/**
 * Copyright 2014-2015 Ralph Schaer <ralphschaer@gmail.com>
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
package ch.rasc.wampspring.handler;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.messaging.handler.HandlerMethod;

public class WampHandlerMethod extends HandlerMethod {

	private final String[] replyTo;

	private final Boolean excludeSender;

	private final boolean authenticationRequired;

	public WampHandlerMethod(String beanName, BeanFactory beanFactory, Method method,
			String[] replyTo, Boolean excludeSender, boolean authenticationRequired) {
		super(beanName, beanFactory, method);
		if (replyTo != null) {
			this.replyTo = replyTo;
		}
		else {
			this.replyTo = new String[0];
		}
		this.excludeSender = excludeSender;

		this.authenticationRequired = authenticationRequired;
	}

	public String[] getReplyTo() {
		return replyTo;
	}

	public Boolean isExcludeSender() {
		return excludeSender;
	}

	public boolean isAuthenticationRequired() {
		return authenticationRequired;
	}

}
