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

/**
 * Annotation that denotes a method that is called when the server receives a CALL message
 * and the procURI matches one of the listed values of the annotation ({@link #value()}).
 *
 * If no topicURI is provided the method listens for the topicURI 'beanName.methodName'
 * <p>
 * The method <code>doSomething</code> in the following example listens for CALL messages
 * that are sent to the procURI 'myService.doSomething'.<br>
 * The method <code>callMe</code> is called by the library when a CALL message with the
 * procURI 'callMe' arrives.
 *
 * <pre class="code">
 * &#064;Service
 * public class MyService {
 *
 * 	&#064;WampCallListener
 * 	public void doSomething(CallMessage message) { }
 *
 * 	&#064;WampCallListener('callMe')
 * 	public void callMe(String argument) { }
 * }
 * </pre>
 *
 * A non null return value of this method will be sent back in a CALLRESULT message to the
 * client which sent the CALL message. If this method throws an exception it will be
 * wrapped in a CALLERROR message and sent back to the client.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WampCallListener {

	/**
	 * One or more procURI(s) the method should listen on. If empty the default value
	 * 'beanName.methodName' is used.
	 */
	String[]value() default {};

	/**
	 * If true a call to this method has to be authenticated. If false no authentication
	 * is required.
	 * <p>
	 * Takes precedence over {@link WampAuthenticated} and the global setting
	 * {@link DefaultWampConfiguration#authenticationRequired()}
	 */
	boolean[]authenticated() default {};

}
