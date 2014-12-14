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
import ch.rasc.wampspring.message.CallErrorMessage;
import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.CallResultMessage;

/**
 * Annotation that denotes a method that is called when the server receives a
 * {@link CallMessage} and the procURI matches one of the listed values of the annotation.
 *
 * If no procURI is provided the method is accessible by the procURI 'beanName.methodName'
 *
 * In the following example the method can be called by sending a CallMessage with the
 * procURI 'myService.doSomething'
 *
 * <pre class="code">
 * &#064;Service
 * public class MyService {
 * 
 * 	&#064;WampCallListener
 * 	public void doSomething(CallMessage message) {
 * 
 * 	}
 * }
 * </pre>
 *
 * The return value of such annotated method (if any) will be sent back to the calling
 * client with a {@link CallResultMessage} or {@link CallErrorMessage}.
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WampCallListener {

	/**
	 * ProcURI for the call.
	 */
	String[] value() default {};

	/**
	 * If true a call to this annotated method has to be authenticated. If false no
	 * authentication is required. Takes precedence over {@link WampAuthenticated} and the
	 * global setting {@link WampConfigurer#authenticationRequired()}
	 */
	boolean[] authenticated() default {};

}
