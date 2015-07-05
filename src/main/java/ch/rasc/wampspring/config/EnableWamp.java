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

/**
 * Add this annotation to any {@code @Configuration} class to enable the WAMP support. A
 * default endpoint '/wamp' will be registered.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWamp
 * public class MyAppConfig {
 * }
 * </pre>
 * <p>
 * Another way to enable WAMP is by creating a &#064;Configuration class that extends
 * {@link AbstractWampConfigurer}.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAutoConfiguration
 * &#064;EnableWamp
 * public class Config extends AbstractWampConfigurer {
 * 	&#064;Override
 * 	public void registerWampEndpoints(WampEndpointRegistry registry) {
 * 		registry.addEndpoint(&quot;/wampOverSockJS&quot;).withSockJS();
 * 	}
 * }
 * </pre>
 *
 * <p>
 * The third way to enable WAMP support is by creating a &#064;Configuration class that
 * extends {@link DefaultWampConfiguration}. Don't add the &#064;EnableWamp annotation in
 * this case.
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyAppConfig extends DefaultWampConfiguration {
 *
 * 	&#064;Bean
 * 	public Executor clientInboundChannelExecutor() {
 * 		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 * 		executor.setThreadNamePrefix(&quot;wampClientInboundChannel-&quot;);
 * 		executor.setCorePoolSize(4);
 * 		executor.setMaxPoolSize(2000);
 * 		executor.setKeepAliveSeconds(120);
 * 		executor.setQueueCapacity(2000);
 * 		executor.setAllowCoreThreadTimeOut(true);
 *
 * 		return executor;
 * 	}
 *
 * 	&#064;Override
 * 	public void registerWampEndpoints(WampEndpointRegistry registry) {
 * 		registry.addEndpoint(&quot;/wamp&quot;).withSockJS();
 * 	}
 *
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DefaultWampConfiguration.class)
public @interface EnableWamp {
	// nothing here
}