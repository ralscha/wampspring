/**
 * Copyright 2014-2017 the original author or authors.
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
package ch.rasc.wampspring.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;

import ch.rasc.wampspring.EventMessenger;
import ch.rasc.wampspring.annotation.WampPublishListener;
import ch.rasc.wampspring.annotation.WampSubscribeListener;
import ch.rasc.wampspring.annotation.WampUnsubscribeListener;
import ch.rasc.wampspring.call.TestDto;
import ch.rasc.wampspring.message.PublishMessage;

public class PubSubService {

	@Autowired
	private EventMessenger eventMessenger;

	@WampSubscribeListener("secondTopic")
	public void subscribe() {
		this.eventMessenger.sendToAll("secondTopic", "a simple message");
	}

	@WampPublishListener("sumTopic")
	public void sum(List<Integer> numbers) {
		int total = 0;
		for (Integer number : numbers) {
			total += number;
		}
		this.eventMessenger.sendToAll("resultTopic", total);
	}

	@WampPublishListener
	public void dto(TestDto testDto) {
		assertThat(testDto.getName()).isEqualTo("Hello PubSub");
		this.eventMessenger.sendToAll("pubSubService.dto.result",
				"Server says: " + testDto.getName());
	}

	@WampPublishListener(replyTo = "replyTo1")
	public String incomingPublish1(String incoming) {
		return "return1:" + incoming;
	}

	@WampPublishListener(replyTo = "replyTo2", excludeSender = true)
	public String incomingPublish2(String incoming) {
		return "return2:" + incoming;
	}

	@WampPublishListener(value = "incomingPublish3", replyTo = "replyTo3",
			excludeSender = false)
	public String incomingPublish3(String incoming) {
		return "return3:" + incoming;
	}

	@WampPublishListener(value = "incomingPublish4",
			replyTo = { "replyTo4_1", "replyTo4_2", "replyTo4_3" })
	public String incomingPublish4(String incoming) {
		return "return4:" + incoming;
	}

	@WampPublishListener(value = "incomingPublish5", replyTo = "replyTo5",
			broadcast = false)
	public String incomingPublish5(String incoming) {
		return "return5:" + incoming;
	}

	@WampPublishListener(value = "incomingPublish6", replyTo = "replyTo6",
			broadcast = false, excludeSender = true)
	public String incomingPublish6(String incoming) {
		return "return6:" + incoming;
	}

	@WampSubscribeListener(replyTo = "replyTo1")
	public String incomingSubscribe1() {
		return "returnSub1";
	}

	@WampSubscribeListener(replyTo = "replyTo2", excludeSender = true)
	public String incomingSubscribe2() {
		return "returnSub2";
	}

	@WampSubscribeListener(value = "incomingSub3", replyTo = "replyTo3",
			excludeSender = false)
	public String incomingSubscribe3() {
		return "returnSub3";
	}

	@WampSubscribeListener(value = "incomingSub4",
			replyTo = { "replyTo4_1", "replyTo4_2", "replyTo4_3" })
	public String incomingSubscribe4() {
		return "returnSub4";
	}

	@WampSubscribeListener(value = "incomingSub5", replyTo = "replyTo5",
			broadcast = false)
	public String incomingSubscribe5() {
		return "returnSub5";
	}

	@WampSubscribeListener(value = "incomingSub6", replyTo = "replyTo6",
			broadcast = false, excludeSender = true)
	public String incomingSubscribe6() {
		return "returnSub6";
	}

	@WampUnsubscribeListener(replyTo = "replyTo1")
	public String incomingUnsubscribe1() {
		return "returnUnsub1";
	}

	@WampUnsubscribeListener(replyTo = "replyTo2", excludeSender = true)
	public String incomingUnsubscribe2() {
		return "returnUnsub2";
	}

	@WampUnsubscribeListener(value = "incomingUnsub3", replyTo = "replyTo3",
			excludeSender = false)
	public String incomingUnsubscribe3() {
		return "returnUnsub3";
	}

	@WampUnsubscribeListener(value = "incomingUnsub4",
			replyTo = { "replyTo4_1", "replyTo4_2", "replyTo4_3" })
	public String incomingUnsubscribe4() {
		return "returnUnsub4";
	}

	@WampUnsubscribeListener(value = "incomingUnsub5", replyTo = "replyTo5",
			broadcast = false)
	public String incomingUnsubscribe5() {
		return "returnUnsub5";
	}

	@WampUnsubscribeListener(value = "incomingUnsub6", replyTo = "replyTo6",
			broadcast = false, excludeSender = true)
	public String incomingUnsubscribe6() {
		return "returnUnsub6";
	}

	@WampPublishListener(value = "payloadMethod", replyTo = "payloadMethodResult")
	public String payloadMethod(@Payload String event) {
		return "payloadMethod method called: " + event;
	}

	@WampPublishListener("sendToAllExcept")
	public void testSendToAllExcept(PublishMessage msg) {
		this.eventMessenger.sendToAllExcept("responseSendToAllExcept", 1,
				msg.getWebSocketSessionId());
	}

	@WampPublishListener("sendToAllExceptSet")
	public void testSendToAllExceptSet(PublishMessage msg) {
		Set<String> except = new HashSet<>();
		except.add(msg.getWebSocketSessionId());
		this.eventMessenger.sendToAllExcept("responseSendToAllExceptSet", 1, except);
	}

}
