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
package ch.rasc.wampspring;

import java.util.Collections;
import java.util.Set;

import ch.rasc.wampspring.handler.PubSubHandler;
import ch.rasc.wampspring.message.EventMessage;

/**
 * A messenger that allows the calling code to send {@link EventMessage}s back to the
 * client. This is a spring bean that can be autowired into any other spring bean and
 * allows any part of the application to send messages back to the client
 *
 * e.g.
 *
 * <pre class="code">
 * &#064;Service
 * public class MyService {
 * 	&#064;Autowired
 * 	EventMessenger eventMessenger;
 * 
 * 	public void doSomething() {
 * 		eventMessenger.sendToAll(&quot;aTopic&quot;, &quot;the message&quot;);
 * 	}
 * }
 * </pre>
 * <p>
 * This is very similar to the
 * {@link org.springframework.messaging.simp.SimpMessagingTemplate} class from Spring's
 * STOMP support.
 */
public class EventMessenger {

	private final PubSubHandler pubSubHandler;

	public EventMessenger(PubSubHandler pubSubHandler) {
		this.pubSubHandler = pubSubHandler;
	}

	/**
	 * Send a {@link EventMessage} to every client that is currently subscribed to the
	 * provided topicURI
	 *
	 * @param topicURI the name of the topic
	 * @param event the message
	 */
	public void sendToAll(String topicURI, Object event) {
		pubSubHandler.sendToAll(new EventMessage(topicURI, event));
	}

	/**
	 * Send a {@link EventMessage} to every client that is currently subscribed to the
	 * provided topicURI except the one provided with the excludeSessionId parameter.
	 *
	 * @param topicURI the name of the topic
	 * @param event the message
	 * @param excludeSessionIds a set of session ids that will be excluded
	 */
	public void sendToAllExcept(String topicURI, Object event, String excludeSessionId) {
		pubSubHandler.sendToAllExcept(new EventMessage(topicURI, event),
				Collections.singleton(excludeSessionId));
	}

	/**
	 * Send a {@link EventMessage} to every client that is currently subscribed to the
	 * provided topicURI except the ones listed in the excludeSessionIds set.
	 *
	 * @param topicURI the name of the topic
	 * @param event the message
	 * @param excludeSessionIds a set of session ids that will be excluded
	 */
	public void sendToAllExcept(String topicURI, Object event,
			Set<String> excludeSessionIds) {
		pubSubHandler.sendToAllExcept(new EventMessage(topicURI, event),
				excludeSessionIds);
	}

	/**
	 * Send a {@link EventMessage} to the clients that are subscribed to the provided
	 * topicURI and are listed in the eligibleSessionIds set. If no session of the
	 * provided set is subscribed to the topicURI nothing happens.
	 *
	 * @param topicURI the name of the topic
	 * @param event the message
	 * @param eligibleSessionIds only the session ids listed here will receive the message
	 */
	public void sendTo(String topicURI, Object event, Set<String> eligibleSessionIds) {
		pubSubHandler.sendTo(new EventMessage(topicURI, event), eligibleSessionIds);
	}
}
