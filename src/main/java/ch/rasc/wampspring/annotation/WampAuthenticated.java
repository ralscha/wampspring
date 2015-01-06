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

import ch.rasc.wampspring.config.WampConfigurer;

/**
 * Class level annotation.
 * <p>
 * If present all wamp calls to methods in this class that are annotated with
 * {@link WampCallListener}, {@link WampPublishListener}, {@link WampSubscribeListener} or
 * {@link WampUnsubscribeListener} must be authenticated.
 * <p>
 * Every method can disable this requirement by setting the authentication attribute to
 * false
 *
 * This annotation is ignored when authentication is globally enabled (
 * {@link WampConfigurer#authenticationRequired()})
 *
 * @see WampCallListener#authenticated()
 * @see WampPublishListener#authenticated()
 * @see WampSubscribeListener#authenticated()
 * @see WampUnsubscribeListener#authenticated()
 *
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WampAuthenticated {
	// nothing here
}
