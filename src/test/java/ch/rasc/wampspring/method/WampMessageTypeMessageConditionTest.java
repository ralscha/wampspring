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
package ch.rasc.wampspring.method;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.Test;

import ch.rasc.wampspring.message.CallMessage;
import ch.rasc.wampspring.message.PublishMessage;
import ch.rasc.wampspring.message.SubscribeMessage;
import ch.rasc.wampspring.message.UnsubscribeMessage;
import ch.rasc.wampspring.message.WampMessageType;

public class WampMessageTypeMessageConditionTest {

	@Test
	public void testGetMessageType() {
		assertThat(WampMessageTypeMessageCondition.CALL.getMessageType())
				.isEqualTo(WampMessageType.CALL);
		assertThat(WampMessageTypeMessageCondition.PUBLISH.getMessageType())
				.isEqualTo(WampMessageType.PUBLISH);
		assertThat(WampMessageTypeMessageCondition.SUBSCRIBE.getMessageType())
				.isEqualTo(WampMessageType.SUBSCRIBE);
		assertThat(WampMessageTypeMessageCondition.UNSUBSCRIBE.getMessageType())
				.isEqualTo(WampMessageType.UNSUBSCRIBE);

		WampMessageTypeMessageCondition cond = new WampMessageTypeMessageCondition(
				WampMessageType.CALL);
		assertThat(cond.getMessageType()).isEqualTo(WampMessageType.CALL);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetContent() {
		assertThat((Collection<WampMessageType>) WampMessageTypeMessageCondition.CALL
				.getContent()).hasSize(1).containsExactly(WampMessageType.CALL);

		assertThat((Collection<WampMessageType>) WampMessageTypeMessageCondition.PUBLISH
				.getContent()).hasSize(1).containsExactly(WampMessageType.PUBLISH);

		assertThat((Collection<WampMessageType>) WampMessageTypeMessageCondition.SUBSCRIBE
				.getContent()).hasSize(1).containsExactly(WampMessageType.SUBSCRIBE);

		assertThat(
				(Collection<WampMessageType>) WampMessageTypeMessageCondition.UNSUBSCRIBE
						.getContent()).hasSize(1)
								.containsExactly(WampMessageType.UNSUBSCRIBE);

		WampMessageTypeMessageCondition cond = new WampMessageTypeMessageCondition(
				WampMessageType.CALL);
		assertThat((Collection<WampMessageType>) cond.getContent()).hasSize(1)
				.containsExactly(WampMessageType.CALL);

	}

	@Test
	public void testGetMatchingCondition() {
		CallMessage cm = new CallMessage("1", "proc");
		SubscribeMessage sm = new SubscribeMessage("proc");
		UnsubscribeMessage um = new UnsubscribeMessage("proc");
		PublishMessage pm = new PublishMessage("proc", "1");

		assertThat(WampMessageTypeMessageCondition.CALL.getMatchingCondition(cm))
				.isEqualTo(WampMessageTypeMessageCondition.CALL);
		assertThat(WampMessageTypeMessageCondition.SUBSCRIBE.getMatchingCondition(cm))
				.isNull();
		assertThat(WampMessageTypeMessageCondition.UNSUBSCRIBE.getMatchingCondition(cm))
				.isNull();
		assertThat(WampMessageTypeMessageCondition.PUBLISH.getMatchingCondition(cm))
				.isNull();

		assertThat(WampMessageTypeMessageCondition.CALL.getMatchingCondition(sm))
				.isNull();
		assertThat(WampMessageTypeMessageCondition.SUBSCRIBE.getMatchingCondition(sm))
				.isEqualTo(WampMessageTypeMessageCondition.SUBSCRIBE);
		assertThat(WampMessageTypeMessageCondition.UNSUBSCRIBE.getMatchingCondition(sm))
				.isNull();
		assertThat(WampMessageTypeMessageCondition.PUBLISH.getMatchingCondition(sm))
				.isNull();

		assertThat(WampMessageTypeMessageCondition.CALL.getMatchingCondition(um))
				.isNull();
		assertThat(WampMessageTypeMessageCondition.SUBSCRIBE.getMatchingCondition(um))
				.isNull();
		assertThat(WampMessageTypeMessageCondition.UNSUBSCRIBE.getMatchingCondition(um))
				.isEqualTo(WampMessageTypeMessageCondition.UNSUBSCRIBE);
		assertThat(WampMessageTypeMessageCondition.PUBLISH.getMatchingCondition(um))
				.isNull();

		assertThat(WampMessageTypeMessageCondition.CALL.getMatchingCondition(pm))
				.isNull();
		assertThat(WampMessageTypeMessageCondition.SUBSCRIBE.getMatchingCondition(pm))
				.isNull();
		assertThat(WampMessageTypeMessageCondition.UNSUBSCRIBE.getMatchingCondition(pm))
				.isNull();
		assertThat(WampMessageTypeMessageCondition.PUBLISH.getMatchingCondition(pm))
				.isEqualTo(WampMessageTypeMessageCondition.PUBLISH);

		WampMessageTypeMessageCondition cond = new WampMessageTypeMessageCondition(
				WampMessageType.CALL);
		assertThat(cond.getMatchingCondition(cm)).isEqualTo(cond);
		assertThat(cond.getMatchingCondition(sm)).isNull();
		assertThat(cond.getMatchingCondition(um)).isNull();
		assertThat(cond.getMatchingCondition(pm)).isNull();
	}

}
