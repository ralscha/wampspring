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
package ch.rasc.wampspring.message;

/**
 * Base class for all publish/subscribe messages. They all share the same property
 * topicURI.
 *
 * @see SubscribeMessage
 * @see UnsubscribeMessage
 * @see PublishMessage
 * @see EventMessage
 */
public abstract class PubSubMessage extends WampMessage {

	private String topicURI;

	protected PubSubMessage(WampMessageType type) {
		super(type);
	}

	public PubSubMessage(WampMessageType type, String topicURI) {
		super(type);
		this.topicURI = topicURI;
	}

	public String getTopicURI() {
		return this.topicURI;
	}

	protected void setTopicURI(String topicURI) {
		this.topicURI = topicURI;
	}

	@Override
	public String getDestination() {
		return this.topicURI;
	}
}
