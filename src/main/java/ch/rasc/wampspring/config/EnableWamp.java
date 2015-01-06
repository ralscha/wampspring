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
package ch.rasc.wampspring.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Add this annotation to an {@code @Configuration} class to enable WAMP support:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWamp
 * public class MyAppConfig {
 * 
 * }
 * </pre>
 * <p>
 * Customize the imported configuration by implementing the {@link WampConfigurer}
 * interface:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWamp
 * public class MyAppConfig implements WampConfigurer {
 * 
 * 	&#064;Override
 * 	public Executor outboundExecutor() {
 * 		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 * 		executor.setThreadNamePrefix(&quot;WampOutbound-&quot;);
 * 		executor.initialize();
 * 		return executor;
 * 	}
 * 
 * 	&#064;Override
 * 	public String wampEndpointPath() {
 * 		return &quot;/wamp&quot;;
 * 	}
 * 
 * }
 * </pre>
 *
 * <p>
 * Or by extending the {@link WampConfigurerAdapter} class and overriding only the methods
 * that need to be different.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWamp
 * public class MyAppConfig extends WampConfigurerAdapter {
 * 
 * 	&#064;Override
 * 	public String wampEndpointPath() {
 * 		return &quot;/myOwnWampEndpoint&quot;;
 * 	}
 * 
 * }
 * </pre>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableWebSocket
@Import(DelegatingWampConfiguration.class)
public @interface EnableWamp {
	// nothing here
}