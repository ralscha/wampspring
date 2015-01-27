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
package ch.rasc.wampspring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ch.rasc.wampspring.config.DefaultWampConfiguration;
import ch.rasc.wampspring.message.EventMessage;

/**
 * Annotation that denotes a method that is called when the server receives a PUBLISH
 * message and the topicURI matches one of the listed values of the annotation (
 * {@link #value()}).
 *
 * If no topicURI is provided the method listens for the topicURI 'beanName.methodName'
 * <p>
 * The method <code>feed</code> in the following example listens for PUBLISH messages that
 * are sent to the topicURI 'myService.feed'. <br>
 * The method <code>publishNews</code> is called by the library when a PUBLISH message
 * with the topicURI '/topic/news' arrives.
 *
 * <pre class="code">
 * &#064;Service
 * public class MyService {
 * 
 * 	&#064;WampPublishListener
 * 	public void feed() {
 * 	}
 * 
 * 	&#064;WampPublishListener(&quot;/topic/news&quot;)
 * 	public void publishNews(String news) {
 * 	}
 * }
 * </pre>
 *
 * When this method returns a non null value and the attribute {@link #replyTo()} specifies
 * one or more destinations the return value is wrapped in an {@link EventMessage} and
 * sent to the broker which sends by default an EVENT message to every subscribers of the listed
 * {@link #replyTo()} destinations.
 * <ul>
 * <li>
 * When the {@link #excludeSender()} attribute is true the sender of the PUBLISH message
 * will not receive the EVENT message.</li>
 * <li>
 * When the {@link #broadcast()} attribute is false only the sender of the PUBLISH message
 * will receive the EVENT message.</li>
 * <li>
 * When {@link #excludeSender()} is true and {@link #broadcast()} is false no one will
 * receive an EVENT message.</li>
 * </ul>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WampPublishListener {

	/**
	 * One or more topicURI(s)/destination(s) the method should listen on. If empty the
	 * default value 'beanName.methodName' is used.
	 */
	String[] value() default {};

	/**
	 * If not empty the return value of this method (wrapped in an {@link EventMessage})
	 * is sent to all subscribers of the listed topicURI(s)/destination(s). This attribute
	 * is ignored when the method does not have a return value or the return value is
	 * <code>null</code>.
	 */
	String[] replyTo() default {};

	/**
	 * Exclude the sender of the PUBLISH message from the replyTo receivers.
	 * <p>
	 * This attribute will be ignored when no {@link EventMessage} is created.
	 * @see #replyTo()
	 */
	boolean excludeSender() default false;

	/**
	 * By default when the method has a return value and this value is not
	 * <code>null</code> and the attribute {@link #replyTo()} is not empty an EventMessage
	 * is created and sent to all subscribers of the listed topicURI(s)/destination(s).
	 * <p>
	 * If this attribute is set to false only the sender of the PUBLISH message will
	 * receive the EVENT message.
	 * <p>
	 * If this attribute is false and {@link #excludeSender()} is true no EVENT message
	 * will be created. A non null return value will be ignored.
	 * <p>
	 * @see #replyTo()
	 * @see #excludeSender()
	 */
	boolean broadcast() default true;

	/**
	 * If true a call to this method has to be authenticated. If false no authentication
	 * is required.
	 * <p>
	 * Takes precedence over {@link WampAuthenticated} and the global setting
	 * {@link DefaultWampConfiguration#authenticationRequired()}
	 */
	boolean[] authenticated() default {};
}
