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
package ch.rasc.wampspring.handler;

import java.lang.reflect.Method;

import org.springframework.web.method.HandlerMethod;

public class WampHandlerMethod extends HandlerMethod {

	private final String[] replyTo;

	private final Boolean excludeSender;

	public WampHandlerMethod(Object bean, Method method, String[] replyTo, Boolean excludeSender) {
		super(bean, method);
		if (replyTo != null) {
			this.replyTo = replyTo;
		} else {
			this.replyTo = new String[0];
		}
		this.excludeSender = excludeSender;
	}

	public String[] getReplyTo() {
		return replyTo;
	}

	public Boolean isExcludeSender() {
		return excludeSender;
	}

}
