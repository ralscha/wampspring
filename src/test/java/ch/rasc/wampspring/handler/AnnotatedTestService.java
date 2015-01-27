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
package ch.rasc.wampspring.handler;

import ch.rasc.wampspring.annotation.WampCallListener;
import ch.rasc.wampspring.annotation.WampPublishListener;
import ch.rasc.wampspring.annotation.WampSubscribeListener;
import ch.rasc.wampspring.annotation.WampUnsubscribeListener;

public class AnnotatedTestService {

	@WampCallListener
	public int call(int one, int two) {
		return one + two;
	}

	@WampSubscribeListener
	public int subscribe() {
		return 2;
	}

	@WampSubscribeListener(replyTo = "annotatedTestService.subscribeReplyTo")
	public int subscribeReplyTo() {
		System.out.println("3");
		return 3;
	}

	@WampSubscribeListener(replyTo = "annotatedTestService.subscribeExcludeMe",
			excludeSender = true)
	public int subscribeExcludeMe() {
		return 4;
	}

	@WampUnsubscribeListener
	public int unsubscribe() {
		return 5;
	}

	@WampUnsubscribeListener(replyTo = "annotatedTestService.unsubscribeReplyTo")
	public int unsubscribeReplyTo() {
		return 6;
	}

	@WampUnsubscribeListener(replyTo = "annotatedTestService.unsubscribeExcludeMe",
			excludeSender = true)
	public int unsubscribeExcludeMe() {
		return 7;
	}

	@WampPublishListener
	public int publish() {
		return 8;
	}

	@WampPublishListener(replyTo = "annotatedTestService.publishReplyTo")
	public int publishReplyTo() {
		return 9;
	}

	@WampPublishListener(excludeSender = true,
			replyTo = "annotatedTestService.publishExcludeMe")
	public int publishExcludeMe() {
		return 10;
	}
}
