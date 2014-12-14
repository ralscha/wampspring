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
package ch.rasc.wampspring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ch.rasc.wampspring.config.WampConfigurer;
import ch.rasc.wampspring.message.EventMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;

/**
 * Annotation that denotes a method that is called when the server receives a
 * {@link UnsubscribeMessage} and the topicURI matches one of the listed values of the
 * annotation.
 *
 * If no topicURI is provided the method is accessible by the topicURI
 * 'beanName.methodName'
 *
 * In the following example the method can be called by sending a UnsubscribeMessage with
 * the topicURI 'myService.doSomething'
 *
 * <pre class="code">
 * &#064;Service
 * public class MyService {
 * 
 * 	&#064;WampUnsubscribeListener
 * 	public void doSomething(UnsubscribeMessage message) {
 * 
 * 	}
 * }
 * </pre>
 *
 * If the attribute replyTo has a value the return value of the method (if any) will be
 * wrapped into an {@link EventMessage} and sent to the listed topicURI(s). Additionally
 * if the excludeSender attribute is true the sender of the {@link UnsubscribeMessage}
 * does not receive an {@link EventMessage}.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WampUnsubscribeListener {

	/**
	 * TopicURI(s) for the subscription.
	 */
	String[] value() default {};

	/**
	 * Send the return value with an {@link EventMessage} to the listed TopicURI(s)
	 */
	String[] replyTo() default {};

	/**
	 * Exclude the sender of the {@link UnsubscribeMessage} from the replyTo receivers.
	 * This attribut will be ignored if no {@link EventMessage} is created.
	 */
	boolean excludeSender() default false;

	/**
	 * If true a call to this annotated method has to be authenticated. If false no
	 * authentication is required. Takes precedence over {@link WampAuthenticated} and the
	 * global setting {@link WampConfigurer#authenticationRequired()}
	 */
	boolean[] authenticated() default {};
}
