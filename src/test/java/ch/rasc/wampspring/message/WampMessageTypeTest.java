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

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;

public class WampMessageTypeTest extends BaseMessageTest {

	@Test
	public void typeIdTest() {
		assertThat(WampMessageType.WELCOME.getTypeId()).isEqualTo(0);
		assertThat(WampMessageType.PREFIX.getTypeId()).isEqualTo(1);
		assertThat(WampMessageType.CALL.getTypeId()).isEqualTo(2);
		assertThat(WampMessageType.CALLRESULT.getTypeId()).isEqualTo(3);
		assertThat(WampMessageType.CALLERROR.getTypeId()).isEqualTo(4);
		assertThat(WampMessageType.SUBSCRIBE.getTypeId()).isEqualTo(5);
		assertThat(WampMessageType.UNSUBSCRIBE.getTypeId()).isEqualTo(6);
		assertThat(WampMessageType.PUBLISH.getTypeId()).isEqualTo(7);
		assertThat(WampMessageType.EVENT.getTypeId()).isEqualTo(8);
	}

	@Test
	public void fromTypeIdTest() {
		assertThat(WampMessageType.fromTypeId(0)).isEqualsToByComparingFields(
				WampMessageType.WELCOME);
		assertThat(WampMessageType.fromTypeId(1)).isEqualsToByComparingFields(
				WampMessageType.PREFIX);
		assertThat(WampMessageType.fromTypeId(2)).isEqualsToByComparingFields(
				WampMessageType.CALL);
		assertThat(WampMessageType.fromTypeId(3)).isEqualsToByComparingFields(
				WampMessageType.CALLRESULT);
		assertThat(WampMessageType.fromTypeId(4)).isEqualsToByComparingFields(
				WampMessageType.CALLERROR);
		assertThat(WampMessageType.fromTypeId(5)).isEqualsToByComparingFields(
				WampMessageType.SUBSCRIBE);
		assertThat(WampMessageType.fromTypeId(6)).isEqualsToByComparingFields(
				WampMessageType.UNSUBSCRIBE);
		assertThat(WampMessageType.fromTypeId(7)).isEqualsToByComparingFields(
				WampMessageType.PUBLISH);
		assertThat(WampMessageType.fromTypeId(8)).isEqualsToByComparingFields(
				WampMessageType.EVENT);
	}
}
