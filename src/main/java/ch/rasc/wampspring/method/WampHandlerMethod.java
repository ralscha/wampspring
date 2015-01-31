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
package ch.rasc.wampspring.method;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.messaging.handler.HandlerMethod;

public class WampHandlerMethod extends HandlerMethod {

	private final String[] replyTo;

	private final Boolean broadcast;

	private final Boolean excludeSender;

	private final boolean authenticationRequired;

	public WampHandlerMethod(String beanName, BeanFactory beanFactory, Method method,
			String[] replyTo, Boolean broadcast, Boolean excludeSender,
			boolean authenticationRequired) {
		super(beanName, beanFactory, method);

		if (replyTo != null) {
			this.replyTo = replyTo;
		}
		else {
			this.replyTo = new String[0];
		}

		this.broadcast = broadcast;

		this.excludeSender = excludeSender;

		this.authenticationRequired = authenticationRequired;
	}

	public String[] getReplyTo() {
		return this.replyTo;
	}

	public boolean isBroadcast() {
		return this.broadcast != null && this.broadcast.booleanValue();
	}

	public boolean isExcludeSender() {
		return this.excludeSender != null && this.excludeSender.booleanValue();
	}

	public boolean isAuthenticationRequired() {
		return this.authenticationRequired;
	}

}
